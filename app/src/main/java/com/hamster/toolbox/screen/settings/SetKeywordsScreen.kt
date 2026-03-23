package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.ai.KeywordManager
import com.hamster.toolbox.ai.KeywordsData
import com.hamster.toolbox.utils.compose.EditTextDialog
import com.hamster.toolbox.utils.compose.InquiryItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.ScrollTarget
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//TODO:搜索

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetKeywordsScreen(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialList = remember { KeywordManager.loadCustomKeywords(context) }
    val keywordsList = remember {
        mutableStateListOf<KeywordsData>().apply { addAll(initialList) }
    }

    val targets = remember { mutableMapOf<String, ScrollTarget>() }
    val sharedTiltState = rememberSharedTiltState()

    fun getModifier(id: String): Modifier {
        return targets.getOrPut(id) { ScrollTarget() }.modifier
    }

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()),sharedTiltState = sharedTiltState) {
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