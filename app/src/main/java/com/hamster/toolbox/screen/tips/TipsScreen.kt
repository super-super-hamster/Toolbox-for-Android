package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.ScheduleTips
import com.hamster.toolbox.WeatherTips
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState

@Composable
fun TipsScreen(
    onNavigate: (Route) -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "课程表", icon = R.drawable.ic_curriculum) {
                onNavigate(ScheduleTips)
            }

            ClickItem(title = "助手", icon = R.drawable.ic_assistant) {
                onNavigate(AssistantTips)
            }

            ClickItem(title = "天气", icon = R.drawable.ic_weather) {
                onNavigate(WeatherTips)
            }
        }
    }
}