package com.hamster.toolbox.ai.tools

import android.content.Context
import android.util.Log

class SetAlarmTool(private val context: Context) : Tool {
    override val name = "set_alarm"
    override val description = "设置一个闹钟。当用户需要设定起床、提醒等闹钟时调用此工具。"
    override val scope = ToolScope.GENERAL

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "time" to mapOf(
                "type" to "string",
                "description" to "闹钟的时间，格式必须是 HH:mm，例如 08:30"
            ),
            "label" to mapOf(
                "type" to "string",
                "description" to "闹钟的标签或备注，例如 '起床'、'开会'"
            )
        ),
        // 强制大模型必须返回这两个字段
        "required" to listOf("time", "label")
    )

    override suspend fun execute(arguments: String): String {
        return try {
            Log.d("fuck", "set alarm")

            "闹钟设定成功！"
        } catch (e: Exception) {
            "闹钟设定失败: ${e.message}"
        }
    }
}

class GetWeather() {

}