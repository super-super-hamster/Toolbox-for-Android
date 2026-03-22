package com.hamster.toolbox.screen.tips

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.rememberSharedTiltState

@Composable
fun TipsScreen() {
    val sharedTiltState = rememberSharedTiltState()

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {

    }
}