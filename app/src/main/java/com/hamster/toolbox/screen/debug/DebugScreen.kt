package com.hamster.toolbox.screen.debug

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.system.Receiver
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.SwitchItem
import com.hamster.toolbox.compose.rememberBooleanPreference
import com.hamster.toolbox.compose.rememberSharedTiltState

@Composable
fun DebugScreen(
    setLoading: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val sharedTiltState = rememberSharedTiltState()

    var showPackageName by rememberBooleanPreference("super_hamster_show_package_name", false)

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

        ItemGroup(titleState = sharedTiltState) {
            SwitchItem(title = "时间页面显示包名", checked = showPackageName) {
                showPackageName = it
            }

            ClickItem(title = "恢复时间页面初始化") {
                prefs.edit { putBoolean("has_init_invisible_apps", false)}
            }
        }
    }
}