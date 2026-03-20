package com.hamster.toolbox.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.EditTextItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.compose.rememberStringPreference

@Composable
fun WeatherSettings() {
    val sharedTiltState = rememberSharedTiltState()

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var weatherApiKey by rememberStringPreference("weather_api_key", "")
    var weatherApiHost by rememberStringPreference("weather_api_host", "")

    PageColumn(sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                title = "天气 API",
                summary = if (weatherApiKey.isEmpty()) "未设置" else "******",
                dialogTitle = "API Key",
                initialValue = weatherApiKey,
                hint = "输入 API KEY",
                singleLine = true,
                icon = R.drawable.ic_a,
                onCancel = { weatherApiKey = prefs.getString("weather_api_key", "") ?: "" },
                onConfirm = { input ->
                    weatherApiKey = input
                    prefs.edit { putString("weather_api_key", weatherApiKey) }
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
                onCancel = { weatherApiHost = prefs.getString("weather_api_host", "") ?: "" },
                onConfirm = { input ->
                    weatherApiHost = input
                    prefs.edit { putString("weather_api_host", weatherApiHost) }
                    true
                }
            )
        }
    }
}