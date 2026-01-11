package com.hamster.toolbox.screen.random

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigestudio.wheelpicker.compose.WheelPickerComposable
import com.aigestudio.wheelpicker.WheelPicker
import com.hamster.toolbox.R
import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt
import com.hamster.toolbox.utils.squircleShape
import com.hamster.toolbox.utils.tiltGestureContainer
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun RandomNumberScreen() {
    val data = (0..100).toList()
    var selectedMin by remember { mutableIntStateOf(0) }
    var selectedMax by remember { mutableIntStateOf(9) }

    var randomNumber by remember { mutableIntStateOf(0) }
    var isRolling by remember { mutableStateOf(false) }

    var canGenerate = true

    LaunchedEffect(isRolling) {
        if (isRolling) {
            val range = (selectedMin..selectedMax)
            var currentDelay = 10L
            val maxDelay = 400L

            while (currentDelay < maxDelay) {
                randomNumber = range.random()
                delay(currentDelay)
                currentDelay = (currentDelay * 1.1f).toLong()
            }

            isRolling = false
            canGenerate = true
        }
    }

    MaterialTheme {
        CompositionLocalProvider( // 禁用边缘回弹和光晕效果
            LocalOverscrollFactory provides null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.background))
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(96.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WheelPickerComposable(
                        data = data,
                        selectedItemPosition = selectedMin,
                        onItemSelected = { picker, data, position ->
                            selectedMin = position
                        },
                        modifier = Modifier.fillMaxHeight(),
                        factory = {
                            isCurved = true // 3D效果
                            isCyclic = true // 循环滚动
                            visibleItemCount = 7
                            selectedItemTextColor = AndroidColor.BLACK
                            isAtmospheric = true // 边缘透明

                            setOnWheelChangeListener(object : WheelPicker.OnWheelChangeListener {
                                // 记录上一次的索引
                                var lastIndex = 0

                                override fun onWheelScrolled(offset: Int) {
                                    val itemHeight = if (visibleItemCount > 0) height / visibleItemCount else 0

                                    if (itemHeight > 0) {
                                        val currentIndex = -offset / itemHeight
                                        if (currentIndex != lastIndex) {
                                            lastIndex = currentIndex
                                            // 触发震动
                                            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }

                                override fun onWheelSelected(position: Int) {
                                    selectedMax = max(selectedMax, selectedMin)
                                }
                                override fun onWheelScrollStateChanged(state: Int) {
                                    canGenerate = state != WheelPicker.SCROLL_STATE_IDLE
                                }
                            })
                        }
                    )

                    Text("≤", fontSize = 24.sp)
                    Text(randomNumber.toString(), fontSize = 36.sp)
                    Text("≤", fontSize = 24.sp)

                    WheelPickerComposable(
                        data = data,
                        selectedItemPosition = selectedMax,
                        onItemSelected = { picker, data, position ->
                            selectedMax = position
                        },
                        modifier = Modifier.fillMaxHeight(),
                        factory = {
                            isCurved = true
                            isCyclic = true
                            visibleItemCount = 7
                            selectedItemTextColor = AndroidColor.BLACK
                            isAtmospheric = true

                            setOnWheelChangeListener(object : WheelPicker.OnWheelChangeListener {
                                var lastIndex = 0

                                override fun onWheelScrolled(offset: Int) {
                                    val itemHeight = if (visibleItemCount > 0) height / visibleItemCount else 0

                                    if (itemHeight > 0) {
                                        val currentIndex = -offset / itemHeight
                                        if (currentIndex != lastIndex) {
                                            lastIndex = currentIndex
                                            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }

                                override fun onWheelSelected(position: Int) {
                                    selectedMin = min(selectedMin, selectedMax)
                                }
                                override fun onWheelScrollStateChanged(state: Int) {}
                            })
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        modifier = Modifier
                            .height(96.dp)
                            .width(128.dp),
                        colors = ButtonDefaults.buttonColors(colorResource(R.color.mikuGreen)),
                        shape = squircleShape,
                        onClick = {
                            if (canGenerate) {
                                isRolling = true
                            }
                        }
                    ) {
                        Text("生成", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}