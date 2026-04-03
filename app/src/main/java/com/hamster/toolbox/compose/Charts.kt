package com.hamster.toolbox.compose

import androidx.annotation.Keep
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import com.hamster.toolbox.R

@Composable
fun <T> LineChart(
    data: List<T>,
    yValueMapper: (T) -> Float,
    xLabelMapper: (Int, T) -> String?,
    modifier: Modifier = Modifier,
    lineColor: Color = colorResource(R.color.mikuGreen),
    dotColor: Color = colorResource(R.color.mikuGreen),
    textColor: Color = colorResource(R.color.text),
    maxYValue: Float? = null,
    isScrollable: Boolean = false,
    pointSpacing: Dp = 64.dp, // 开启左右滑动后的点水平间距
    horizontalLines: List<HorizontalLine> = emptyList(),
    onPointClick: ((index: Int, item: T) -> Unit)? = null
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = 10.sp)
    val labelStyle = TextStyle(color = textColor.copy(alpha = 0.6f), fontSize = 9.sp)

    // 动态计算 Y 轴最大值
    val computedMaxY = remember(data, maxYValue, horizontalLines) {
        val dataMax = data.maxOfOrNull { yValueMapper(it) } ?: 0f
        val linesMax = horizontalLines.maxOfOrNull { it.value } ?: 0f
        val max = maxYValue ?: maxOf(dataMax, linesMax)
        if (max <= 0f) 1f else max
    }

    val scrollState = rememberScrollState()

    // 🌟 新增：用于在绘制时记录每个数据点在画布上的精确坐标
    // 注意：这里用普通的 Map 即可，不需要 mutableStateOf，避免在 onDraw 中更新引发无限重组
    val pointCoordinates = remember { mutableMapOf<Int, Offset>() }

    val canvasModifier = if (isScrollable) {
        val extraPadding = 40.dp
        val calculatedWidth = if (data.size > 1) {
            (pointSpacing * (data.size - 1)) + extraPadding
        } else {
            pointSpacing + extraPadding
        }
        modifier.horizontalScroll(scrollState).width(calculatedWidth)
    } else {
        modifier.fillMaxWidth()
    }

    // 🌟 新增：给 Canvas 添加手势监听并拼接高度属性
    val finalModifier = canvasModifier
        .height(220.dp)
        .pointerInput(data, computedMaxY) { // 将依赖项传入，数据变化时重新绑定
            detectTapGestures { tapOffset ->
                // 定义点击的有效半径，24.dp 扩大了点击热区，提升手指触控体验
                val clickThreshold = 24.dp.toPx()
                var clickedIndex = -1
                var minDistance = Float.MAX_VALUE

                // 遍历保存的点坐标，寻找距离手指最近的一个点
                for ((index, pointOffset) in pointCoordinates) {
                    val distance = (pointOffset - tapOffset).getDistance()
                    // 如果该点在点击半径内，且是所有点中最近的
                    if (distance < clickThreshold && distance < minDistance) {
                        minDistance = distance
                        clickedIndex = index
                    }
                }

                // 如果找到了被点击的点，触发回调
                if (clickedIndex != -1 && onPointClick != null) {
                    onPointClick(clickedIndex, data[clickedIndex])
                }
            }
        }

    Canvas(modifier = finalModifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val paddingBottom = 40.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingX = if (isScrollable) 20.dp.toPx() else 0f
        val graphHeight = canvasHeight - paddingBottom - paddingTop
        val graphWidth = canvasWidth - (paddingX * 2)

        // --- 0. 绘制水平参考线 ---
        horizontalLines.forEach { line ->
            val ratio = line.value / computedMaxY
            val y = paddingTop + graphHeight - (ratio * graphHeight)

            drawLine(
                color = line.color ?: textColor.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1.dp.toPx(),
            )

            val textLayoutResult = textMeasurer.measure(line.label, labelStyle)
            val labelX = if (isScrollable) scrollState.value.toFloat() + 4.dp.toPx() else 4.dp.toPx()

            drawText(
                textMeasurer = textMeasurer,
                text = line.label,
                style = labelStyle,
                topLeft = Offset(labelX, y - textLayoutResult.size.height)
            )
        }

        // --- 1. 计算折线坐标点 ---
        val stepX = if (data.size > 1) graphWidth / (data.size - 1) else graphWidth / 2f
        val path = Path()
        val points = mutableListOf<Offset>()

        pointCoordinates.clear() // 🌟 新增：每次重新绘制时先清空历史坐标

        data.forEachIndexed { index, item ->
            val x = paddingX + if (data.size > 1) index * stepX else stepX
            val yVal = yValueMapper(item)
            val ratio = yVal / computedMaxY
            val y = paddingTop + graphHeight - (ratio * graphHeight)

            val offset = Offset(x, y)
            points.add(offset)

            pointCoordinates[index] = offset // 🌟 新增：记录当前点的绝对坐标，供手势检测使用

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // --- 2. 绘制折线 ---
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // --- 3. 绘制圆点与 X 轴文本 ---
        points.forEachIndexed { index, point ->
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = point)
            val label = xLabelMapper(index, data[index])
            if (label != null) {
                val textLayoutResult = textMeasurer.measure(label, textStyle)

                // 添加了防文字越界的逻辑（来自之前的建议）
                var textStartX = point.x - textLayoutResult.size.width / 2f
                if (textStartX < 0f) textStartX = 0f
                if (textStartX + textLayoutResult.size.width > canvasWidth) {
                    textStartX = canvasWidth - textLayoutResult.size.width.toFloat()
                }

                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    style = textStyle,
                    topLeft = Offset(
                        x = textStartX,
                        y = canvasHeight - paddingBottom + 10.dp.toPx()
                    )
                )
            }
        }

        // --- 4. 绘制底线 ---
        drawLine(
            color = textColor.copy(alpha = 0.3f),
            start = Offset(0f, canvasHeight - paddingBottom),
            end = Offset(canvasWidth, canvasHeight - paddingBottom),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Keep
data class HorizontalLine(
    @SerializedName("value") val value: Float,
    @SerializedName("label") val label: String,
    @SerializedName("color") val color: Color? = Color.Gray
)