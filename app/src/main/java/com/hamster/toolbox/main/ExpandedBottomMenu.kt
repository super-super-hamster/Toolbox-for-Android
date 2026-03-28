package com.hamster.toolbox.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import com.hamster.toolbox.Debug
import com.hamster.toolbox.GameConsole
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
import com.hamster.toolbox.utils.compose.TabItem
import com.hamster.toolbox.utils.compose.Tabs
import com.hamster.toolbox.utils.compose.assistantBubbleShape
import com.hamster.toolbox.utils.compose.rememberStringPreference
import com.hamster.toolbox.utils.compose.squircleShape
import com.hamster.toolbox.utils.compose.userBubbleShape
import kotlinx.coroutines.launch
import java.io.File

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
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

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
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 课程表
            IconButton(onClick = { onNavigate(Schedule) }) {
                Icon(painterResource(R.drawable.ic_calendar), null, tint = Color.Gray)
            }
            // 尺子
            IconButton(onClick = { onNavigate(Ruler) }) {
                Icon(painterResource(R.drawable.ic_ruler), null, tint = Color.Gray)
            }
            // 随机数
            IconButton(onClick = { onNavigate(RandomNumber) }) {
                Icon(painterResource(R.drawable.ic_numbers), null, tint = Color.Gray)
            }
            // 游戏
            IconButton(onClick = { onNavigate(GameConsole) }) {
                Icon(painterResource(R.drawable.ic_game_console), null, tint = Color.Gray)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 时间
            IconButton(onClick = { onNavigate(Time) }) {
                Icon(painterResource(R.drawable.ic_time), null, tint = Color.Gray)
            }
            // Tips
            IconButton(onClick = { onNavigate(Tips) }) {
                Icon(painterResource(R.drawable.ic_tips), null, tint = Color.Gray)
            }
            // 设置
            IconButton(onClick = { onNavigate(SettingsGraph) }) {
                Icon(painterResource(R.drawable.ic_settings), null, tint = Color.Gray)
            }

            if (userName == "SuperHamster") {
                IconButton(onClick = { onNavigate(Debug) }) {
                    Icon(painterResource(R.drawable.ic_debug), null, tint = Color.Gray)
                }
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
    apiKey: String?
) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val listState = rememberLazyListState()
    val chatSize = AI.chatHistory.size

    val userAvatarPath by rememberStringPreference("user_avatar_path", "")

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
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AI.chatHistory) { message ->
                if (message.role != "system") {
                    ChatBubble(message = message, userAvatarPath = userAvatarPath)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { setInputText(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 3,
                shape = squircleShape,
                keyboardOptions = KeyboardOptions(
                    // 默认中文输入法
                    hintLocales = LocaleList(Locale("zh"))
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    // 边框颜色
                    focusedBorderColor = colorResource(R.color.text),
                    unfocusedBorderColor = Color.LightGray,

                    // 背景颜色
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,

                    // 光标颜色
                    cursorColor = Color.LightGray,

                    // 文字颜色
                    focusedTextColor = colorResource(R.color.text),
                    unfocusedTextColor = colorResource(R.color.text)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        setInputText("")

                        coroutineScope.launch {
                            AI.chatWithAssistant(context, mainViewModel, inputText, apiKey) { onNavigate(it) }
                        }
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorResource(R.color.icon),
                    contentColor = Color.White
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