package com.hamster.toolbox.ai.tools

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.gson.JsonParser
import com.hamster.toolbox.Diary
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.screen.diary.DiaryViewModel
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CreateNewDiaryTool(
    private val mainViewModel: MainViewModel,
    private val diaryViewModel: DiaryViewModel
) : Tool {
    override val name = "create_new_diary"
    override val description = "创建新日记。"
    override val scope = ToolScope.DIARY

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "time" to mapOf(
                "type" to "string",
                "description" to "可选。创建的日记日期，格式为yyyy年MM月dd日"
            ),
            "title" to mapOf(
                "type" to "string",
                "description" to "可选。"
            )
        )
    )

    override suspend fun execute(arguments: String): String {
        if (diaryViewModel.isLocked) {
            return "日记当前处于锁定状态，提醒用户前往日记页面解锁日记"
        } else {
            try {
                val jsonObject = JsonParser.parseString(arguments).asJsonObject

                val timeStr = jsonObject.get("time")?.asString ?: ""
                val title = jsonObject.get("title")?.asString ?: ""

                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                val date = LocalDate.parse(timeStr, formatter)

                diaryViewModel.createDiary(
                    title = title,
                    date = date.atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                ) {
                    mainViewModel.showAddDiaryDialog = true
                }
                return "创建成功"
            } catch (e: Exception) {
                return "发生错误： ${e.message}"
            }
        }
    }
}

class GetDiaryContentTool(
    private val navController: NavController,
    private val diaryViewModel: DiaryViewModel,
    private val onConfirm: suspend (String, String) -> Boolean
) : Tool {
    override val name = "get_diary_content"
    override val description = "仅在用户要求获取标题建议时调用此工具，其他时候禁止调用。"
    override val scope = ToolScope.DIARY

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        val currentHierarchy = navController.currentDestination?.hierarchy
        if (diaryViewModel.isLocked) {
            return "日记当前处于锁定状态，提醒用户前往日记页面解锁日记"
        } else if (currentHierarchy != null && !currentHierarchy.any { it.hasRoute<Diary>() }) {
            return "当前未选中任何日记,提醒用户在日记预览页面进入日记页面"
        } else {
            val isConfirmed = onConfirm("获取日记内容", "是否允许助手获取日记内容？\n请保证网络环境安全")

            return if (isConfirmed) {
                val diaryContent = diaryViewModel.getDiary().first()?.diary?.content
                diaryContent ?: ""
            } else {
                "用户拒绝了获取日记内容请求"
            }
        }
    }
}

class ProvideDiaryTitleSuggestionTool(
    private val diaryViewModel: DiaryViewModel
) : Tool {
    override val name = "provide_diary_title_suggestion"
    override val description = "给出日记标题建议，在获取用户日记内容后调取此工具，调用结束后如果用户没有其他需求即可结束本次对话。"
    override val scope = ToolScope.DIARY

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "title" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "string"
                ),
                "description" to "返回1到3个标题建议。"
            )
        ),
        "required" to listOf("title")
    )

    override suspend fun execute(arguments: String): String {
        if (diaryViewModel.isLocked) {
            return "日记当前处于锁定状态，提醒用户前往日记页面解锁日记"
        } else {
            val jsonObject = JsonParser.parseString(arguments).asJsonObject

            val titleJsonArray = jsonObject.getAsJsonArray("title")

            diaryViewModel.titleSuggestion = titleJsonArray.map { it.asString }
            diaryViewModel.showTitleSuggestionDialog = true

            return "已成功发送创建日记请求"
        }
    }
}

class GetDiaryUsageTool : Tool {
    override val name = "get_diary_usage"
    override val description = "获取日记的使用方法，仅当用户对日记功能有疑问时调用此工具。"
    override val scope = ToolScope.DIARY

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "关于隐私：可以在设置中打开“使用密码保护日记”，这将在通过设备密码验证后才可进入日记页面。\n" +
                "保证日记内容仅保存在本地，且未经用户允许不会以任何形式（如网络）传播到其他设备上，如果不放心可关闭网络等权限。\n" +
                "目前仅通过助手生成标题推荐时会将日记内容发送至deepseek官方服务器，如果需要使用请自行确保网络环境安全。\n" +
                "关于用法：点击底部通用按钮即可创建日记。在此基础上，可以选择不同的日期或者设置一个标题。"
    }
}