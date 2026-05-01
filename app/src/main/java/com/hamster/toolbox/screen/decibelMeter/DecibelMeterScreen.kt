package com.hamster.toolbox.screen.decibelMeter

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.RingProgress
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.squircleShape
import com.hamster.toolbox.R
import com.hamster.toolbox.compose.SliderDialog
import com.hamster.toolbox.compose.rememberFloatPreference
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.utils.color.ColorLine
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DecibelMeterScreen(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val sharedTiltState = rememberSharedTiltState()

    val viewModel: DecibelMeterViewModel = viewModel()

    var offset by rememberFloatPreference("decibel_meter_offset", 0f)

    var isBegin by remember { mutableStateOf(false) }

    val colorLine = ColorLine(listOf(
        Color(0xFF22C55E),
        Color(0xFFEAB308),
        Color(0xFFFF8904),
        Color(0xFFEF4444),
        Color(0xFFB91C1C)
    ))

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecording()
        }
    }

    PageColumn(sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState, modifier = Modifier.weight(1f)) {
            // 主体内容布局
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                StatCard(
                    avg = if (isBegin) (viewModel.stats10s.avg + offset).roundToInt() else 0,
                    max = if (isBegin) (viewModel.stats10s.max + offset).roundToInt() else 0,
                    min = if (isBegin) (viewModel.stats10s.min + offset).roundToInt() else 0,
                    title = "近10秒",
                    avgColor = Color(colorLine.getColor((if (isBegin) viewModel.stats10s.avg + offset else 0f) / 120f)),
                    maxColor = Color(colorLine.getColor((if (isBegin) viewModel.stats10s.max + offset else 0f) / 120f)),
                    minColor = Color(colorLine.getColor((if (isBegin) viewModel.stats10s.min + offset else 0f) / 120f))
                )

                Spacer(modifier = Modifier.height(32.dp))

                StatCard(
                    avg = if (isBegin) (viewModel.statsTotal.avg + offset).roundToInt() else 0,
                    max = if (isBegin) (viewModel.statsTotal.max + offset).roundToInt() else 0,
                    min = if (isBegin) (viewModel.statsTotal.min + offset).roundToInt() else 0,
                    title = "总计",
                    avgColor = Color(colorLine.getColor((if (isBegin) viewModel.statsTotal.avg + offset else 0f) / 120f)),
                    maxColor = Color(colorLine.getColor((if (isBegin) viewModel.statsTotal.max + offset else 0f) / 120f)),
                    minColor = Color(colorLine.getColor((if (isBegin) viewModel.statsTotal.min + offset else 0f) / 120f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    RingProgress(
                        ringColor = Color(colorLine.getColor((if (isBegin) viewModel.currentDb + offset else 0f) / 120f)),
                        progress = ((if (isBegin) viewModel.currentDb + offset else 0f) / 120f).coerceIn(0f, 1f),
                        text = if (viewModel.isRecording) String.format(Locale.getDefault(), "%.1f dB", viewModel.currentDb + offset) else "开始",
                    ) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.toggleRecording()
                            isBegin = !isBegin
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            }
        }

        if (mainViewModel.showDecibelMeterOffsetDialog) {
            SliderDialog(
                title = "校准",
                content = String.format(Locale.getDefault(), "%+.1f dB", offset),
                value = offset,
                valueRange = -30f..30f,
                onValueChange = { newValue ->
                    offset = (newValue * 10f).roundToInt() / 10f
                },
                onDismissRequest = { mainViewModel.showDecibelMeterOffsetDialog = false },
                onCancel = {},
                onConfirm = { true },
                setValue = { offset = it }
            )
        }
    }
}

@Composable
fun StatCard(
    avg: Int,
    max: Int,
    min: Int,
    title: String,
    avgColor: Color,
    maxColor: Color,
    minColor: Color
) {
    Surface(
        color = colorResource(R.color.light_gray),
        shape = squircleShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                IndicatorItem(
                    text = avg.toString(),
                    label = "平均值",
                    color = avgColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndicatorItem(
                    text = max.toString(),
                    label = "最大值",
                    color = maxColor,
                    modifier = Modifier.weight(1f)
                )

                IndicatorItem(
                    text = min.toString(),
                    label = "最小值",
                    color = minColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun IndicatorItem(
    text: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row {
            Text(text = label, fontSize = 14.sp, color = Color.Black)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 16.sp)
        }
    }
}