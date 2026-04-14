package com.hamster.toolbox.utils.color

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

suspend fun getColorGroups(
    bitmap: Bitmap,
    radius: Float = 20f,
): List<GroupNode> = withContext(Dispatchers.Default) {
    val width = bitmap.width
    val height = bitmap.height
    val size = width * height
    val pixels = IntArray(size)
    val radiusSq = radius * radius

    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val lArray = FloatArray(size)
    val aArray = FloatArray(size)
    val bArray = FloatArray(size)

    val groupPixels = IntArray(size) { -1 }
    val group = mutableListOf<GroupNode>()

    for (i in pixels.indices) {
        val rgb = RGB(Color.red(pixels[i]), Color.green(pixels[i]), Color.blue(pixels[i]))
        val lab = rgb.toLAB()
        lArray[i] = lab.l
        aArray[i] = lab.a
        bArray[i] = lab.b
    }

    val q = IntArray(size)
    var head = 0
    var tail = 0

    val mx = arrayOf(0, 0, 1, -1)
    val my = arrayOf(1, -1, 0, 0)

    for (i in groupPixels.indices) {
        if (groupPixels[i] != -1) {
            continue
        }

        group.add(GroupNode())
        val groupId = group.size - 1
        group.last().cover(lArray[i], aArray[i], bArray[i])
        groupPixels[i] = groupId

        q[tail++] = i
        val curGroup = group[groupPixels[i]]

        val curX = i % width
        val curY = i / width
        if (curX == 0 || curX == width - 1 || curY == 0 || curY == height - 1) {
            curGroup.isBackground = true
        }

        while (head < tail) {
            val cur = q[head++]

            val x = cur % width
            val y = cur / width

            for (j in 0..3) {
                val nx = x + mx[j]
                val ny = y + my[j]
                val nextPos = ny * width + nx

                if (nx !in 0 until width || ny !in 0 until height || groupPixels[nextPos] != -1) {
                    continue
                }

                if (mergeCheck(curGroup, radius, radiusSq, lArray[nextPos], aArray[nextPos], bArray[nextPos])) {
                    curGroup.merge(lArray[nextPos], aArray[nextPos], bArray[nextPos])

                    if (nx == 0 || nx == width - 1 || ny == 0 || ny == height - 1) {
                        curGroup.isBackground = true
                    }

                    groupPixels[nextPos] = groupId
                    q[tail++] = nextPos
                }
            }
        }
    }

    group.sortByDescending { it.count }

    group
}

suspend fun getMidtoneColors(
    bitmap: Bitmap,
    radius: Float = 20f,
    mergeRadius: Float = 10f,
    count: Int,
    mergeSimilarColor: Boolean = true,
    ignoreBackground: Boolean = false,
    minBrightness: Float = 15f,
    maxBrightness: Float = 85f
): List<Int> = withContext(Dispatchers.Default) {
    val group = getColorGroups(bitmap, radius)

    val result = mutableListOf<Int>()
    val selectedColor = mutableListOf<LAB>()
    for (g in group) {
        if (!(ignoreBackground && g.isBackground)) {
            if (g.l !in minBrightness..maxBrightness) {
                continue
            }

            if (mergeSimilarColor && selectedColor.isNotEmpty()) {
                var minDis = 1e9f
                selectedColor.forEach {
                    minDis = min(minDis, dis(g, it.l, it.a, it.b))
                }
                if (minDis <= mergeRadius) {
                    continue
                }
            }

            val lab = LAB(g.l, g.a, g.b)
            val rgb = lab.toRGB()
            result.add(rgb.toInt())
            selectedColor.add(lab)
        }
        if (result.size == count) {
            break
        }
    }

    result
}

suspend fun getMidtoneColor(bitmap: Bitmap,
                     radius: Float = 20f,
                     mergeRadius: Float = 10f,
                     mergeSimilarColor: Boolean = true,
                     ignoreBackground: Boolean = false
):Int {
    val color = getMidtoneColors(bitmap, radius, mergeRadius, 1, mergeSimilarColor, ignoreBackground)
    return if (color.isEmpty()) 0 else color.first()
}

fun mergeCheck(groupNode: GroupNode, radius: Float, radiusSq: Float, l: Float, a: Float, b: Float): Boolean {
    return dis(groupNode, l, a, b) <= radiusSq &&
            max(abs(l - groupNode.maxL), abs(l - groupNode.minL)) <= radius &&
            max(abs(a - groupNode.maxA), abs(a - groupNode.minA)) <= radius &&
            max(abs(b - groupNode.maxB), abs(b - groupNode.minB)) <= radius
}

fun dis(groupNode: GroupNode, l: Float, a: Float, b: Float): Float {
    return  sqrt((groupNode.l - l) * (groupNode.l - l) +
            (groupNode.a - a) * (groupNode.a - a) +
            (groupNode.b - b) * (groupNode.b - b))
}

data class GroupNode(
    var maxL: Float = 0.toFloat(),
    var minL: Float = 0.toFloat(),
    var maxA: Float = 0.toFloat(),
    var minA: Float = 0.toFloat(),
    var maxB: Float = 0.toFloat(),
    var minB: Float = 0.toFloat(),

    var sumL: Float = 0.toFloat(),
    var sumA: Float = 0.toFloat(),
    var sumB: Float = 0.toFloat(),

    var count: Int = 1,

    var isBackground: Boolean = false
) {
    val l: Float get() = sumL / count
    val a: Float get() = sumA / count
    val b: Float get() = sumB / count
}

fun GroupNode.merge(l: Float, a: Float, b: Float) {
    this.maxL = max(this.maxL, l)
    this.minL = min(this.minL, l)
    this.maxA = max(this.maxA, a)
    this.minA = min(this.minA, a)
    this.maxB = max(this.maxB, b)
    this.minB = min(this.minB, b)

    this.sumL += l
    this.sumA += a
    this.sumB += b

    ++this.count
}

fun GroupNode.cover(l: Float, a: Float, b: Float) {
    this.maxL = l
    this.minL = l
    this.maxA = a
    this.minA = a
    this.maxB = b
    this.minB = b
    this.sumL = l
    this.sumA = a
    this.sumB = b
    this.count = 1
}