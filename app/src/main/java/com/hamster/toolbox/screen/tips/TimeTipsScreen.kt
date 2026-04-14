package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hamster.toolbox.compose.ExplanationItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState

@Composable
fun TimeTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(title = "显示与隐藏", content = "点击底部通用按钮可切换时间统计页面和设置可见性页面，在设置可见性页面可以隐藏不需要展示的应用。") { }
        }
    }
}