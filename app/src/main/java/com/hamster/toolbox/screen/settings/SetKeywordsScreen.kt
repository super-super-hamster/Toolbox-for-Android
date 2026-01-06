package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.hamster.toolbox.R
import com.hamster.toolbox.MainViewModel
import com.hamster.toolbox.ai.KeywordManager
import com.hamster.toolbox.ai.KeywordsData
import com.hamster.toolbox.utils.EditTextDialog
import com.hamster.toolbox.utils.InquiryItem
import com.hamster.toolbox.utils.ItemGroup
import com.hamster.toolbox.utils.ScrollTarget
import com.hamster.toolbox.utils.rememberSharedTiltState
import com.hamster.toolbox.utils.tiltGestureContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetKeywordsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 状态初始化
    // 建议：loadCustomKeywords 最好是挂起函数放在 LaunchedEffect 里，或者由 ViewModel 加载。
    // 这里为了保持原逻辑，暂时放在 remember 中 (注意：如果数据量大，可能会卡顿一下)
    val initialList = remember { KeywordManager.loadCustomKeywords(context) }
    val keywordsList = remember {
        mutableStateListOf<KeywordsData>().apply { addAll(initialList) }
    }

    // 滚动目标逻辑
    val targets = remember { mutableMapOf<String, ScrollTarget>() }
    val sharedTiltState = rememberSharedTiltState()

    fun getModifier(id: String): Modifier {
        return targets.getOrPut(id) { ScrollTarget() }.modifier
    }

    fun jumpTo(id: String) {
        if (id.isNotEmpty()) {
            scope.launch {
                delay(400)
                targets[id]?.scrollTo(context)
            }
        }
    }

    // UI 布局
    MaterialTheme {
        CompositionLocalProvider(
            LocalOverscrollFactory provides null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tiltGestureContainer(sharedTiltState)
                    .background(colorResource(id = R.color.background))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ItemGroup(titleState = sharedTiltState) {
                    keywordsList.forEachIndexed { index, keyword ->
                        InquiryItem(
                            modifier = getModifier(keyword.word),
                            title = keyword.word,
                            summary = "",
                            dialogTitle = "\"" + keyword.word + "\"",
                            dialogContent = "是否删除？",
                            onConfirm = {
                                return@InquiryItem try {
                                    keywordsList.removeAt(index)

                                    scope.launch(Dispatchers.IO) {
                                        KeywordManager.removeKeyword(context, keyword.word)
                                    }
                                    true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                            }
                        )
                    }
                }

                // 添加热词的 Dialog
                if (viewModel.isShowAddKeywordDialog) {
                    EditTextDialog(
                        title = "添加热词",
                        initialValue = "",
                        hint = "热词",
                        singleLine = true,
                        onDismissRequest = { viewModel.isShowAddKeywordDialog = false },
                        onConfirm = { input ->
                            return@EditTextDialog try {
                                val newData = KeywordsData(input, 5f)

                                keywordsList.add(newData)

                                scope.launch(Dispatchers.IO) {
                                    KeywordManager.addKeyword(context, newData)
                                }
                                true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                        }
                    )
                }
            }
        }
    }
}