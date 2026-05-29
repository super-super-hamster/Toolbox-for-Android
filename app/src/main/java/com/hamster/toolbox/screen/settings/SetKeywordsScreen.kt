package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.ai.KeywordManager
import com.hamster.toolbox.ai.KeywordsData
import com.hamster.toolbox.compose.EditTextDialog
import com.hamster.toolbox.compose.InquiryItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.VerticalScrollPageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.scrollTargetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetKeywordsScreen(
    onNavigate: (Route) -> Unit,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialList = remember { KeywordManager.loadCustomKeywords(context) }
    val keywordsList = remember {
        mutableStateListOf<KeywordsData>().apply { addAll(initialList) }
    }

    val sharedTiltState = rememberSharedTiltState()

    VerticalScrollPageColumn(
        sharedTiltState = sharedTiltState,
        scrollTarget = mainViewModel.settingsScrollTarget,
        scrollTrigger = mainViewModel.settingsScrollTrigger
    ) {
        ItemGroup(titleState = sharedTiltState) {
            keywordsList.forEachIndexed { index, keyword ->
                InquiryItem(
                    modifier = Modifier.scrollTargetId(keyword.word),
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

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "助手 Tips", icon = R.drawable.ic_tips) {
                onNavigate(AssistantTips)
            }
        }

        // 添加热词的 Dialog
        if (mainViewModel.isShowAddKeywordDialog) {
            EditTextDialog(
                title = "添加热词",
                initialValue = "",
                hint = "热词",
                singleLine = true,
                onDismissRequest = { mainViewModel.isShowAddKeywordDialog = false },
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