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
import com.hamster.toolbox.compose.ExplanationItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState

@Composable
fun AssistantTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    val uriHandler = LocalUriHandler.current

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(
                title = "API KEY",
                content = "使用助手功能前，请在“设置 -> 大模型 API”中填写 API Key。（当前仅适配 DeepSeek）",
                buttonContent = "前往 Deep Seek 开放平台"
            ) {
                uriHandler.openUri("https://platform.deepseek.com/api_keys")
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(
                title = "热词",
                content = "在 “设置 -> 热词” 中添加热词的可以让助手更容易从语音中识别") { }
        }
    }
}