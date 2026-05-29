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
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.copyCurriculumJSONPrompt
import com.hamster.toolbox.utils.validateAndSaveJson
import kotlinx.coroutines.launch
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.ScheduleTips
import com.hamster.toolbox.compose.VerticalScrollPageColumn
import com.hamster.toolbox.compose.rememberStringPreference
import com.hamster.toolbox.compose.scrollTargetId
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.repository.SettingsRepository
import com.hamster.toolbox.repository.settingsStore
import com.hamster.toolbox.screen.schedule.Course

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImportCurriculumScreen(
    mainViewModel: MainViewModel,
    onShowLoading: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit,
    onNavigateToSettings: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context.settingsStore) }
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val sharedTiltState = rememberSharedTiltState()
    var naturalLanguage by remember { mutableStateOf("") }

    val scheduleJson by rememberStringPreference("schedule_json", "")

    VerticalScrollPageColumn(
        sharedTiltState = sharedTiltState,
        scrollTrigger = mainViewModel.settingsScrollTrigger,
        scrollTarget = mainViewModel.settingsScrollTarget
    ) {
        ItemGroup(titleState = sharedTiltState) {
            EditTextItem(
                modifier = Modifier.scrollTargetId("import_from_natural_language"),
                title = "通过自然语言导入",
                dialogTitle = "输入自然语言",
                initialValue = naturalLanguage,
                hint = "请输入课程信息",
                onConfirm = { input ->
                    scope.launch {
                        try {
                            onShowLoading(true)
                            val apiKey = settingsRepository.getAiApiKey()
                            if (apiKey.isNotEmpty()) {
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
                modifier = Modifier.scrollTargetId("import_from_json"),
                title = "通过JSON导入",
                summary = "将JSON文本导入为课程表",
                dialogTitle = "输入JSON",
                initialValue = scheduleJson,
                hint = "请输入符合格式要求的JSON文本",
                onConfirm = { input ->
                    if (input.isNotBlank()) {
                        try {
                            val courseListType = object : TypeToken<List<Course>>() {}.type
                            val gson = Gson()
                            val courses: List<Course> = gson.fromJson(scheduleJson, courseListType)
                            val validJsonString = gson.toJson(courses)

                            prefs.edit { putString("schedule_json", validJsonString) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
            )
            ClickItem(title = "复制提示词", summary = "通过外部AI生成符合要求的JSON文本", modifier = Modifier.scrollTargetId("copy_prompt")) {
                copyCurriculumJSONPrompt(context)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "课程表 Tips", icon = R.drawable.ic_tips, modifier = Modifier.scrollTargetId("schedule_tips")) {
                onNavigate(ScheduleTips)
            }
        }
    }
}