package com.hamster.toolbox.screen.random

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aigestudio.wheelpicker.WheelPicker
import com.aigestudio.wheelpicker.compose.WheelPickerComposable
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.ItemCard
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.applySharedTilt
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.compose.squircleShape
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RandomNumberScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val data = (0..100).toList()
    var selectedMin by remember { mutableIntStateOf(prefs.getInt("random_number_min", 0)) }
    var selectedMax by remember { mutableIntStateOf(prefs.getInt("random_number_max", 9)) }

    // 按钮
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

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

    val sharedTiltState = rememberSharedTiltState()

    PageColumn(sharedTiltState = sharedTiltState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            ItemCard(titleState = sharedTiltState) {
                Column(verticalArrangement = Arrangement.Center) {
                    WheelPickerComposable(
                        data = data,
                        selectedItemPosition = selectedMin,
                        onItemSelected = { _, _, position ->
                            selectedMin = position },
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
                                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                }
            }

            ItemCard(titleState = sharedTiltState, modifier = Modifier.weight(1f), horizontalPadding = 10.dp) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("≤", fontSize = 36.sp, modifier = Modifier.padding(horizontal = 16.dp))
                        Text(
                            text = randomNumber.toString(),
                            fontSize = 54.sp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .defaultMinSize(minWidth = 80.dp),
                            style = TextStyle(fontFeatureSettings = "tnum"), // 等宽字体
                            textAlign = TextAlign.Center
                        )
                        Text("≤", fontSize = 36.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .height(96.dp)
                                .width(128.dp)
                                .padding(bottom = 20.dp)
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                }
                                .shadow(
                                    elevation = 3.dp,
                                    shape = squircleShape,
                                    clip = false,
                                    spotColor = colorResource(id = R.color.item_group_card_shadow),
                                    ambientColor = colorResource(id = R.color.item_group_card_shadow)
                                ),
                            colors = ButtonDefaults.buttonColors(colorResource(R.color.mikuGreen)),
                            shape = squircleShape,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 2.dp
                            ),
                            onClick = {
                                if (canGenerate) {
                                    prefs.edit { putInt("random_number_min", selectedMin) }
                                    prefs.edit { putInt("random_number_max", selectedMax) }
                                    isRolling = true
                                }
                            },
                        ) {
                            Text("生成", fontSize = 18.sp)
                        }
                    }
                }
            }

            ItemCard(titleState = sharedTiltState) {
                Column(verticalArrangement = Arrangement.Center) {
                    WheelPickerComposable(
                        data = data,
                        selectedItemPosition = selectedMax,
                        onItemSelected = { _, _, position ->
                            selectedMax = position },
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
                                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
            }
        }



    }
}