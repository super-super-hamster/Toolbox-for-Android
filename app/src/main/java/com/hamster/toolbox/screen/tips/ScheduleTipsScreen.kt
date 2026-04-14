package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hamster.toolbox.R
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
                title = "添加课程",
                content = "在“设置 -> 导入课程表”中选择导入方式" +
                        " 1.通过自然语言导入： 通过描述课程信息的自然语言进行导入，至少包含课程名称和上课时间，也可包含地点和老师名称。(该功能需要API KEY)" +
                        " 2.通过JSON导入： 通过符合格式要求格式的JSON文本进行导入，具体要求见提示词。" +
                        " 3.复制提示词： 让大语言模型生成符合格式的提示词，直接将结果“通过自然语言导入”即可。"
            ) { }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

            ExplanationItem(
                title = "修改课程信息",
                content = "点击课程方格即可修改课程信息。" +
                        "点击空白方格可添加课程。"
            ) { }
        }
    }
}