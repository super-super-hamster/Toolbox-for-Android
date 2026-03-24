package com.hamster.toolbox.utils.compose

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.hamster.toolbox.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sv.lib.squircleshape.CornerSmoothing
import sv.lib.squircleshape.SquircleShape

val squircleShape = SquircleShape(
    radius = 16.dp,
    smoothing = CornerSmoothing.Medium
)

val userBubbleShape = SquircleShape(
    topStart = 16.dp,
    topEnd = 3.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
    smoothing = CornerSmoothing.Medium
)

val assistantBubbleShape = SquircleShape(
    topStart = 3.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
    smoothing = CornerSmoothing.Medium
)

@Composable
fun ItemGroup(
    titleState: SharedTiltState,
    color: Color = colorResource(id = R.color.item_group_card),
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 1.dp, bottom = 1.dp)
                .applySharedTilt(titleState)
                .shadow(
                    elevation = 2.dp,
                    shape = squircleShape,
                    clip = false, // 防止内容被裁掉
                    spotColor = colorResource(id = R.color.item_group_card_shadow),  // 主阴影
                    ambientColor = colorResource(id = R.color.item_group_card_shadow)// 柔和阴影
                ),
            colors = CardDefaults.cardColors(containerColor = color),
            shape = squircleShape
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun ClickItem(
    modifier: Modifier = Modifier,
    title: String,
    summary: String = "",
    @DrawableRes icon: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != 0) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .width(24.dp),
                tint = colorResource(R.color.icon)
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(text = title, fontSize = 16.sp, color = colorResource(id = R.color.text))
            if (summary.isNotEmpty()) {
                Text(text = summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.arrow_right_bold),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.icon)
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun SwitchItem(
    modifier: Modifier = Modifier,
    title: String,
    summary: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    @DrawableRes icon: Int = 0,
    onCheckedChange: (Boolean) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.toggle_button_anim))

    // 控制动画进度
    val progress by animateFloatAsState(
        targetValue = if (checked) 0.5f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "LottieProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 0.dp, top = 16.dp, bottom = 16.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null, // 去掉点击的视觉效果
                onClick = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != 0) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .width(24.dp),
                tint = colorResource(R.color.icon)
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                color = if (enabled) colorResource(id = R.color.text) else colorResource(id = R.color.text).copy(alpha = 0.38f)
            )
            if (summary.isNotEmpty()) {
                Text(text = summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(40.dp)
                .width(80.dp)
                .alpha(if (enabled) 1f else 0.5f)
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun EditTextItem(
    modifier: Modifier = Modifier,
    title: String,
    summary: String = "",
    dialogTitle: String,
    initialValue: String,
    hint: String = "",
    singleLine: Boolean = false,
    @DrawableRes icon: Int = 0,
    onCancel: () -> Unit = {},
    onConfirm: (String) -> Boolean // 返回true关闭窗口，返回false不关闭
) {
    var showDialog by remember { mutableStateOf(false) }

    ClickItem(title = title, summary = summary, modifier = modifier, icon = icon) {
        showDialog = true
    }

    if (showDialog) {
        EditTextDialog(
            title = dialogTitle,
            initialValue = initialValue,
            hint = hint,
            singleLine = singleLine,
            onCancel = onCancel,
            onDismissRequest = { showDialog = false },
            onConfirm = onConfirm
        )
    }
}

@Composable
fun EditTextDialog(
    title: String,
    initialValue: String,
    hint: String = "",
    singleLine: Boolean = false,
    type: String = "String",
    onCancel: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var tempText by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeight = with(density) { windowInfo.containerSize.width.toDp() }

    // 在组件加载时触发请求焦点
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()

        keyboardController?.show()
    }

    val submitAction = {
        if (onConfirm(tempText.text)) {
            onDismissRequest()
        }
    }

    val cancelAction = {
        onCancel()
        onDismissRequest()
    }

    StandardDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.heightIn(max = screenHeight * 0.6f) // 限制最大高度
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = tempText,
                onValueChange = { input ->
                    val inputText = input.text
                    when (type) {
                        "Int" -> {
                            if (inputText.all { it.isDigit() }) {
                                tempText = input
                            }
                        }
                        "Float" -> {
                            if (inputText.matches(Regex("^\\d*\\.?\\d*$"))) {
                                tempText = input
                            }
                        }
                        else -> tempText = input
                    } },
                placeholder = { Text(text = hint, color = Color.Gray) },
                singleLine = singleLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // 弹性高度
                    .focusRequester(focusRequester), // 焦点
                keyboardOptions = KeyboardOptions(
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                    keyboardType = if (type == "String") KeyboardType.Text else KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (singleLine) submitAction() }
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = squircleShape,
                    colors = ButtonDefaults.textButtonColors(Color.Transparent),
                    onClick = cancelAction
                ) {
                    Text("取消", color = Color.Gray)
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = squircleShape,
                    colors = ButtonDefaults.textButtonColors(colorResource(R.color.btn_confirm)),
                    onClick = submitAction
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
fun InquiryItem(
    modifier: Modifier = Modifier,
    title: String,
    summary: String,
    dialogTitle: String,
    dialogContent: String,
    cancelText: String = "取消",
    confirmText: String = "确认",
    @DrawableRes icon: Int = 0,
    onCancel: () -> Unit = {},
    onConfirm: () -> Boolean
) {
    var showDialog by remember { mutableStateOf(false) }

    ClickItem(title = title, summary = summary, modifier = modifier, icon = icon) {
        showDialog = true
    }

    if (showDialog) {
        InquiryDialog(
            title = dialogTitle,
            content = dialogContent,
            cancelText = cancelText,
            confirmText = confirmText,
            onCancel = onCancel,
            onConfirm = onConfirm,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
fun InquiryDialog(
    title: String,
    content: String,
    cancelText: String = "取消",
    confirmText: String = "确认",
    onCancel: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onConfirm: () -> Boolean
) {
    val confirmAction = {
        if (onConfirm()) {
            onDismissRequest()
        }
    }

    val cancelAction = {
        onCancel()
        onDismissRequest()
    }

    StandardDialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(enabled = false) {},
            shape = squircleShape,
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.bg_dialog)
            ),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (content.isNotEmpty()) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = squircleShape,
                        colors = ButtonDefaults.textButtonColors(Color.Transparent),
                        onClick = cancelAction
                    ) {
                        Text(text = cancelText, color = Color.Gray)
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = squircleShape,
                        colors = ButtonDefaults.textButtonColors(colorResource(R.color.btn_confirm)),
                        onClick = confirmAction
                    ) {
                        Text(text = confirmText, color = colorResource(R.color.text))
                    }
                }
            }
        }
    }
}

@Composable
fun ExplanationItem(
    modifier: Modifier = Modifier,
    title: String,
    content: String,
    buttonContent: String? = null,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = squircleShape,
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.bg_dialog)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tips),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )

                Text(
                    text = title,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    color = colorResource(id = R.color.text)
                )
            }


            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = colorResource(id = R.color.explanation_text),
                modifier = Modifier.padding(4.dp)
            )

            buttonContent?.let {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = squircleShape,
                    colors = ButtonDefaults.textButtonColors(colorResource(R.color.btn_confirm).copy(alpha = 0.8f)),
                    onClick = onButtonClick,
                ) {
                    Text(text = it, color = colorResource(R.color.text))
                }
            }
        }
    }
}

@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    hasPadding: Boolean = true,
    sharedTiltState: SharedTiltState,
    content: @Composable ColumnScope.() -> Unit //@Composable ColumnScope.() 让content知道自己的父组件是column
) {
    Box(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.background)).tiltGestureContainer(sharedTiltState)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(all = if (hasPadding) 12.dp else 0.dp)
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.top_padding)))

            content()

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_padding)))
        }
    }
}

@Composable
fun ButtonPro(
    @DrawableRes icon: Int,
    onTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
) {
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = Modifier.height(48.dp).width(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 2.dp,
                    shape = squircleShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black,
                )
                .background(
                    color = colorResource(R.color.mikuGreen),
                    shape = squircleShape
                )

                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val upEvent = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                        }
                        if (upEvent != null) {
                            onTap()
                        } else {
                            onLongPressStart()
                            waitForUpOrCancellation()
                            onLongPressEnd()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = "universal",
                modifier = Modifier.size(20.dp),
                tint = colorResource(R.color.icon)
            )
        }
    }
}

@Composable
fun AnimationButton(
    modifier: Modifier = Modifier,
    changed: Boolean,
    @RawRes animation: Int = 0,
    onClick: (Boolean) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animation))

    val progress by animateFloatAsState(
        targetValue = if (!changed) 0.5f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "LottieProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(changed) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Crop,
            dynamicProperties = rememberLottieDynamicProperties( // 所有图层染色
                rememberLottieDynamicProperty(
                    property = LottieProperty.COLOR_FILTER,
                    value = SimpleColorFilter(colorResource(R.color.sheet_button).toArgb()),
                    keyPath = arrayOf("**")
                )
            ),
            modifier = Modifier
                .height(40.dp)
                .width(80.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(
    title: String? = null,
    initialSelectedDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            BlurEffect()

            Surface(
                shape = squircleShape,
                color = colorResource(R.color.bg_dialog),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .clickable(enabled = false) {}
                    .scale(0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = {
                            Text(
                                text = title ?: "选择日期",
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = DatePickerDefaults.colors(
                            containerColor = colorResource(R.color.bg_dialog),
                            selectedDayContainerColor = colorResource(R.color.mikuGreen),
                            todayDateBorderColor = colorResource(R.color.mikuGreen),
                            selectedYearContainerColor = colorResource(R.color.mikuGreen)
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f),
                            colors = ButtonDefaults.textButtonColors(Color.Transparent),
                            onClick = onDismiss
                        ) {
                            Text(text = "取消", color = colorResource(R.color.text))
                        }

                        Button(
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f),
                            shape = squircleShape,
                            colors = ButtonDefaults.textButtonColors(colorResource(R.color.btn_confirm)),
                            onClick = {
                                onDateSelected(datePickerState.selectedDateMillis)
                                onDismiss()
                            }
                        ) {
                            Text(text = "确定", color = colorResource(R.color.text))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .tiltGestureContainer(sharedTiltState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            BlurEffect()

            Card(
                modifier = modifier
                    .applySharedTilt(sharedTiltState)
                    .clickable(enabled = false) {}, // 接收外部传入的 modifier
                shape = squircleShape,
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.bg_dialog)),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun rememberStringPreference(key: String, default: String = ""): MutableState<String> {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val state = remember { mutableStateOf(prefs.getString(key, default) ?: default) }

    return remember(state) {
        object : MutableState<String> {
            override var value: String
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    prefs.edit { putString(key, newValue) }
                }
            override fun component1() = value
            override fun component2(): (String) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberBooleanPreference(key: String, default: Boolean = false): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }

    return remember(state) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    prefs.edit { putBoolean(key, newValue) }
                }

            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}

@Composable
fun BlurEffect(blurRadius: Int = 24, blurDimAmount: Float = 0f, fallbackDimAmount: Float = 0.6f) {
    val context = LocalContext.current
    val view = LocalView.current

    val activityManager = remember(context) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    LaunchedEffect(Unit) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        dialogWindow?.let { window ->
            val isBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !activityManager.isLowRamDevice
            if (isBlur) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = blurRadius
                    dimAmount = blurDimAmount
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    dimAmount = fallbackDimAmount
                }
            }
        }
    }
}

@Stable
class SharedTiltState {
    var currentGlobalTouch by mutableStateOf<Offset?>(null)
}

@Composable
fun rememberSharedTiltState() = remember { SharedTiltState() }

fun Modifier.tiltGestureContainer(state: SharedTiltState): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull()

            if (change != null && change.pressed) {
                state.currentGlobalTouch = change.position // 获取屏幕绝对坐标
            } else {
                state.currentGlobalTouch = null
            }
        }
    }
}

@Composable
fun Modifier.applySharedTilt(
    state: SharedTiltState,
    maxTilt: Float = 1.5f
): Modifier {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    var myBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(state.currentGlobalTouch) {
        val touch = state.currentGlobalTouch
        // 关键判断：全局触摸点是否在这张卡片的全局 Bounds 内
        if (touch != null && myBounds.contains(touch)) {
            val centerX = myBounds.center.x
            val centerY = myBounds.center.y
            val normX = (touch.x - centerX) / (myBounds.width / 2f)
            val normY = (touch.y - centerY) / (myBounds.height / 2f)

            launch { rotX.snapTo(normY * -maxTilt) }
            launch { rotY.snapTo(normX * maxTilt) }
        } else {
            if (rotX.value != 0f || rotY.value != 0f) {
                launch { rotX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                launch { rotY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
            }
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            // 获取卡片在整个屏幕上的绝对位置。即使外层有 Scroll 滚动，这个位置也会实时更新！
            myBounds = coordinates.boundsInRoot()
        }
        .graphicsLayer {
            rotationX = rotX.value
            rotationY = rotY.value
            cameraDistance = 12f * density
        }
}

fun Modifier.glow(
    color: Color,
    blurRadius: Dp = 15.dp,
    spread: Dp = 0.dp,
    shape: Shape
) = this.drawBehind {
    val shadowColor = color.toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    val spreadPx = spread.toPx()
    val blurPx = blurRadius.toPx()

    this.drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        frameworkPaint.color = transparentColor // 形状本身透明
        frameworkPaint.setShadowLayer(
            blurPx,
            0f,
            0f,
            shadowColor
        )

        canvas.save()
        if (spreadPx > 0f) {
            val scaleX = (size.width + 2 * spreadPx) / size.width
            val scaleY = (size.height + 2 * spreadPx) / size.height

            canvas.scale(scaleX, scaleY, center.x, center.y)
        }

        val outline = shape.createOutline(size, layoutDirection, this@drawBehind)
        canvas.drawOutline(outline, paint)

        canvas.restore()
    }
}