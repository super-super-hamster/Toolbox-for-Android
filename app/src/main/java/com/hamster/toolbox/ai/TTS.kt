package com.hamster.toolbox.ai

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TTS private constructor(context: Context) {
    private val assetManager = context.applicationContext.assets
    private var tts: OfflineTts? = null
    var isInitialized = false
        private set

    companion object {
        @Volatile
        private var INSTANCE: TTS? = null

        fun getInstance(context: Context): TTS {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTS(context).also { INSTANCE = it }
            }
        }
    }

    // 初始化模型
    suspend fun initModel() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "melo_tts/model.onnx",
                    lexicon = "melo_tts/lexicon.txt",
                    tokens = "melo_tts/tokens.txt",
                    dictDir = "melo_tts/dict"
                ),
                numThreads = 2, // 分配给推理的线程数
                debug = false,
                provider = "cpu"
            ),
            ruleFsts = "melo_tts/date.fst,melo_tts/number.fst,melo_tts/phone.fst",
            maxNumSentences = 1
        )

        tts = OfflineTts(assetManager = assetManager, config = config)
        isInitialized = true
    }

    // 生成音频数据
    suspend fun generateAudio(text: String): Pair<FloatArray, Int>? = withContext(Dispatchers.Default) {
        if (!isInitialized) return@withContext null

        val audio = tts?.generate(text = text, sid = 0, speed = 1.0f)

        if (audio != null) {
            return@withContext Pair(audio.samples, audio.sampleRate)
        }
        return@withContext null
    }

    fun release() {
        tts?.release()
        tts = null
        isInitialized = false
    }
}