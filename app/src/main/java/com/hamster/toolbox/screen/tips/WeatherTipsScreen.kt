package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.ClickItem
import com.hamster.toolbox.utils.compose.ExplanationItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.rememberSharedTiltState

@Composable
fun WeatherTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    val uriHandler = LocalUriHandler.current

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(
                title = "获取和风天气API KEY",
                content = "  1. 登录 和风天气控制台;\n" +
                    "  2. 进入“我的项目管理” -> 点击“创建项目” -> 填写名称并保存;\n" +
                    "  3. 点击“创建凭据” -> 填写凭据名称，身份认证方式选择 API KEY，保存后复制。" +
                    "注意：在应用限制部分，请不要选择“不限制”和“网站”以外的选项，否则请求将无法响应。" +
                        "应用内对和风天气的请求进行了5分钟的缓存，对于个人使用，每天的免费额度是完全足够的。",
                buttonContent = "前往 和风天气控制台"
            ) {
                uriHandler.openUri("https://console.qweather.com/home")
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(
                title = "获取和风天气API Host",
                content = "在 和风天气控制栏设置 页面中复制",
                buttonContent = "前往 和风天气控制台设置"
            ) {
                uriHandler.openUri("https://console.qweather.com/setting")
            }
        }
    }
}