package com.hamster.toolbox

import kotlinx.serialization.Serializable

sealed interface Route

@Serializable
object Debug : Route

// 设置导航
@Serializable
object SettingsGraph : Route

// 主设置
@Serializable
object Settings : Route

// 导入课程表
@Serializable
object ImportCurriculum : Route

// 设置热词
@Serializable
object SetKeywords : Route

// 天气
@Serializable
object WeatherSettings : Route

// 尺子
@Serializable
object Ruler : Route

// 随机数
@Serializable
object RandomNumber : Route

// 课程表
@Serializable
object Schedule : Route

// 课程表导航
@Serializable
object TipsGraph : Route

// Tips
@Serializable
object Tips : Route

// 天气Tips
@Serializable
object WeatherTips : Route

// 课程表Tips
@Serializable
object ScheduleTips : Route

// 助手Tips
@Serializable
object AssistantTips : Route

// 尺子Tips
@Serializable
object RulerTips : Route

// 随机数Tips
@Serializable
object RandomNumberTips : Route

// 取色器Tips
@Serializable
object ColorPickerTips : Route

// 时间Tips
@Serializable
object TimeTips : Route

// 游戏机
@Serializable
object GameConsole : Route

// 时间
@Serializable
object Time : Route

// 日记导航
@Serializable
object DiaryGraph : Route

// 日记预览
@Serializable
object DiaryPreview : Route

// 日记
@Serializable
object Diary : Route

// 分贝仪
@Serializable
object DecibelMeter : Route

// 取色器
@Serializable
object ColorPicker : Route