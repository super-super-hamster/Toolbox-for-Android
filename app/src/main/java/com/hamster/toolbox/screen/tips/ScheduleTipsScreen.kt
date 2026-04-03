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
fun ScheduleTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(
                title = "JSON",
                content = "JSON 是一种通用的数据文本格式,通过“标签 : 内容”的形式来准确描述数据。\n“复制课程表提示词中包含对需求格式的描述，若无API建议使用外部AI生成。”"
            ) { }
        }
    }
}