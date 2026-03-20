package com.hamster.toolbox.screen.ruler

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.utils.compose.EditTextDialog

@Composable
fun RulerScreen() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var zoomFactor by remember { mutableFloatStateOf(prefs.getFloat("ruler_zoom_factor", 1f)) }

    // 获取每毫米对应的物理像素数
    val pxPerMm = rememberPxPerMm() * zoomFactor

    val maxCm = 100
    val totalHeightMm = maxCm * 10f

    val totalHeightPx = totalHeightMm * pxPerMm
    val totalHeightDp = with(LocalDensity.current) { totalHeightPx.toDp() }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E0E0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            RulerContent(
                heightDp = totalHeightDp,
                pxPerMm = pxPerMm,
                maxCm = maxCm
            )
        }
        TextButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showCalibrationDialog = true },) {
            Text(
                text = "校准",
                style = MaterialTheme.typography.displayMedium,
                color = Color.Black.copy(alpha = 0.8f),)
        }
        if (showCalibrationDialog) {
            EditTextDialog(
                title = "校准",
                initialValue = zoomFactor.toString(),
                hint = "输入缩放倍数",
                type = "Float",
                onDismissRequest = { showCalibrationDialog = false },
                onConfirm = { text ->
                    try {
                        zoomFactor = text.toFloat()
                        prefs.edit { putFloat("ruler_zoom_factor", text.toFloat()) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            )
        }
    }
}

@Composable
fun RulerContent(
    heightDp: androidx.compose.ui.unit.Dp,
    pxPerMm: Float,
    maxCm: Int
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        val width = size.width

        for (mm in 0..(maxCm * 10)) {
            val y = mm * pxPerMm

            val isCm = mm % 10 == 0
            val isHalfCm = mm % 5 == 0 && !isCm

            val lineLength = when {
                isCm -> 60f
                isHalfCm -> 40f
                else -> 20f
            }

            val strokeWidth = if (isCm) 4f else 2f

            drawLine(
                color = Color.Black,
                start = Offset(0f, y),
                end = Offset(lineLength, y),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = Color.Black,
                start = Offset(width, y),
                end = Offset(width - lineLength, y),
                strokeWidth = strokeWidth
            )

            if (isCm) {
                val cmValue = mm / 10
                val textLayoutResult = textMeasurer.measure(
                    text = cmValue.toString(),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(lineLength + 10f, y - textLayoutResult.size.height / 2)
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(width - lineLength - 10f - textLayoutResult.size.width, y - textLayoutResult.size.height / 2)
                )
            }
        }
    }
}

// 获取屏幕真实的物理像素密度
@Composable
fun rememberPxPerMm(): Float {
    val context = LocalContext.current
    return remember(context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.ydpi / 25.4f
    }
}