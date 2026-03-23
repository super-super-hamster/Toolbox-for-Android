package com.hamster.toolbox

import kotlinx.serialization.Serializable

sealed interface Route

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

// 游戏机
@Serializable
object GameConsole : Route