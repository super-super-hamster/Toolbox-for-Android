package com.hamster.toolbox.ai

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.hamster.toolbox.RandomNumber
import com.hamster.toolbox.Route
import com.hamster.toolbox.Ruler
import com.hamster.toolbox.Schedule
import com.hamster.toolbox.SettingsGraph
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.utils.prompt.PromptLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: 没有意图识别，不好做流式输出
object AI {
    private val apiService = AiService.service
    val chatHistory = mutableStateListOf<Message>()

    suspend fun chatWithAssistant(context: Context, mainViewModel: MainViewModel, message: String, apiKey: String?, onNavigate: (Route) -> Unit) {
        chatHistory.add(Message("user", message))
        val request = Request(messages = chatHistory.toList())

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChatCompletion("Bearer $apiKey", request)
                val responseJson = response.choices.firstOrNull()?.message?.content

                val aiResponse: AiResponse = Gson().fromJson(responseJson, AiResponse::class.java)
                if (aiResponse.type == "chat" || aiResponse.type == "qa") {
                    chatHistory.add(Message("assistant", aiResponse.content))
                } else {
                    assistantQuery(context, mainViewModel, apiKey, aiResponse) { onNavigate(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendWithPrompt(context: Context, message: String, promptId: String, apiKey: String?) : AiResponse? {
        val messageList = mutableListOf<Message>()
        PromptLoader.getPromptById(context, promptId)?.let { messageList.add(Message("system", it)) }
        messageList.add(Message("user", message))

        val request = Request(messages = messageList)

        Log.d("fuck", "send")

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChatCompletion("Bearer $apiKey", request)
                val responseJson = response.choices.firstOrNull()?.message?.content
                Log.d("fuck", "response json")
                responseJson?.let { Log.d("fuck", it) }
                val aiResponse: AiResponse = Gson().fromJson(responseJson, AiResponse::class.java)
                aiResponse
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun assistantQuery(context: Context, mainViewModel: MainViewModel, apiKey: String?, response: AiResponse, onNavigate: (Route) -> Unit) {
        when(response.type) {
            "avatar", "nickname", "signature", "semester_start_date", "import_curriculum_options",
            "curriculum_notification", "class_notification", "alarm_notification", "assistant_avatar",
            "assistant_nickname", "api_key" -> {
                settingsScrollTo(response.type, mainViewModel) { onNavigate(it) }
            }
            "nav_curriculum" -> { withContext(Dispatchers.Main) { onNavigate(Schedule) } } // UI更新必须在主线程
            "nav_ruler" -> { withContext(Dispatchers.Main) { onNavigate(Ruler) } }
            "nav_settings" -> { withContext(Dispatchers.Main) { onNavigate(SettingsGraph) } }
            "nav_random" -> { withContext(Dispatchers.Main) { onNavigate(RandomNumber) } }
            "set_alarm" -> {
//                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//                val apiKey = prefs.getString("api_key", null)
                if (apiKey.isNullOrBlank()) {
//                    settingsScrollTo("api_key", mainViewModel) { onNavigate(it) }
                    return
                }
                val alarmResponse = sendWithPrompt(context, response.content, "set_alarm", apiKey)
                val alarm = Alarm()
                alarm.setAlarmFromJSON(context, alarmResponse?.content)
            }
        }

        chatHistory.add(Message("assistant", "好的"))
    }

    private suspend fun settingsScrollTo(target: String, mainViewModel: MainViewModel, onNavigate: (Route) -> Unit) {
        mainViewModel.setSettingsScrollTarget(target)
        withContext(Dispatchers.Main) {
            onNavigate(SettingsGraph)
        }
    }
}