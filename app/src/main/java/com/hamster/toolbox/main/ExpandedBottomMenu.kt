package com.hamster.toolbox.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hamster.toolbox.GameConsole
import com.hamster.toolbox.R
import com.hamster.toolbox.RandomNumber
import com.hamster.toolbox.Route
import com.hamster.toolbox.Ruler
import com.hamster.toolbox.Schedule
import com.hamster.toolbox.SettingsGraph
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.utils.compose.TabItem
import com.hamster.toolbox.utils.compose.Tabs
import com.hamster.toolbox.utils.compose.assistantBubbleShape
import com.hamster.toolbox.utils.compose.squircleShape
import com.hamster.toolbox.utils.compose.userBubbleShape
import kotlinx.coroutines.launch

@Composable
fun ExpandedBottomMenu(
    apiKey: String?,
    selectedIndex: Int,
    mainViewModel: MainViewModel,
    setSelectedIndex: (Int) -> Unit,
    inputText: String,
    setInputText: (String) -> Unit,
    onNavigate: (route: Route) -> Unit
) {
    val tabsList = listOf(
        TabItem(title = "助手") {
            Assistant(
                mainViewModel = mainViewModel,
                inputText = inputText,
                setInputText = { setInputText(it) },
                onNavigate = { onNavigate(it) },
                apiKey = apiKey
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
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
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
            // 设置
            IconButton(onClick = { onNavigate(SettingsGraph) }) {
                Icon(painterResource(R.drawable.ic_settings), null, tint = Color.Gray)
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

    LaunchedEffect(chatSize) {
        if (chatSize > 0) {
            listState.animateScrollToItem(chatSize - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AI.chatHistory) { message ->
                if (message.role != "system") {
                    ChatBubble(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { setInputText(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 3,
                shape = squircleShape,
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
fun ChatBubble(message: Message) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) colorResource(R.color.user_bubble) else colorResource(R.color.mikuGreen),
            shape = if (isUser) userBubbleShape else assistantBubbleShape,
            modifier = Modifier
                .widthIn(max = 280.dp)
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
    }
}