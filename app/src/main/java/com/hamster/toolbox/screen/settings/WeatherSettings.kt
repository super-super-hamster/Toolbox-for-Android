package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.EditTextItem
import com.hamster.toolbox.utils.ItemGroup
import com.hamster.toolbox.utils.rememberSharedTiltState
import com.hamster.toolbox.utils.rememberStringPreference
import com.hamster.toolbox.utils.tiltGestureContainer

@Composable
fun WeatherSettings() {
    val sharedTiltState = rememberSharedTiltState()

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var weatherApiKey by rememberStringPreference("weather_api_key", "")
    var weatherApiHost by rememberStringPreference("weather_api_host", "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tiltGestureContainer(sharedTiltState)
            .background(colorResource(id = R.color.background))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.top_padding)))

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

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_padding)))
    }
}