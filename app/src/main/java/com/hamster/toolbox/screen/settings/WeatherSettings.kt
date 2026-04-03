package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.WeatherTips
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.rememberStringPreference

@Composable
fun WeatherSettings(
    onNavigate: (Route) -> Unit,
) {
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

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "天气 Tips", icon = R.drawable.ic_tips) {
                onNavigate(WeatherTips)
            }
        }
    }
}