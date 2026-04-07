package com.hamster.toolbox.compose

import androidx.annotation.Keep
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId // 必须有这个导入

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

@Composable
fun Heatmap(
    modifier: Modifier = Modifier,
    dataMap: Map<Long, Int>,
    levelColors: List<Color>,
    endMonth: YearMonth = YearMonth.now(),
    verticalGap: Dp = 24.dp,
    horizontalGap: Dp = 24.dp,
    emptyColor: Color = colorResource(R.color.heatmap_empty),
) {
    val normalizedDataMap = remember(dataMap) {
        val zoneId = ZoneId.systemDefault()
        val map = mutableMapOf<LocalDate, Int>()
        dataMap.forEach { (timestamp, level) ->
            val date = Instant.ofEpochMilli(timestamp)
                .atZone(zoneId)
                .toLocalDate()
            map[date] = level
        }
        map
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelMedium.copy(color = Color.Gray)

    // 2. 使用 BoxWithConstraints 获取真实的可用宽度
    BoxWithConstraints(modifier = modifier) {
        val cols = 2
        val rows = 6

        // 定义间距参数 (dp)
        val cellGap = 4.dp
        val titleHeight = 24.dp

        // --- 核心尺寸计算 ---
        // 单个月份模块的最大允许宽度
        val monthBlockW = (maxWidth - horizontalGap * (cols - 1)) / cols
        // 单个小方块的边长：宽度减去6个间隙后，除以7列 (这里强制以宽度为基准计算正方形)
        val cellSize = (monthBlockW - cellGap * 6) / 7

        // 反向推导单个月份模块需要的高度：标题高度 + 6行方块 + 5个行间隙
        val monthBlockH = titleHeight + (cellSize * 6) + (cellGap * 5)

        // 计算整个 Canvas 所需的总高度
        val totalCanvasHeight = (monthBlockH * rows) + (verticalGap * (rows - 1))

        val scrollState = rememberScrollState()

        // 当滚动内容的高度计算出来后（maxValue更新），自动滑动到最底部
        LaunchedEffect(scrollState.maxValue) {
            // 如果你希望瞬间定位到最下面，用 scrollTo：
            scrollState.scrollTo(scrollState.maxValue)

            // 如果你希望刚进页面时有一个平滑滚动到最下面的动画，用 animateScrollTo：
            // scrollState.animateScrollTo(scrollState.maxValue)
        }

        // 3. 构建可滚动的容器
        Column(
            modifier = Modifier
                .fillMaxSize() // 填满外部给予的可见区域
                .verticalScroll(scrollState) // 开启垂直滚动
        ) {
            // 内部的 Canvas 使用计算出的精准高度
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalCanvasHeight)
            ) {
                // 将 dp 转换为 px 以供 Canvas 绘制使用
                val monthBlockWPx = monthBlockW.toPx()
                val monthBlockHPx = monthBlockH.toPx()
                val monthGapXPx = horizontalGap.toPx()
                val monthGapYPx = verticalGap.toPx()
                val cellSizePx = cellSize.toPx()
                val cellGapPx = cellGap.toPx()
                val titleHeightPx = titleHeight.toPx()

                val startMonth = endMonth.minusMonths(11)

                // 4. 遍历绘制
                for (i in 0 until 12) {
                    val currentMonth = startMonth.plusMonths(i.toLong())
                    val r = i / cols
                    val c = i % cols

                    val offsetX = c * (monthBlockWPx + monthGapXPx)
                    val offsetY = r * (monthBlockHPx + monthGapYPx)

                    translate(left = offsetX, top = offsetY) {
                        // 绘制月份标题
                        val monthText = "${currentMonth.monthValue}月"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = monthText,
                            style = textStyle,
                            topLeft = Offset(0f, 0f)
                        )

                        // 绘制日历网格
                        val daysInMonth = currentMonth.lengthOfMonth()
                        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
                        val startColIndex = firstDayOfWeek - 1

                        for (day in 1..daysInMonth) {
                            val date = currentMonth.atDay(day)
                            val level = normalizedDataMap[date] ?: -1

                            val color = if (level in levelColors.indices) {
                                levelColors[level]
                            } else {
                                emptyColor
                            }

                            val dayGridIndex = startColIndex + (day - 1)
                            val dayRow = dayGridIndex / 7
                            val dayCol = dayGridIndex % 7

                            val cellX = dayCol * (cellSizePx + cellGapPx)
                            val cellY = titleHeightPx + dayRow * (cellSizePx + cellGapPx)

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(cellX, cellY),
                                size = Size(cellSizePx, cellSizePx),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Keep
data class HorizontalLine(
    @SerializedName("value") val value: Float,
    @SerializedName("label") val label: String,
    @SerializedName("color") val color: Color? = Color.Gray
)