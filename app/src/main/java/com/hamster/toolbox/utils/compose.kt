package com.hamster.toolbox.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.toolbox.R
import kotlinx.coroutines.launch
import sv.lib.squircleshape.CornerSmoothing
import sv.lib.squircleshape.SquircleShape
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp

val squircleShape = SquircleShape(
    radius = 16.dp,
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
                    elevation = 4.dp,
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
fun BackItem(
    title: String = "返回",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 0.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.arrow_left_bold),
            contentDescription = "Back",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = colorResource(id = R.color.title_text)
        )
    }
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 0.dp, top = 16.dp, bottom = 16.dp)
            .clickable(
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

// TODO:弹
@Composable
fun EditTextDialog(
    title: String,
    initialValue: String,
    hint: String = "",
    singleLine: Boolean = false,
    onCancel: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var tempText by remember { mutableStateOf(initialValue) }

    val submitAction = {
        if (onConfirm(tempText)) {
            onDismissRequest()
        }
    }

    val cancelAction = {
        onCancel()
        onDismissRequest()
    }

    val dismissAction = {
        onDismissRequest()
    }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissAction
                    )
            ) {
                BlurEffect()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        placeholder = { Text(text = hint, color = Color.Gray) },
                        singleLine = singleLine,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
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

    val dismissAction = {
        onDismissRequest()
    }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissAction
                    )
            ) {
                BlurEffect()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
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
}

@Composable
fun ExplanationItem(
    content: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = squircleShape,
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.bg_explanation)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.question),
                contentDescription = "tip",
                modifier = Modifier.size(20.dp),
                tint = colorResource(R.color.explanation_text)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = colorResource(id = R.color.explanation_text)
            )
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
        modifier = Modifier.size(72.dp),
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
                .innerShadow(
                    shape = squircleShape,
                    color = colorResource(R.color.inner_shadow),
                    blur = 2.5.dp,
                    offsetX = 0.dp,
                    offsetY = 1.dp,
                    spread = 3.dp
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            ) {
                BlurEffect()
            }

            Surface(
                shape = squircleShape,
                color = colorResource(R.color.bg_dialog),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(24.dp)
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
    fun onTouch(offset: Offset?) {
        currentGlobalTouch = offset
    }
}

@Composable
fun rememberSharedTiltState(): SharedTiltState {
    return remember { SharedTiltState() }
}

fun Modifier.tiltGestureContainer(state: SharedTiltState): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull()

            if (change != null && change.pressed) {
                state.onTouch(change.position)
            } else {
                state.onTouch(null)
            }
        }
    }
}

@Composable
fun Modifier.applySharedTilt(
    state: SharedTiltState,
    maxTilt: Float = 5f
): Modifier {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }

    var myBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        snapshotFlow { state.currentGlobalTouch }.collect { touch ->
            if (touch != null && myBounds.contains(touch)) {
                val centerX = myBounds.center.x
                val centerY = myBounds.center.y

                val normX = (touch.x - centerX) / (myBounds.width / 2f)
                val normY = (touch.y - centerY) / (myBounds.height / 2f)

                launch { rotX.snapTo(normY * -maxTilt) }
                launch { rotY.snapTo(normX * maxTilt) }
            } else {
                if (rotX.value != 0f && rotY.value != 0f) {
                    launch { rotX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                    launch { rotY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                }
            }
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            myBounds = coordinates.boundsInRoot()
        }
        .graphicsLayer {
            rotationX = rotX.value
            rotationY = rotY.value
            cameraDistance = 12f * density
        }
}

fun Modifier.innerShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.2f),
    blur: Dp = 4.dp,
    offsetY: Dp = 2.dp, // 向下偏移
    offsetX: Dp = 2.dp, // 向右偏移
    spread: Dp = 0.dp
) = this.drawWithContent {
    drawContent()

    drawIntoCanvas { canvas ->
        val shadowSize = size
        val shadowOutline = shape.createOutline(shadowSize, layoutDirection, this)

        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().apply {
                maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }

        canvas.save()

        canvas.clipPath(Path().apply { addOutline(shadowOutline) })

        canvas.translate(offsetX.toPx(), offsetY.toPx())

        val strokeWidth = blur.toPx() * 2
        paint.asFrameworkPaint().style = android.graphics.Paint.Style.STROKE
        paint.asFrameworkPaint().strokeWidth = strokeWidth

        canvas.drawOutline(
            shape.createOutline(size, layoutDirection, this),
            paint
        )

        canvas.restore()
    }
}