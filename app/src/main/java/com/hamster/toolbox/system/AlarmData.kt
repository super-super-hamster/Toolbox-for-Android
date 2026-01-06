package com.hamster.toolbox.system

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AlarmData(
    @SerializedName("hour") val hour: Int,
    @SerializedName("minute") val minute: Int,
    @SerializedName("days") val days: ArrayList<Int>? = null,
    @SerializedName("vibrate") val vibrate: Boolean = false,
)
