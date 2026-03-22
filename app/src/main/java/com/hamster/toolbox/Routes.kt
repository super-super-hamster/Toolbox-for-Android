package com.hamster.toolbox

import kotlinx.serialization.Serializable

sealed interface Route

@Serializable
object SettingsGraph : Route

@Serializable
data class Settings(
    val trigger: Long = 0L,
    val jumpTarget: String? = null
) : Route

@Serializable
object ImportCurriculum : Route

@Serializable
object SetKeywords : Route

@Serializable
object WeatherSettings : Route

@Serializable
object Ruler : Route

@Serializable
object RandomNumber : Route

@Serializable
object Schedule : Route

@Serializable
object Tips : Route

@Serializable
object GameConsole : Route