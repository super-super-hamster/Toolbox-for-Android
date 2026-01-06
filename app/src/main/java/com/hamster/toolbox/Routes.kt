package com.hamster.toolbox

import kotlinx.serialization.Serializable

sealed interface Route

@Serializable
object Home : Route

@Serializable
object ImportCurriculum : Route

@Serializable
object SetKeywords : Route

@Serializable
data class Settings(
    val trigger: Long = 0L,
    val jumpTarget: String? = null
) : Route