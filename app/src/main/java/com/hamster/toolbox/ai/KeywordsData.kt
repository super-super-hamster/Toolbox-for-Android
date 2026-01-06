package com.hamster.toolbox.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class KeywordsData(
    @SerializedName("word") val word: String,
    @SerializedName("score") val score: Float
)