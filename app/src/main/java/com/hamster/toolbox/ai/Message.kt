package com.hamster.toolbox.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Request(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = false
)

@Keep
data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") var content: String
)

@Keep
data class Response(
    @SerializedName("choices") val choices: List<Choice>
)

@Keep
data class Choice(
    @SerializedName("message") val message: Message
)
