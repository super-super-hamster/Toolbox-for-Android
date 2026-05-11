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
fun DiaryTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(title = "创建日记", content = "点击底部通用按钮即可创建日记。\n" +
                    "在此基础上，你可以选择不同的日期或者设置一个标题。") { }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(title = "隐私", content = "可以在设置中打开“使用密码保护日记”，这将在通过设备密码验证后才可进入日记页面。\n" +
                    "保证日记内容仅保存在本地，且不会以任何形式（如网络）传播到其他设备上，如果不放心可关闭网络等权限。") {
                // TODO: 前往设置
            }
        }
    }
}