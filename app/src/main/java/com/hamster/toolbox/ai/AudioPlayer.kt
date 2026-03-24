package com.hamster.toolbox.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioPlayer {

    suspend fun play(samples: FloatArray, sampleRate: Int = 44100) = withContext(Dispatchers.IO) {
        if (samples.isEmpty()) return@withContext

        var maxAmplitude = 0.0f
        for (sample in samples) {
            val absSample = kotlin.math.abs(sample)
            if (absSample > maxAmplitude) {
                maxAmplitude = absSample
            }
        }

        val targetPeak = 0.95f
        val volumeScale = if (maxAmplitude > 0f) targetPeak / maxAmplitude else 1.0f

        val shortSamples = ShortArray(samples.size)
        for (i in samples.indices) {
            var pcm = samples[i] * 32767.0f * volumeScale
            pcm = pcm.coerceIn(-32768f, 32767f)
            shortSamples[i] = pcm.toInt().toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(shortSamples.size * 2) // Short 占 2 个字节
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(shortSamples, 0, shortSamples.size)
            audioTrack.play()

            val durationMs = (samples.size.toLong() * 1000) / sampleRate
            kotlinx.coroutines.delay(durationMs + 100) // 多等 100ms 缓冲

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
}