package com.hamster.toolbox.utils.prompt

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PromptItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String
)
