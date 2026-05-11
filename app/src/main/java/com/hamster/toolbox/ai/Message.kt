package com.hamster.toolbox.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Request(
    @SerializedName("model") val model: String = "deepseek-v4-flash",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("tools") val tools: List<ToolDefinition>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null,
    @SerializedName("temperature") val temperature: Int = 1
)

@Keep
data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") var content: String? = null,
    @SerializedName("reasoning_content") var reasoningContent: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null

)

@Keep
data class ToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: FunctionCall
)

@Keep
data class FunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
)

// 工具定义
@Keep
data class ToolDefinition(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: FunctionDefinition
)

// 函数定义
@Keep
data class FunctionDefinition(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any>
)

@Keep
data class Response(
    @SerializedName("choices") val choices: List<Choice>
)

@Keep
data class Choice(
    @SerializedName("message") val message: Message,
    @SerializedName("finish_reason") val finishReason: String? = null
)
