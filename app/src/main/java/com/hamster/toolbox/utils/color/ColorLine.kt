package com.hamster.toolbox.utils.color

import androidx.compose.ui.graphics.Color
import kotlin.math.min

class ColorLine(rgb: List<Color>) {
    private val lab: MutableList<LAB> = mutableListOf()
    private var segmentCount: Int = 0

    init {
        require(rgb.isNotEmpty())

        lab.add(rgb.first().toRGB().toLAB())
        lab.add(lab.first())

        rgb.forEach {
            lab.add(it.toRGB().toLAB())
        }

        lab.add(rgb.last().toRGB().toLAB())
        lab.add(lab.last())

        segmentCount = lab.size - 3
    }

    fun getColor(t: Float): Int {
        val ct = t.coerceIn(0f, 1f)
        val mappedT = ct * segmentCount
        val index = min(mappedT.toInt(), segmentCount - 1)

        val u = if (index == segmentCount - 1 && ct == 1f) 1f else mappedT - index

        val c0 = lab[index]
        val c1 = lab[index + 1]
        val c2 = lab[index + 2]
        val c3 = lab[index + 3]

        val l = calculateBSplineValue(c0.l, c1.l, c2.l, c3.l, u)
        val a = calculateBSplineValue(c0.a, c1.a, c2.a, c3.a, u)
        val b = calculateBSplineValue(c0.b, c1.b, c2.b, c3.b, u)

        val result = LAB(l, a, b).toInt()
        return result
    }

    // 计算单段局部进度
    fun calculateBSplineValue(p0: Float, p1: Float, p2: Float, p3: Float, u: Float): Float {
        val u2 = u * u
        val u3 = u2 * u

        val w0 = (1.0f - 3.0f * u + 3.0f * u2 - u3) / 6.0f
        val w1 = (4.0f - 6.0f * u2 + 3.0f * u3) / 6.0f
        val w2 = (1.0f + 3.0f * u + 3.0f * u2 - 3.0f * u3) / 6.0f
        val w3 = u3 / 6.0f

        return w0 * p0 + w1 * p1 + w2 * p2 + w3 * p3
    }
}