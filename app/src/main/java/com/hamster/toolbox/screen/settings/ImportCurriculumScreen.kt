package com.hamster.toolbox.screen.settings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.preference.PreferenceManager
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.rememberStringPreference
import com.hamster.toolbox.utils.copyCurriculumJSONPrompt
import com.hamster.toolbox.utils.validateAndSaveJson
import kotlinx.coroutines.launch
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.ScheduleTips

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImportCurriculumScreen(
    onShowLoading: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit,
    onNavigateToSettings: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sharedTiltState = rememberSharedTiltState()
    var naturalLanguage by remember { mutableStateOf("") }

    var jsonString by rememberStringPreference("schedule_json")

    PageColumn(sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                title = "通过自然语言导入",
                dialogTitle = "输入自然语言",
                initialValue = naturalLanguage,
                hint = "请输入课程信息",
                onConfirm = { input ->
                    scope.launch {
                        try {
                            onShowLoading(true)
                            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                            val apiKey = prefs?.getString("api_key", null)
                            if (apiKey == null) {
                                onNavigateToSettings("api_key")
                            } else {
                                val response = AI.sendWithPrompt(context, input, "import_curriculum", apiKey)
                                validateAndSaveJson(response?.content, context)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            onShowLoading(false)
                        }
                    }
                    true
                }
            )
            EditTextItem(
                title = "通过JSON导入",
                summary = "将JSON文本导入为课程表",
                dialogTitle = "输入JSON",
                initialValue = jsonString,
                hint = "请输入符合格式要求的JSON文本",
                onConfirm = { input ->
                    validateAndSaveJson(input, context)
                    true
                }
            )
            ClickItem(title = "复制提示词", summary = "通过外部AI生成符合要求的JSON文本") {
                copyCurriculumJSONPrompt(context)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "课程表 Tips", icon = R.drawable.ic_tips) {
                onNavigate(ScheduleTips)
            }
        }
    }
}