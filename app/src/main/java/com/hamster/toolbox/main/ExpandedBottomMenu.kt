package com.hamster.toolbox.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.hamster.toolbox.ColorPicker
import com.hamster.toolbox.Debug
import com.hamster.toolbox.DecibelMeter
import com.hamster.toolbox.DiaryPreview
import com.hamster.toolbox.R
import com.hamster.toolbox.RandomNumber
import com.hamster.toolbox.Route
import com.hamster.toolbox.Ruler
import com.hamster.toolbox.Schedule
import com.hamster.toolbox.SettingsGraph
import com.hamster.toolbox.Time
import com.hamster.toolbox.Tips
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.compose.TabItem
import com.hamster.toolbox.compose.Tabs
import com.hamster.toolbox.compose.assistantBubbleShape
import com.hamster.toolbox.compose.rememberStringPreference
import com.hamster.toolbox.compose.squircleShape
import com.hamster.toolbox.compose.userBubbleShape
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ExpandedBottomMenu(
    apiKey: String?,
    selectedIndex: Int,
    mainViewModel: MainViewModel,
    setSelectedIndex: (Int) -> Unit,
    inputText: String,
    setInputText: (String) -> Unit,
    onNavigate: (route: Route) -> Unit,
    onDragDown: () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val tabsList = listOf(
        TabItem(title = "助手") {
            Assistant(
                mainViewModel = mainViewModel,
                inputText = inputText,
                setInputText = { setInputText(it) },
                onNavigate = { onNavigate(it) },
                onDragDown = onDragDown,
                apiKey = apiKey,
            )
        },
        TabItem(title = "菜单") {
            Menu(onNavigate = onNavigate)
        }
    )

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // 消耗事件，防止滑动事件穿透给底部组件
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        if (dragOffset >= 150f) {
                            onDragDown()
                        }
                    }
                )
            }
    ) {
        Tabs(
            tabs = tabsList,
            selectedIndex = selectedIndex,
            setSelectedIndex = { setSelectedIndex(it) }
        )
    }
}

@Composable
private fun Menu(
    onNavigate: (route: Route) -> Unit
) {
    val userName by rememberStringPreference("nickname", "")

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 课程表
            MenuItem(name = "课程表", icon = painterResource(R.drawable.ic_calendar), onClick = { onNavigate(Schedule) })

            // 尺子
            MenuItem(name = "尺子", icon = painterResource(R.drawable.ic_ruler), onClick = { onNavigate(Ruler) })

            // 随机数
            MenuItem(name = "随机数", icon = painterResource(R.drawable.ic_numbers), onClick = { onNavigate(RandomNumber) })
            // 游戏
//            IconButton(onClick = { onNavigate(GameConsole) }) {
//                Icon(painterResource(R.drawable.ic_game_console), null, tint = Color.Gray)
//            }
            // 取色器
            MenuItem(name = "取色器", icon = painterResource(R.drawable.ic_color_picker), onClick = { onNavigate(ColorPicker) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 时间
            MenuItem(name = "时间", icon = painterResource(R.drawable.ic_time), onClick = { onNavigate(Time) })

            // Tips
            MenuItem(name = "Tips", icon = painterResource(R.drawable.ic_tips), onClick = { onNavigate(Tips) })

            // 设置
            MenuItem(name = "设置", icon = painterResource(R.drawable.ic_settings), onClick = { onNavigate(SettingsGraph) })

            // 日记
            MenuItem(name = "日记", icon = painterResource(R.drawable.ic_diary), onClick = { onNavigate(DiaryPreview) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 分贝仪
            MenuItem(name = "分贝仪", icon = painterResource(R.drawable.ic_decibel_meter), onClick = { onNavigate(DecibelMeter) })

            // Debug
            if (userName == "SuperHamster") {
                MenuItem(name = "Debug", icon = painterResource(R.drawable.ic_debug), onClick = { onNavigate(Debug) })
            }
        }
    }
}

@Composable
fun Assistant(
    inputText: String,
    mainViewModel: MainViewModel,
    setInputText: (String) -> Unit,
    onNavigate: (route: Route) -> Unit,
    onDragDown: () -> Unit,
    apiKey: String?
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val listState = rememberLazyListState()
    val chatSize = AI.chatHistory.size

    var isSending by remember { mutableStateOf(false) }

    val userAvatarPath by rememberStringPreference("user_avatar_path", "")

    var flyingText by remember { mutableStateOf("") }
    val flyOffsetX = remember { Animatable(0f) }
    val flyAlpha = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 位于顶部且由用户划动而非惯性时
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    onDragDown()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(chatSize) {
        if (chatSize > 0) {
            listState.animateScrollToItem(chatSize - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color.Gray,
                    shape = squircleShape
                )
                .padding(horizontal = 8.dp)
                .nestedScroll(nestedScrollConnection), // 顶部继续下划
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(AI.chatHistory) { index, message ->
                if (message.role != "system") {
                    val isLastMessage = index == AI.chatHistory.size - 1

                    var isBubbleVisible by remember { mutableStateOf(!isLastMessage) }

                    LaunchedEffect(message) {
                        isBubbleVisible = true
                    }

                    AnimatedVisibility(
                        visible = isBubbleVisible,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight / 2 },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 400)
                        )
                    ) {
                        ChatBubble(message = message, userAvatarPath = userAvatarPath)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { setInputText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("...") },
                    maxLines = 3,
                    shape = squircleShape,
                    keyboardOptions = KeyboardOptions(
                        hintLocales = LocaleList(Locale("zh"))
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.mikuGreen),
                        unfocusedBorderColor = Color.LightGray,
                        errorBorderColor = Color.Red,
                        disabledBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = colorResource(R.color.mikuGreen),
                        cursorColor = colorResource(R.color.mikuGreen)
                    )
                )

                if (flyingText.isNotEmpty()) {
                    Text(
                        text = flyingText,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp) // 匹配 TextField 的内边距
                            .offset { IntOffset(flyOffsetX.value.roundToInt(), 0) }
                            .alpha(flyAlpha.value),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                enabled = !isSending,
                onClick = {
                    if (inputText.isNotBlank() && flyingText.isEmpty()) {
                        setInputText("")
                        isSending = true

                        coroutineScope.launch {
                            flyingText = inputText
                            flyAlpha.snapTo(1f)
                            flyOffsetX.snapTo(0f)

                            val moveJob = launch {
                                flyOffsetX.animateTo(150f, animationSpec = tween(300))
                            }
                            val fadeJob = launch {
                                flyAlpha.animateTo(0f, animationSpec = tween(300))
                            }

                            joinAll(moveJob, fadeJob)
                            flyingText = ""

                            mainViewModel.viewModelScope.launch {
                                try {
                                    AI.chatWithAssistant(context, mainViewModel, inputText, apiKey) { onNavigate(it) }
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorResource(R.color.mikuGreen),
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(R.color.mikuGreen).copy(alpha = 0.75f),
                    disabledContentColor = Color.White
                )
            ) {
                Icon(painter = painterResource(R.drawable.ic_send), contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: Message,
    userAvatarPath: String,
) {
    val isUser = message.role == "user"

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val maxBubbleWidth = with(density) {
        (windowInfo.containerSize.width * 0.6f).toDp()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Icon(
                modifier = Modifier.padding(end = 8.dp).size(48.dp).clip(CircleShape),
                painter = painterResource(R.drawable.ic_assistant_chat),
                contentDescription = null
            )
        }

        Surface(
            color = if (isUser) colorResource(R.color.user_bubble) else colorResource(R.color.mikuGreen),
            shape = if (isUser) userBubbleShape else assistantBubbleShape,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .shadow(
                    elevation = 6.dp,
                    shape = if (isUser) userBubbleShape else assistantBubbleShape,
                    clip = false,
                    spotColor = colorResource(id = R.color.item_group_card_shadow),
                    ambientColor = colorResource(id = R.color.item_group_card_shadow)
                )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = colorResource(R.color.text)
            )
        }

        if (isUser) {
            UserAvatar(avatarPath = userAvatarPath)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun UserAvatar(avatarPath: String?) {
    val hasCustomAvatar = !avatarPath.isNullOrEmpty() && File(avatarPath).exists()

    if (hasCustomAvatar) {
        AsyncImage(
            model = avatarPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.default_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun MenuItem(
    name: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Gray
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // 去掉点击的视觉效果
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            fontSize = 8.sp,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}