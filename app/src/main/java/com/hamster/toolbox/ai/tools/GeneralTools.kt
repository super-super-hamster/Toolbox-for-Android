package com.hamster.toolbox.ai.tools

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hamster.toolbox.WeatherRepository
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.system.AlarmData
import com.hamster.toolbox.weatherStore

class SetAlarmTool(
    private val context: Context,
    private val onConfirm: suspend (String, String) -> Boolean
) : Tool {
    override val name = "set_alarm"
    override val description = "设置一个闹钟。当用户需要设定起床、提醒等闹钟时调用此工具。"
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

            val isConfirmed = onConfirm("设置闹钟", "是否要设置 的闹钟？")

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

class GetWeather(private val context: Context) : Tool {
    override val name = "get_weather"
    override val description = "获取天气信息"
    override val scope = ToolScope.GENERAL

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "location" to mapOf(
                "type" to "string",
                "description" to "查询天气的位置，若为空则默认查询本地天气。"
            ),
        ),
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