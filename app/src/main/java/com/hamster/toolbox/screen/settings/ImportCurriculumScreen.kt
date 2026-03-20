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
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.utils.compose.ClickItem
import com.hamster.toolbox.utils.compose.EditTextItem
import com.hamster.toolbox.utils.compose.ExplanationItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.copyCurriculumJSONPrompt
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.compose.rememberStringPreference
import com.hamster.toolbox.utils.validateAndSaveJson
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImportCurriculumScreen(
    onShowLoading: (Boolean) -> Unit,
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
                hint = "请输入包含课程信息，至少包含课程名称和上课时间，也可包含地点和老师名称",
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

        Spacer(modifier = Modifier.height(8.dp))

        ItemGroup(titleState = sharedTiltState) {
            ExplanationItem("JSON 是一种通用的数据文本格式,通过“标签 : 内容”的形式来准确描述数据。\n“复制课程表提示词中包含对需求格式的描述，若无API建议使用外部AI生成。”")
        }
    }
}