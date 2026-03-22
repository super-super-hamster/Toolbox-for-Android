package com.hamster.toolbox.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun AudioSpectrumVisualizer(
    spectrumFlow: Flow<FloatArray>,
    modifier: Modifier = Modifier,
    numLines: Int = 10,
    lineColor: Color
) {
    val spectrumData by spectrumFlow.collectAsState(initial = FloatArray(numLines))

    val animatedHeights = remember { List(numLines) { Animatable(0f) } }

    LaunchedEffect(spectrumData) {
        for (i in 0 until numLines) {
            launch {
                val target = spectrumData.getOrElse(i) { 0f }

                animatedHeights[i].animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 80, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val totalSpacing = canvasWidth * 0.2f
        val spacing = totalSpacing / (numLines - 1)
        val barWidth = (canvasWidth - totalSpacing) / numLines

        val minHeight = 4.dp.toPx()

        for (i in 0 until numLines) {
            val currentValue = animatedHeights[i].value

            val barHeight = maxOf(canvasHeight * currentValue, minHeight)

            val xOffset = i * (barWidth + spacing)
            val yOffset = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                color = lineColor,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}