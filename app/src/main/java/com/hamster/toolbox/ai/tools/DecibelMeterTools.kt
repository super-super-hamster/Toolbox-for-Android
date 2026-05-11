package com.hamster.toolbox.ai.tools

import com.hamster.toolbox.screen.decibelMeter.DecibelMeterViewModel

class GetMeasureDecibelTool(
    private val decibelMeterViewModel: DecibelMeterViewModel
) : Tool {
    override val name = "get_measure_decibel_tool"
    override val description = "获取用户当前检测的分贝值"
    override val scope = ToolScope.DECIBEL_METER

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "当前测量数据：总计平均值为${decibelMeterViewModel.statsTotal.avg}, 总计最大值为${decibelMeterViewModel.statsTotal.max}, 总计最小值为${decibelMeterViewModel.statsTotal.min} \n" +
                "近10秒平均值为${decibelMeterViewModel.stats10s.avg}, 近10秒最大值为${decibelMeterViewModel.stats10s.max}, 近10秒最小值为${decibelMeterViewModel.stats10s.min}"
    }
}

class GetDecibelMeterUsageTool : Tool {
    override val name = "get_decibel_meter_usage"
    override val description = "获取分贝仪的使用方法，当用户对分贝仪功能有疑问时调用此工具。"
    override val scope = ToolScope.DECIBEL_METER

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "关于校准：由于不同设备差异，需要进行校准以接近真实值。\n" +
                "点击底部通用按钮即可开始校准。\n" +
                "在安静环境（如隔音良好的图书馆)下，背景音大约30至40分贝。\n" +
                "关于使用：点击开始即可开始测量。\n" +
                "关于测量结果：测量结果仅供参考。受硬件，校准等因素，测量结果不可避免与真实值有一定差距。"
    }
}