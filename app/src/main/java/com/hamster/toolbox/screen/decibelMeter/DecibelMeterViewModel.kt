package com.hamster.toolbox.screen.decibelMeter

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

data class DecibelStats(
    val avg: Float = 0f,
    val max: Float = 0f,
    val min: Float = 0f
)

class DecibelMeterViewModel() : ViewModel() {

    var isRecording by mutableStateOf(false)
        private set
    var currentDb by mutableFloatStateOf(0f)
        private set
    var stats10s by mutableStateOf(DecibelStats())
        private set
    var statsTotal by mutableStateOf(DecibelStats())
        private set

    private var totalDb: Float = 0f
    private var totalCount: Int = 0

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val history10s = ArrayDeque<Pair<Long, Float>>()

    private val smoothingFactor = 0.2f
    private var lastSmoothedDb = 0f

    @SuppressLint("MissingPermission")
    fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            reset()
            startRecording()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (isActive && isRecording) {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    var maxAmplitude = 0.0
                    for (i in 0 until readSize) {
                        val amplitude = abs(buffer[i].toInt()).toDouble()
                        if (amplitude > maxAmplitude) {
                            maxAmplitude = amplitude
                        }
                    }

                    val db = if (maxAmplitude > 1) (20 * log10(maxAmplitude)).toFloat() else 0f

                    val now = System.currentTimeMillis()

                    withContext(Dispatchers.Main) {
                        updateStats(now, db)
                    }
                }
            }
        }
    }

    private fun updateStats(now: Long, newDb: Float) {
        if (newDb <= 0) return

        // 平滑数据
        lastSmoothedDb = if (lastSmoothedDb == 0f) {
            newDb
        } else {
            (smoothingFactor * newDb) + ((1 - smoothingFactor) * lastSmoothedDb)
        }

        currentDb = lastSmoothedDb

        // 更新总计
        ++totalCount
        totalDb += newDb
        statsTotal = DecibelStats(
            avg = totalDb / totalCount,
            max = max(statsTotal.max, newDb),
            min = if (statsTotal.min == 0f) newDb else min(statsTotal.min, newDb)
        )

        // Float的有效精度大约1.677e6
        if (totalDb > 1e6) {
            totalDb /= 2
            totalCount /= 2
        }

        // 更新近10秒
        history10s.addLast(Pair(now, newDb))
        while (history10s.isNotEmpty() && (now - history10s.first().first > 10000)) {
            history10s.removeFirst()
        }
        val values10s = history10s.map { it.second }
        if (values10s.isNotEmpty()) {
            stats10s = DecibelStats(
                avg = values10s.average().toFloat(),
                max = values10s.maxOrNull() ?: 0f,
                min = values10s.minOrNull() ?: 0f
            )
        }
    }

    fun reset() {
        stats10s = DecibelStats()
        statsTotal = DecibelStats()

        totalDb = 0f
        totalCount = 0
        currentDb = 0f
        lastSmoothedDb = 0f
        history10s.clear()
    }

    private fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}