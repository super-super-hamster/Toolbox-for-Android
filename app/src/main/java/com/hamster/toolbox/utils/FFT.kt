package com.hamster.toolbox.utils

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.log2

// size 必须为2的整数次幂
class FFT(val size: Int) {
    // 预计算三角函数表
    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)
    // 预计算位反转索引
    private val bitReverse = IntArray(size)

    init {
        for (i in 0 until size / 2) {
            val angle = -2.0 * PI * i / size
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }

        val shift = log2(size.toFloat()).toInt()
        for (i in 0 until size) {
            var j = i
            var rev = 0
            for (k in 0 until shift) {
                rev = (rev shl 1) or (j and 1)
                j = j shr 1
            }
            bitReverse[i] = rev
        }
    }

    fun transform(real: FloatArray, imag: FloatArray) {
        // 位反转置换
        for (i in 0 until size) {
            val j = bitReverse[i]
            if (i < j) {
                val tempRe = real[i]
                val tempIm = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tempRe
                imag[j] = tempIm
            }
        }

        // 蝶形运算
        var step = 1
        while (step < size) {
            val jump = step shl 1
            val tableStep = size / jump
            for (group in 0 until step) {
                val cosW = cosTable[group * tableStep]
                val sinW = sinTable[group * tableStep]
                for (pair in group until size step jump) {
                    val match = pair + step
                    val reB = real[match]
                    val imB = imag[match]

                    val tr = cosW * reB - sinW * imB
                    val ti = sinW * reB + cosW * imB

                    real[match] = real[pair] - tr
                    imag[match] = imag[pair] - ti
                    real[pair] += tr
                    imag[pair] += ti
                }
            }
            step = jump
        }
    }
}