package com.hamster.toolbox.ai.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamster.toolbox.ai.FunctionDefinition
import com.hamster.toolbox.ai.ToolDefinition

enum class ToolScope {
    GENERAL,        // 通用
    DIARY,          // 日记
    SETTINGS,       // 设置
    SCHEDULE,       // 课程表
    RULE,           // 尺子
    RANDOM,         // 随机数
    COLOR_PICKER,   // 取色器
    TIME,           // 时间
    DECIBEL_METER,  // 分贝仪
}

class SetScopeTool(private val registry: ToolRegistry) : Tool {
    override val name = "set_scope"
    override val scope = ToolScope.GENERAL

    override val description = """
        当你无法使用当前提供的工具完成用户需求时，请调用此工具获取其他领域的专用工具。
        支持的领域类型有：
        - "DIARY" (日记相关)
        - "SETTINGS" (设置相关)
        - "SCHEDULE" (课程表相关)
        - "RULE" (尺子相关)
        - "RANDOM" (随机数相关)
        - "COLOR_PICKER" (取色器相关)
        - "TIME" (应用使用时长相关)
        - "DECIBEL_METER" (分贝仪相关)
    """.trimIndent()

    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "domain" to mapOf(
                "type" to "string",
                "enum" to listOf("SETTINGS", "SCHEDULE", "DIARY"),
                "description" to "需要的特定工具领域"
            )
        ),
        "required" to listOf("domain")
    )

    override suspend fun execute(arguments: String): String {
        val scope: ToolScope = ToolScope.GENERAL
        when (arguments) {
            "DIARY" -> ToolScope.DIARY
            "SETTINGS" -> ToolScope.SETTINGS
            "SCHEDULE" -> ToolScope.SCHEDULE
            "RULE" -> ToolScope.RULE
            "RANDOM" -> ToolScope.RANDOM
            "COLOR_PICKER" -> ToolScope.COLOR_PICKER
            "TIME" -> ToolScope.TIME
            "DECIBEL_METER" -> ToolScope.DECIBEL_METER
        }

        registry.setCurrentScope(scope)

        return "成功加载 $arguments 领域的工具！"
    }
}

class ToolRegistry {
    private val allTools = mutableMapOf<String, Tool>()

    private var activeScopes by mutableStateOf(ToolScope.GENERAL)

    // 注册tool
    fun registerAll(vararg tools: Tool) {
        tools.forEach { allTools[it.name] = it }
    }

    // 设置作用域
    fun setCurrentScope(currentScope: ToolScope?) {
        if (currentScope != null) {
            activeScopes = currentScope
        }
    }

    // 获取tool
    fun getActiveToolDefinitions(): List<ToolDefinition> {
        return allTools.values
            .filter { it.scope == ToolScope.GENERAL || it.scope == activeScopes }
            .map { tool ->
                ToolDefinition(
                    function = FunctionDefinition(
                        name = tool.name,
                        description = tool.description,
                        parameters = tool.parameters
                    )
                )
            }
    }

    // 调用tool
    suspend fun dispatchCall(name: String, arguments: String): String {
        val tool = allTools[name] ?: return "调用失败：本地未注册名为 '$name' 的工具。"

        // 执行工具并捕获异常，将异常结果返还
        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            e.printStackTrace()
            "执行工具 '$name' 时发生异常: ${e.message}"
        }
    }
}

interface Tool {
    val name: String
    val description: String
    val parameters: Map<String, Any>
    val scope: ToolScope
    suspend fun execute(arguments: String): String
}