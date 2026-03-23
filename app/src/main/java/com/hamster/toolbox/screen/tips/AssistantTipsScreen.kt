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
fun AssistantTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    val uriHandler = LocalUriHandler.current

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem("使用助手功能前，请在“设置 -> 大模型 API”中填写 API Key。（当前仅适配 DeepSeek）")
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem("在 “设置 -> 热词” 中添加热词的可以让助手更容易识别")
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "前往 Deep Seek 开放平台", icon = R.drawable.ic_tips) {
                uriHandler.openUri("https://platform.deepseek.com/api_keys")
            }
        }
    }
}