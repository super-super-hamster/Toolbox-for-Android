package com.hamster.toolbox.ai.tools

import com.google.gson.JsonParser
import com.hamster.toolbox.main.MainViewModel

class SetRandomNumberRangeTool(
    private val mainViewModel: MainViewModel
) : Tool {
    override val name = "set_random_number_range"
    override val description = "设置随机数范围，最小最大值的范围都应满足**0<=最小值<=最大值<=100**，当用户要求设置的范围不合理时，不应调用此工具，应直接告知用户范围不合理。仅当用户要求设置随机数范围，且范围合理时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.RANDOM

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "min" to mapOf(
                "type" to "int",
                "description" to "生成的随机数的范围的最小值。"
            ),
            "max" to mapOf(
                "type" to "int",
                "description" to "生成的随机数的范围的最大值。"
            )
        ),
        "required" to listOf("min", "max")
    )

    override suspend fun execute(arguments: String): String {
        val jsonObject = JsonParser.parseString(arguments).asJsonObject

        val min = jsonObject.get("min")?.asInt ?: 0
        val max = jsonObject.get("max")?.asInt ?: 0

        if (min in 0..100 && max in 0..100) {
            mainViewModel.randomNumberMax = max
            mainViewModel.randomNumberMin = min
            return "设置成功"
        } else {
            return "设置失败，随机数范围应在[0,100]之间。"
        }
    }
}

class GetRandomNumberRangeTool(
    private val mainViewModel: MainViewModel
) : Tool {
    override val name = "get_random_number_range"
    override val description = "获取生成的随机数的范围。仅当必须知道随机数的生成范围时调用此工具，，禁止除此以外的情况下调用。"
    override val scope = ToolScope.RANDOM

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "生成的随机数的范围是:[${mainViewModel.randomNumberMin},${mainViewModel.randomNumberMax}]"
    }
}

class GetGeneratedRandomNumberTool(
    private val mainViewModel: MainViewModel
) : Tool {
    override val name = "get_generated_random_number"
    override val description = "获取生成的随机数。仅当必须知道生成的随机数时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.RANDOM

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "生成的随机数是:${mainViewModel.generatedRandomNumber}"
    }
}

class GenerateRandomNumberTool(
    private val mainViewModel: MainViewModel
) : Tool {
    override val name = "generate_radom_number"
    override val description = "生成的随机数。仅当用户要求使用随机数功能生成随机数时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.RANDOM

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        mainViewModel.tryGenerateRandomNumber = true
        return "已尝试生成随机数"
    }
}

class GetRandomUsageTool : Tool {
    override val name = "get_random_usage"
    override val description = "获取随机数生成器的使用方法，仅当用户对随机数生成器功能有疑问时调用此工具。禁止除此以外的情况下调用。"
    override val scope = ToolScope.RANDOM

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "左右两边滚轮分别为生成的随机数范围的最小值和最大值，点击生成按钮后将生成范围内的随机数。"
    }
}