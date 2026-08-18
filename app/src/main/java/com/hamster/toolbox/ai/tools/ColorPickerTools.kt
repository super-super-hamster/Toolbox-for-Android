package com.hamster.toolbox.ai.tools

import com.hamster.toolbox.main.MainViewModel

class GetPickedColorTool(
    private val mainViewModel: MainViewModel
) : Tool {
    override val name = "get_picked_color"
    override val description = "获取用户当前图片中提取的主要色调。仅当需要知道用户当前提取的颜色时才可调用，禁止除此以外的情况下调用。"
    override val scope = ToolScope.COLOR_PICKER

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "提取的主要色调降序排列依次是 ${mainViewModel.pickedColor}"
    }
}

class GetColorPickerUsageTool : Tool {
    override val name = "get_color_picker_usage"
    override val description = "获取取色器的使用方法。仅当用户对取色器功能有疑问时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.COLOR_PICKER

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "点击加号选择图片，将给出图片中的主要中等亮度和中等饱和度的颜色。"
    }
}