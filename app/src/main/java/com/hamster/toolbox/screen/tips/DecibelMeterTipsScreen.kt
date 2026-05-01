package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hamster.toolbox.compose.ExplanationItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.R

@Composable
fun DecibelMeterTipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(title = "校准", content = "由于不同设备差异，需要进行校准以接近真实值。\n" +
                    "点击底部通用按钮即可开始校准。\n" +
                    "在安静环境（如隔音良好的图书馆)下，背景音大约30至40分贝。") { }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem(title = "提醒", content = "测量结果仅供参考。\n" +
                    "受硬件，校准等因素，测量结果不可避免与真实值有一定差距。") { }
        }
    }
}