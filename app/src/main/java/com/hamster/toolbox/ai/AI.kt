package com.hamster.toolbox.ai

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.gson.Gson
import com.hamster.toolbox.SettingsRepository
import com.hamster.toolbox.ai.tools.CreateNewDiaryTool
import com.hamster.toolbox.ai.tools.GetColorPickerUsageTool
import com.hamster.toolbox.ai.tools.GetDecibelMeterUsageTool
import com.hamster.toolbox.ai.tools.GetDiaryContentTool
import com.hamster.toolbox.ai.tools.GetDiaryUsageTool
import com.hamster.toolbox.ai.tools.GetMeasureDecibelTool
import com.hamster.toolbox.ai.tools.GetPickedColorTool
import com.hamster.toolbox.ai.tools.GetWeather
import com.hamster.toolbox.ai.tools.ProvideDiaryTitleSuggestionTool
import com.hamster.toolbox.ai.tools.SetAlarmTool
import com.hamster.toolbox.ai.tools.SetScopeTool
import com.hamster.toolbox.ai.tools.ToolRegistry
import com.hamster.toolbox.ai.tools.ToolScope
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.screen.decibelMeter.DecibelMeterViewModel
import com.hamster.toolbox.screen.diary.DiaryViewModel
import com.hamster.toolbox.settingsStore
import com.hamster.toolbox.utils.prompt.PromptLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.UUID
import kotlin.math.max

object AI {
    private val apiService = AiService.service
//    val chatHistory = mutableStateListOf<Message>()
    val toolRegistry = ToolRegistry()
    lateinit var settingsRepository: SettingsRepository

    suspend fun chatWithAssistant(message: String, apiKey: String?, mainViewModel: MainViewModel) {
        if (apiKey.isNullOrBlank()) {
            return
        }

        val bracketRegex = Regex("""\\\[(.*?)\\]""", RegexOption.DOT_MATCHES_ALL)

        mainViewModel.apiHistory.add(Message("user", message))
        mainViewModel.uiHistory.add(ChatUiModel.Text(
            role = "user",
            content = message.replace(bracketRegex) { matchResult ->
                val mathContent = matchResult.groupValues[1].trim()
                "```math\n$mathContent\n```"
            }
        ))

        withContext(Dispatchers.IO) {
            try {
                var isConversationFinished = false
                var stepCount = 0

                // 最大请求次数 5
                while (!isConversationFinished && stepCount < 5) {
                    ++stepCount
                    val request = Request(
                        messages = mainViewModel.apiHistory.toList(),
                        tools = toolRegistry.getActiveToolDefinitions().takeIf { it.isNotEmpty() },
                        model = settingsRepository.getAiModelName()
                    )

                    val response = apiService.getChatCompletion("Bearer $apiKey", request)
                    val choice = response.choices.firstOrNull() ?: break
                    val responseMessage = choice.message
                    val finishReason = choice.finishReason

                    if (responseMessage.content == null) {
                        responseMessage.content = ""
                    }

                    if (finishReason == "tool_calls" && !responseMessage.toolCalls.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            mainViewModel.apiHistory.add(responseMessage)
                        }

                        for (toolCall in responseMessage.toolCalls) {
                            val toolResult = toolRegistry.dispatchCall(
                                name = toolCall.function.name,
                                arguments = toolCall.function.arguments
                            )

                            withContext(Dispatchers.Main) {
                                mainViewModel.apiHistory.add(
                                    Message(
                                        role = "tool",
                                        content = toolResult,
                                        toolCallId = toolCall.id
                                    )
                                )
                            }
                        }

                    } else {
                        if (responseMessage.content != null) {
                            val paragraphs = responseMessage.content!!
                                .split(Regex("\\n\\s*\\n")) // 空行分段
                                .filter { it.isNotBlank() }
                            mainViewModel.apiHistory.add(responseMessage)

                            mainViewModel.viewModelScope.launch(Dispatchers.Main) {
                                paragraphs.forEachIndexed { index, paragraph ->
//                                    val bracketRegex = Regex("""\\\[(.*?)\\]""", RegexOption.DOT_MATCHES_ALL)
                                    val processedText = paragraph.replace(bracketRegex) { matchResult ->
                                        val mathContent = matchResult.groupValues[1].trim()
                                        "```math\n$mathContent\n```"
                                    }

                                    mainViewModel.uiHistory.add(
                                        ChatUiModel.Text(
                                            role = "assistant",
                                            content = processedText.trim()
                                        )
                                    )

                                    if (index < paragraphs.size - 1) {
                                        delay(max(400L, processedText.length * 20L))
                                    }
                                }
                            }
                        }
                        isConversationFinished = true
                    }
                }

                if (!isConversationFinished) {
                    mainViewModel.apiHistory.add(Message(role = "assistant", content = "我是笨蛋"))
                    mainViewModel.uiHistory.add(ChatUiModel.Text(role = "assistant", content = "我是笨蛋"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    "API请求错误 (HTTP ${e.code()}): $errorBody"
                } else {
                    "网络请求异常：${e.message}"
                }

                withContext(Dispatchers.Main) {
                    mainViewModel.apiHistory.add(Message(role = "assistant", content = "请求失败，请检查网络或配置：$errorMessage"))
                    mainViewModel.uiHistory.add(ChatUiModel.Text(role = "user", content = "请求失败，请检查网络或配置：$errorMessage"))
                }
            }
        }
    }

    suspend fun sendWithPrompt(context: Context, message: String, promptId: String, apiKey: String?) : AiResponse? {
        val messageList = mutableListOf<Message>()
        messageList.add(Message("system", PromptLoader.getPromptById(context, promptId)))
        messageList.add(Message("user", message))

        val request = Request(messages = messageList)

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChatCompletion("Bearer $apiKey", request)
                val responseJson = response.choices.firstOrNull()?.message?.content
                val aiResponse: AiResponse = Gson().fromJson(responseJson, AiResponse::class.java)
                aiResponse
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getBalance(apiKey: String): String {
        if (apiKey.isBlank()) return "无"

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBalance("Bearer $apiKey")

                if (response.balanceInfos.isEmpty()) {
                    return@withContext "无"
                }

                val balanceStr = response.balanceInfos.joinToString("，") { info ->
                    "${info.totalBalance} ${info.currency}"
                }

                balanceStr

            } catch (e: Exception) {
                e.printStackTrace()
                "无"
            }
        }
    }

    fun setScope(scope: ToolScope) {
        toolRegistry.setCurrentScope(scope)
    }

    fun init(context: Context,navController: NavController, mainViewModel: MainViewModel, decibelMeterViewModel: DecibelMeterViewModel, diaryViewModel: DiaryViewModel) {
        toolRegistry.registerAll(
            SetScopeTool(toolRegistry),
            SetAlarmTool(context) { title, message ->
                mainViewModel.requireUserConfirmation(title, message)
            },
            GetWeather(context),
            GetColorPickerUsageTool(),
            GetPickedColorTool(mainViewModel),
            GetMeasureDecibelTool(decibelMeterViewModel),
            GetDecibelMeterUsageTool(),
            CreateNewDiaryTool(mainViewModel, diaryViewModel),
            GetDiaryContentTool(navController, diaryViewModel) { title, message ->
                mainViewModel.requireUserConfirmation(title, message)
            },
            ProvideDiaryTitleSuggestionTool(diaryViewModel),
            GetDiaryUsageTool()
        )
        settingsRepository = SettingsRepository(context.settingsStore)

        mainViewModel.apiHistory.add(Message("system", PromptLoader.getPromptById(context, "system")))
    }

//    private suspend fun settingsScrollTo(target: String, mainViewModel: MainViewModel, onNavigate: (Route) -> Unit) {
//        mainViewModel.setSettingsScrollTarget(target)
//        withContext(Dispatchers.Main) {
//            onNavigate(SettingsGraph)
//        }
//    }
}

sealed class ChatUiModel {
    val id: String = UUID.randomUUID().toString()

    data class Text(
        val role: String,
        val content: String
    ) : ChatUiModel()

    data class ConfirmCard(
        val title: String,
        val message: String,
        // 挂起凭证,UI层调用 .complete(true/false) 就能唤醒后台大模型
        val deferred: CompletableDeferred<Boolean>,
        var userChoice: Boolean? = null
    ) : ChatUiModel()
}