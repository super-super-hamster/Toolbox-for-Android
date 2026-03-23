package com.hamster.toolbox.ai

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.hamster.toolbox.utils.FFT
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.sqrt

class SpeechRecognizerManager(private val context: Context) {
    private var recognizer: OnlineRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow = _isRecordingFlow.asStateFlow()
    val isRecording: Boolean
        get() = _isRecordingFlow.value
    private var streamScope = CoroutineScope(Dispatchers.IO)

    //最终输出结果的数据流
    private val _finalResultFlow = MutableSharedFlow<String>()
    val finalResultFlow = _finalResultFlow.asSharedFlow()

    //缓存的热词
    private var cachedKeywords: Array<String>? = null

    // 回调接口
    var onPartialResult: ((String) -> Unit)? = null

    private var recordingJob: Job? = null

    // 频谱线数据流
    private val _audioSpectrumFlow = MutableSharedFlow<FloatArray>(extraBufferCapacity = 1)
    val audioSpectrumFlow = _audioSpectrumFlow.asSharedFlow()

    private val numLines = 20
    private val fftSize = 1024
    private val fft = FFT(fftSize)
    private val realBuffer = FloatArray(fftSize)
    private val imagBuffer = FloatArray(fftSize)
    private val magnitudes = FloatArray(fftSize / 2)
    private val mappedBars = FloatArray(numLines)

    fun initModel() {
        val modelDir = "sherpa_model"

        val transducerConfig = OnlineTransducerModelConfig(
            encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx",
            decoder = "$modelDir/decoder-epoch-99-avg-1.int8.onnx",
            joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx"
        )

        val modelConfig = OnlineModelConfig(
            transducer = transducerConfig,
            tokens = "$modelDir/tokens.txt",
            numThreads = 1,
            debug = false,
            modelType = "zipformer" ,
            modelingUnit = "cjkchar"
        )

        val rule1 = EndpointRule(false, 2.4f, 0.0f)
        val rule2 = EndpointRule(true, 1.2f, 0.0f)
        val rule3 = EndpointRule(false, 0.0f, 20.0f)

        val endpointConfig = EndpointConfig(
            rule1 = rule1,
            rule2 = rule2,
            rule3 = rule3
        )

        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = modelConfig,
            endpointConfig = endpointConfig,
            enableEndpoint = true,
            decodingMethod = "modified_beam_search",
            maxActivePaths = 4
        )

        try {
            recognizer?.release()

            recognizer = OnlineRecognizer(
                assetManager = context.assets,
                config = config
            )
            Log.d("Sherpa", "Recognizer initialized successfully")
        } catch (e: Exception) {
            Log.e("Sherpa", "Init failed: ${e.message}")
        }

        updateKeywords()

        Log.d("Debug", "模型加载完成")
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (recognizer == null) {
            return
        }
        if (isRecording) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        _isRecordingFlow.value = true

        if (!KeywordManager.isNew) {
            updateKeywords()
            KeywordManager.isNew = true
        }

        val stream = if (cachedKeywords != null && cachedKeywords!!.isNotEmpty()) {
            val keywordsStr = cachedKeywords!!.joinToString(separator = "\n")
            recognizer?.createStream(keywordsStr)
        } else {
            recognizer?.createStream()
        }

        if (stream == null) {
            Log.e("Sherpa", "Failed to create stream")
            return
        }

        recordingJob = streamScope.launch {
            val buffer = ShortArray(bufferSize / 2)

            while (isRecording) {
                val readRet = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readRet > 0) {
                    val actualData = buffer.copyOfRange(0, readRet)
                    val floatData = FloatArray(actualData.size) { actualData[it] / 32768f }

                    stream.acceptWaveform(floatData, sampleRate)

                    while (recognizer?.isReady(stream) == true) {
                        recognizer?.decode(stream)
                    }

                    val result = recognizer?.getResult(stream)
                    if (result != null) {
                        val text = result.text
                        if (text.isNotEmpty()) {
                            if (recognizer?.isEndpoint(stream) == true) {
                                _finalResultFlow.emit(text)
                                recognizer?.reset(stream)
                            } else {
                                onPartialResult?.invoke(text)
                            }
                        }
                    }

                    processAudio(floatData)
                }
            }

            //强行结束时将流中剩余的内容强制发送出去
            val lastResult = recognizer?.getResult(stream)
            val lastText = lastResult?.text
            if (!lastText.isNullOrEmpty()) {
                _finalResultFlow.emit(lastText)
            }

            stream.release()
        }
    }

    private fun processAudio(audioData: FloatArray) {
        val copyLength = minOf(audioData.size, fftSize)
        System.arraycopy(audioData, 0, realBuffer, 0, copyLength)

        for (i in copyLength until fftSize) {
            realBuffer[i] = 0f
        }
        for (i in 0 until fftSize) {
            imagBuffer[i] = 0f
        }

        fft.transform(realBuffer, imagBuffer)

        var maxMag = 0f
        for (i in 0 until fftSize / 2) {
            val re = realBuffer[i]
            val im = imagBuffer[i]
            val mag = sqrt(re * re + im * im)
            magnitudes[i] = mag
            if (mag > maxMag) {
                maxMag = mag
            }
        }

        // 跳过直流偏移，从索引5开始算
        val binSize = (fftSize / 2 - 5) / numLines

        for (i in 0 until numLines) {
            var sum = 0f
            val startIndex = i * binSize
            val endIndex = startIndex + binSize
            for (j in startIndex until endIndex) {
                sum += magnitudes[j]
            }
            // 底噪门槛
            val noiseThreshold = 0.25f

            var barValue = (sum / binSize) * 2.5f
            barValue = max(0f, barValue - noiseThreshold)

            mappedBars[i] = barValue.coerceIn(0f, 1f)
        }

        // 拷贝 防止并发修改
        _audioSpectrumFlow.tryEmit(mappedBars.copyOf())
    }

    fun stopListening() {
        if (!isRecording) {
            return
        }

        _isRecordingFlow.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun updateKeywords() {
        try {
            val dataList: List<KeywordsData> = KeywordManager.loadAllKeywords(context)

            cachedKeywords = dataList.map { item ->
                item.word
            }.toTypedArray()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        _isRecordingFlow.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        runBlocking {
            recordingJob?.join()
        }

        recognizer?.release()
        recognizer = null
    }
}