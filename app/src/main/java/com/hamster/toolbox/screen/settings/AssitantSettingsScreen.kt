package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.SetKeywords
import com.hamster.toolbox.SettingsRepository
import com.hamster.toolbox.WeatherRepository
import com.hamster.toolbox.WeatherTips
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.OptionDialog
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.rememberStringPreference
import com.hamster.toolbox.settingsStore
import com.hamster.toolbox.weatherStore
import kotlinx.coroutines.launch

@Composable
fun AssistantSettingsScreen(
    setLoading: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit
) {
    val context = LocalContext.current
    val sharedTiltState = rememberSharedTiltState()
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val weatherRepository = remember { WeatherRepository(context.weatherStore) }
    val settingsRepository = remember { SettingsRepository(context.settingsStore) }

    var apiKey by rememberStringPreference("api_key", "")
    var assistantNickname by rememberStringPreference("assistant_nickname", "助手")

    var currentModelName by remember { mutableStateOf("")}
    val aiBalance by settingsRepository.aiBalanceFlow.collectAsStateWithLifecycle(initialValue = "无")
    val weatherApiKey by weatherRepository.weatherApiKeyFlow.collectAsStateWithLifecycle(initialValue = "")
    val weatherApiHost by weatherRepository.weatherApiHostFlow.collectAsStateWithLifecycle(initialValue = "")

    var showModelSelectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            currentModelName = settingsRepository.getAiModelName()
            if (currentModelName.isEmpty()) {
                currentModelName = "deepseek-v4-flash"
            }
        }
    }

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()),sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                title = "助手昵称",
                dialogTitle = "修改助手昵称",
                initialValue = assistantNickname,
                hint = "助手昵称",
                singleLine = true,
                icon = R.drawable.ic_user_name,
                onCancel = { assistantNickname = prefs.getString("assistant_nickname", "") ?: "" },
                onConfirm = { input ->
                    assistantNickname = input
                    prefs.edit { putString("assistant_nickname", assistantNickname) }
                    true
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                title = "大模型 API",
                summary = if (apiKey.isEmpty()) "未设置" else "******",
                dialogTitle = "API Key",
                initialValue = apiKey,
                hint = "输入 API Key",
                singleLine = true,
                icon = R.drawable.ic_key,
                onCancel = { apiKey = prefs.getString("api_key", "") ?: "" },
                onConfirm = { input ->
                    apiKey = input
                    prefs.edit { putString("api_key", apiKey) }
                    true
                }
            )

            ClickItem(
                title = "模型选择",
                summary = currentModelName,
                icon = R.drawable.ic_deepseek
            ) {
                showModelSelectDialog = true
            }

            ClickItem(
                title = "余额查询",
                summary = aiBalance,
                icon = R.drawable.ic_coin
            ) {
                coroutineScope.launch {
                    setLoading(true)
                    settingsRepository.setAiBalance(AI.getBalance(apiKey))
                    setLoading(false)
                }
            }

            ClickItem(
                title = "助手 Tips",
                icon = R.drawable.ic_tips
            ) {
                onNavigate(AssistantTips)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
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

            ClickItem(title = "天气 Tips", icon = R.drawable.ic_tips) {
                onNavigate(WeatherTips)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(
                title = "热词",
                summary = "热词更容易被语音识别",
                icon = R.drawable.ic_characters
            ) {
                onNavigate(SetKeywords)
            }
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
                        settingsRepository.setAiModelName(currentModelName)
                    }
                }
            )
        }
    }
}