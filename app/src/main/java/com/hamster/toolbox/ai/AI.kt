package com.hamster.toolbox.ai

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.hamster.toolbox.Route
import com.hamster.toolbox.SettingsGraph
import com.hamster.toolbox.ai.tools.SetAlarmTool
import com.hamster.toolbox.ai.tools.SetScopeTool
import com.hamster.toolbox.ai.tools.ToolRegistry
import com.hamster.toolbox.ai.tools.ToolScope
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.utils.prompt.PromptLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

object AI {
    private val apiService = AiService.service
    val chatHistory = mutableStateListOf<Message>()
    val toolRegistry = ToolRegistry()

    suspend fun chatWithAssistant(message: String, apiKey: String?) {
        if (apiKey.isNullOrBlank()) {
            return
        }

        chatHistory.add(Message("user", message))

        withContext(Dispatchers.IO) {
            try {
                // 启动状态机循环
                var isConversationFinished = false
                var stepCount = 0

                while (!isConversationFinished && stepCount < 5) {
                    ++stepCount
                    // 2. 构建请求，携带历史记录和当前激活的作用域工具
                    val request = Request(
                        messages = chatHistory.toList(),
                        // 取出当前场景可以使用的工具列表
                        tools = toolRegistry.getActiveToolDefinitions().takeIf { it.isNotEmpty() }
                    )

                    // 3. 发送网络请求
                    val response = apiService.getChatCompletion("Bearer $apiKey", request)
                    val choice = response.choices.firstOrNull() ?: break
                    val responseMessage = choice.message
                    val finishReason = choice.finishReason

                    if (responseMessage.content == null) {
                        responseMessage.content = ""
                    }

                    // 4. 判断大模型想干什么
                    if (finishReason == "tool_calls" && !responseMessage.toolCalls.isNullOrEmpty()) {

                        // 【中断】大模型决定调用工具！

                        // a. 必须把大模型的这个思考过程也存入历史记录，不能丢弃
                        withContext(Dispatchers.Main) {
                            chatHistory.add(responseMessage)
                        }

                        // b. 遍历执行大模型要求的所有工具 (并发/顺序调用)
                        for (toolCall in responseMessage.toolCalls) {
                            // 核心：由注册中心分发执行具体的方法
                            val toolResult = toolRegistry.dispatchCall(
                                name = toolCall.function.name,
                                arguments = toolCall.function.arguments
                            )

                            // c. 把终端执行的结果作为 role="tool" 塞回去
                            withContext(Dispatchers.Main) {
                                chatHistory.add(
                                    Message(
                                        role = "tool",
                                        content = toolResult,
                                        toolCallId = toolCall.id // ID 必须对应上
                                    )
                                )
                            }
                        }
                        // 循环继续，大模型会看到 toolResult 并给出最终回答

                    } else {
                        // 【结束】大模型给出了普通文本回答 (finishReason == "stop")

                        if (responseMessage.content != null) {
                            withContext(Dispatchers.Main) {
                                chatHistory.add(responseMessage)
                            }
                        }
                        // 结束循环，跳出
                        isConversationFinished = true
                    }
                }

                if (!isConversationFinished) {
                    chatHistory.add(Message(role = "assistant", content = "我是笨蛋"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 发生异常时，优雅地告诉用户
                val errorMessage = if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    "API请求错误 (HTTP ${e.code()}): $errorBody"
                } else {
                    "网络请求异常：${e.message}"
                }

                Log.d("fuck", errorMessage)

                withContext(Dispatchers.Main) {
                    chatHistory.add(Message(role = "assistant", content = "请求失败，请检查网络或配置：${e.message}"))
                }
            }
        }
    }

    suspend fun sendWithPrompt(context: Context, message: String, promptId: String, apiKey: String?) : AiResponse? {
        val messageList = mutableListOf<Message>()
        PromptLoader.getPromptById(context, promptId)?.let { messageList.add(Message("system", it)) }
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

    fun setScope(scope: ToolScope) {
        toolRegistry.setCurrentScope(scope)
    }

    fun initTools(context: Context) {
        toolRegistry.registerAll(
            SetScopeTool(toolRegistry),
            SetAlarmTool(context),
        )
    }

    private suspend fun settingsScrollTo(target: String, mainViewModel: MainViewModel, onNavigate: (Route) -> Unit) {
        mainViewModel.setSettingsScrollTarget(target)
        withContext(Dispatchers.Main) {
            onNavigate(SettingsGraph)
        }
    }
}