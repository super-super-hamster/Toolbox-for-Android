package com.hamster.toolbox.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hamster.toolbox.MainViewModel
import com.hamster.toolbox.R
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

    fun jumpTo(id: String) {
        if (id.isNotEmpty()) {
            scope.launch {
                delay(400)
                targets[id]?.scrollTo(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tiltGestureContainer(sharedTiltState)
            .background(colorResource(id = R.color.background))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.top_padding)))

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
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_padding)))
    }
}