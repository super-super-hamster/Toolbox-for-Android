package com.hamster.toolbox.ai.tools

class GetRulerUsageTool : Tool {
    override val name = "get_ruler_usage"
    override val description = "获取分贝仪的使用方法。仅当用户对分贝仪功能有疑问时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.RULER

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "关于校准：尺子刻度可能并不准确，你可以点击屏幕中心的“校准”按钮，输入缩放倍数，与现实中的尺子进行校准。\n" +
                "关于使用：可以上下滑动屏幕以移动尺子相对位置，尺子的范围为0到1米，精度为1cm。"
    }
}