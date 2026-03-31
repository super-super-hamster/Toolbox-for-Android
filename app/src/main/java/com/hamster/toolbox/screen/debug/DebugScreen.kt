package com.hamster.toolbox.screen.debug

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.system.Receiver
import com.hamster.toolbox.utils.compose.ClickItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.rememberSharedTiltState

@Composable
fun DebugScreen(
    setLoading: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()),sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "通知测试") {
                val receiver = Receiver()
                receiver.showNotification(context, "标题", "内容", "按钮文字", null)
            }

            ClickItem(title = "加载动画测试") {
                setLoading(true)
            }

            ClickItem(title = "设置闹钟测试") {
                val alarm = Alarm()
                alarm.setAlarm(context, 9,0)
            }
        }
    }
}