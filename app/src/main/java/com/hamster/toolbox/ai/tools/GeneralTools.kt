package com.hamster.toolbox.ai.tools

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hamster.toolbox.repository.WeatherRepository
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.system.AlarmData
import com.hamster.toolbox.repository.weatherStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SetAlarmTool(
    private val context: Context,
    private val onConfirm: suspend (String, String) -> Boolean
) : Tool {
    override val name = "set_alarm"
    override val description = "设置一个闹钟。当用户需要设定起床、提醒等闹钟时调用此工具。禁止在用户没有明确提出设置闹钟的需求时调用。"
    override val scope = ToolScope.GENERAL

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "hour" to mapOf(
                "type" to "integer",
                "description" to "小时，24小时制 (0-23)"
            ),
            "minute" to mapOf(
                "type" to "integer",
                "description" to "分钟 (0-59)"
            ),
            "days" to mapOf(
                "type" to "array",
                "items" to mapOf("type" to "integer"),
                "description" to "可选。重复的天数。常量：1=周日, 2=周一, 3=周二, 4=周三, 5=周四, 6=周五, 7=周六。如果是一次性闹钟，请不要传递此字段。"
            ),
            "vibrate" to mapOf(
                "type" to "boolean",
                "description" to "是否开启震动，默认 false"
            )
        ),
        // 必须返回这两个字段
        "required" to listOf("hour", "minute")
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val gson = Gson()
            val data: AlarmData = gson.fromJson(arguments, AlarmData::class.java)

            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", data.hour, data.minute)

            val repeatStr = if (data.days.isNullOrEmpty()) {
                "（单次闹钟）"
            } else {
                val weekMap = mapOf(
                    0 to "一", 1 to "二", 2 to "三", 3 to "四",
                    4 to "五", 5 to "六", 6 to "日"
                )
                val daysDesc = data.days
                    .sorted()
                    .mapNotNull { weekMap[it] }
                    .joinToString("、")

                if (data.days.size == 7) "（每天）" else "（每周$daysDesc）"
            }

            val vibrateStr = if (data.vibrate) "，并开启震动" else ""

            val message = "是否要设置 $timeStr 的闹钟$repeatStr$vibrateStr？"
            val isConfirmed = onConfirm("设置闹钟", message)

            return if (isConfirmed) {
                val alarm = Alarm()
                alarm.setAlarm(context, data.hour, data.minute, data.days, data.vibrate)
                "闹钟设定成功"
            } else {
                "用户拒绝了本次设定闹钟"
            }


        } catch (e: Exception) {
            "闹钟设定失败: ${e.message}"
        }
    }
}

class GetWeatherTool(private val context: Context) : Tool {
    override val name = "get_weather"
    override val description = "获取天气信息。"
    override val scope = ToolScope.GENERAL

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "location" to mapOf(
                "type" to "string",
                "description" to "查询天气的位置，若为空则默认查询本地天气。"
            )
        )
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val jsonObject = JsonParser.parseString(arguments).asJsonObject

            val location = jsonObject.get("location")?.asString ?: ""

            var result = ""

            if (location.isEmpty()) {
                val weatherRepository = WeatherRepository(context.weatherStore)
                val data = weatherRepository.getCachedData()
                val city = weatherRepository.getCachedCity()

                result = "当前$city 的天气状况是$data"
            }

            result
        } catch (e: Exception) {
            "获取天气失败 ${e.message}"
        }
    }
}

class GetBasicInformationTool : Tool {
    override val name = "get_basic_information"
    override val description = "用于获取基础信息，包括当前的时间。仅当必须知道基础信息时调用，禁止除此以外的情况下调用。"
    override val scope = ToolScope.GENERAL

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val current = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
            val formatted = current.format(formatter)

            return formatted
        } catch (e: Exception) {
            "获取基础信息失败 ${e.message}"
        }
    }
}

class GetToolboxUsageTool {

}