package com.hamster.toolbox.ai.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor


private val canvasAgent = AIAgent(
    // 直接调用顶层函数初始化
    promptExecutor = simpleOpenAIExecutor("YOUR_API_KEY_HERE"),
    llmModel = OpenAIModels.Chat.GPT4o,

    systemPrompt = """
            你是一个内置在安卓绘图应用里的 AI 助手。
            你的任务是帮助用户查询和修改画板的颜色。
            语气要友好、简短。如果用户提出修改颜色的请求，必须调用工具来完成，然后再回复用户。
        """.trimIndent(),

    toolRegistry = ToolRegistry {
//        tools(ColorTools(this@MainViewModel))
    }
)