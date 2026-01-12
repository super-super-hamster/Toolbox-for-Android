package com.hamster.toolbox.screen.schedule

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

//Keep注解防止在代码混淆时导致类名变化
@Keep
data class Course(
    //SerializedName注解给变量起别名，防止代码混淆导致变量名变化
    @SerializedName("name") val name: String,           //课程名称
    @SerializedName("location") val location: String,       //上课地点
    @SerializedName("teacher") val teacher: String? = null,//授课老师

    @SerializedName("dayOfWeek") val dayOfWeek: Int,         //1-7分别表示周一至周日
    @SerializedName("startTime") val startTime: Int,     //开始节次
    @SerializedName("activeWeeks") val activeWeeks: List<Int>, //上课周次
)