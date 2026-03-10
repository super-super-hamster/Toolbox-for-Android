package com.hamster.toolbox.ai

import android.content.Context
import com.google.gson.Gson
import com.hamster.toolbox.utils.prompt.PromptLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AI {
    private val apiService = AiService.Companion.service
    val chatHistory = mutableListOf<Message>()

    suspend fun sendToAssistant(message: String, apiKey: String?) : AiResponse? {
        chatHistory.add(Message("user", message))
        val request = Request(messages = chatHistory)

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChatCompletion("Bearer $apiKey", request)
                val responseJson = response.choices.firstOrNull()?.message?.content

                val aiResponse: AiResponse = Gson().fromJson(responseJson, AiResponse::class.java)
                if (aiResponse.type == "chat" || aiResponse.type == "qa") {
                    chatHistory.add(Message("assistant", aiResponse.content))
                }
                aiResponse
            } catch (e: Exception) {
                e.printStackTrace()
                null
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
}