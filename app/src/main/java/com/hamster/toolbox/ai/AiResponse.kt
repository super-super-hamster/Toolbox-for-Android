package com.hamster.toolbox.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AiResponse(
    @SerializedName("type") val type: String,
    @SerializedName("content") val content: String
)
