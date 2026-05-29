package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.SetKeywords
import com.hamster.toolbox.repository.SettingsRepository
import com.hamster.toolbox.repository.WeatherRepository
import com.hamster.toolbox.WeatherTips
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.OptionDialog
import com.hamster.toolbox.compose.SliderItem
import com.hamster.toolbox.compose.VerticalScrollPageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.scrollTargetId
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.repository.repositorySetFloat
import com.hamster.toolbox.repository.repositorySetString
import com.hamster.toolbox.repository.settingsStore
import com.hamster.toolbox.repository.weatherStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AssistantSettingsScreen(
    mainViewModel: MainViewModel,
    setLoading: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit
) {
    val context = LocalContext.current
    val sharedTiltState = rememberSharedTiltState()
    val coroutineScope = rememberCoroutineScope()

    // 仓库
    val weatherRepository = remember { WeatherRepository(context.weatherStore) }
    val settingsRepository = remember { SettingsRepository(context.settingsStore) }

    var currentModelName by remember { mutableStateOf("")}
    val aiApiKey by settingsRepository.aiApiKeyFlow.collectAsStateWithLifecycle(initialValue = "")
    val aiBalance by settingsRepository.aiBalanceFlow.collectAsStateWithLifecycle(initialValue = "无")
    val assistantName by settingsRepository.assistantNameFlow.collectAsStateWithLifecycle(initialValue = "助手")
    val weatherApiKey by weatherRepository.weatherApiKeyFlow.collectAsStateWithLifecycle(initialValue = "")
    val weatherApiHost by weatherRepository.weatherApiHostFlow.collectAsStateWithLifecycle(initialValue = "")
    var temperatureValue by remember { mutableFloatStateOf(1.0f) }

    var showModelSelectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        temperatureValue = settingsRepository.getAiTemperature()
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            currentModelName = settingsRepository.getAiModelName()
            if (currentModelName.isEmpty()) {
                currentModelName = "deepseek-v4-flash"
            }
        }
    }

    VerticalScrollPageColumn(
        sharedTiltState = sharedTiltState,
        scrollTrigger = mainViewModel.settingsScrollTrigger,
        scrollTarget = mainViewModel.settingsScrollTarget
    ) {
        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                modifier = Modifier.scrollTargetId("assistant_name"),
                title = "助手昵称",
                summary = assistantName,
                dialogTitle = "修改助手昵称",
                initialValue = assistantName,
                hint = "助手昵称",
                maxLength = 10,
                singleLine = true,
                icon = R.drawable.ic_user_name,
                onCancel = { },
                onConfirm = { input ->
                    coroutineScope.launch {
                        repositorySetString(context.settingsStore, input, SettingsRepository.ASSISTANT_NAME)
                    }
                    true
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                modifier = Modifier.scrollTargetId("api_key"),
                title = "大模型 API",
                summary = if (aiApiKey.isEmpty()) "未设置" else "******",
                dialogTitle = "API Key",
                initialValue = aiApiKey,
                hint = "输入 API Key",
                singleLine = true,
                icon = R.drawable.ic_key,
                onCancel = { },
                onConfirm = { input ->
                    coroutineScope.launch {
                        repositorySetString(context.settingsStore, input, SettingsRepository.AI_API_KEY)
                    }
                    true
                }
            )

            ClickItem(
                modifier = Modifier.scrollTargetId("model_name"),
                title = "模型选择",
                summary = currentModelName,
                icon = R.drawable.ic_deepseek
            ) {
                showModelSelectDialog = true
            }

            ClickItem(
                modifier = Modifier.scrollTargetId("get_balance"),
                title = "余额查询",
                summary = aiBalance,
                icon = R.drawable.ic_coin
            ) {
                coroutineScope.launch {
                    setLoading(true)
                    repositorySetString(context.settingsStore, AI.getBalance(aiApiKey), SettingsRepository.AI_BALANCE)
                    setLoading(false)
                }
            }

            ClickItem(
                modifier = Modifier.scrollTargetId("assistant_tips"),
                title = "助手 Tips",
                icon = R.drawable.ic_tips
            ) {
                onNavigate(AssistantTips)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                modifier = Modifier.scrollTargetId("weather_api_key"),
                title = "天气 API",
                summary = if (weatherApiKey.isEmpty()) "未设置" else "******",
                dialogTitle = "API Key",
                initialValue = weatherApiKey,
                hint = "输入 API KEY",
                singleLine = true,
                icon = R.drawable.ic_key,
                onCancel = { },
                onConfirm = { input ->
                    coroutineScope.launch {
                        weatherRepository.setApiKey(input)
                    }
                    true
                }
            )

            EditTextItem(
                modifier = Modifier.scrollTargetId("weather_api_host"),
                title = "天气 Host",
                summary = if (weatherApiHost.isEmpty()) "未设置" else "******",
                dialogTitle = "API Host",
                initialValue = weatherApiHost,
                hint = "输入 API Host",
                singleLine = true,
                icon = R.drawable.ic_a,
                onCancel = { },
                onConfirm = { input ->
                    coroutineScope.launch {
                        weatherRepository.setApiHost(input)
                    }
                    true
                }
            )

            ClickItem(title = "天气 Tips", icon = R.drawable.ic_tips, modifier = Modifier.scrollTargetId("weather_tips")) {
                onNavigate(WeatherTips)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(
                modifier = Modifier.scrollTargetId("keywords"),
                title = "热词",
                summary = "热词更容易被语音识别",
                icon = R.drawable.ic_characters
            ) {
                onNavigate(SetKeywords)
            }

            SliderItem(
                modifier = Modifier.scrollTargetId("ai_temperature"),
                title = "温度",
                icon = R.drawable.ic_thermometer,
                dialogTitle = "修改温度",
                summary = "温度越高输出越随机，反之则越稳定",
                dialogContent = "$temperatureValue",
                value = temperatureValue,
                onValueChange = { temperatureValue = (it * 10f).roundToInt() / 10f },
                valueRange = 0f..2f,
                onConfirm = {
                    coroutineScope.launch {
                        repositorySetFloat(context.settingsStore, temperatureValue, SettingsRepository.AI_TEMPERATURE)
                    }
                    true
                }
            )
        }

        if (showModelSelectDialog && currentModelName.isNotEmpty()) {
            OptionDialog(
                title = "选择模型",
                options = listOf("deepseek-v4-flash", "deepseek-v4-pro"),
                initialSelections = if (currentModelName == "deepseek-v4-flash") setOf(0) else setOf(1),
                singleSelect = true,
                onDismissRequest = { showModelSelectDialog = false },
                onConfirm = {
                    currentModelName = if (it.first() == 0) "deepseek-v4-flash" else "deepseek-v4-pro"
                    coroutineScope.launch {
                        repositorySetString(context.settingsStore, currentModelName, SettingsRepository.AI_MODEL_NAME)
                    }
                }
            )
        }
    }
}