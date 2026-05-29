package com.hamster.toolbox.ai.tools

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.hamster.toolbox.repository.SettingsRepository
import com.hamster.toolbox.repository.repositorySetString
import com.hamster.toolbox.repository.settingsStore
import com.hamster.toolbox.screen.schedule.Course
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GetCurrentWeekScheduleTool(
    private val context: Context
) : Tool {
    override val name = "get_current_week_schedule"
    override val description = "获取当周的课程表。仅当必须必须知道当周的课程表时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val scheduleJson = prefs.getString("schedule_json", "")

        val startDay = prefs.getString("semester_start_date", null) ?: return "未设置开学日期，无法获取当周的课程表。"
        val week = try {
            val startDate = LocalDate.parse(startDay)
            val currentDate = LocalDate.now()

            val daysBetween = ChronoUnit.DAYS.between(startDate, currentDate)

            if (daysBetween < 0) {
                0
            } else {
                val weekIndex = (daysBetween / 7).toInt()
                weekIndex.coerceIn(0, 19)
            }
        } catch (_: Exception) {
            0
        }

        val gson = Gson()

        val listType = object : TypeToken<List<Course>>() {}.type
        val allCourses: List<Course> = gson.fromJson(scheduleJson, listType) ?: emptyList()

        val currentWeekCourses = allCourses.filter { course ->
            course.activeWeeks.contains(week)
        }

        return gson.toJson(currentWeekCourses)
    }
}

class GetAllScheduleTool(
    private val context: Context
) : Tool {
    override val name = "get_all_schedule"
    override val description = "获取完整的课程表。仅当必须必须知道完整的课程表时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val scheduleJson = prefs.getString("schedule_json", "")
        return scheduleJson!!
    }
}

class CreateNewCourseTool(
    private val context: Context
) : Tool {
    override val name = "create_new_course"
    override val description = "在课程表中添加一节新的课程。仅当用户要求添加课程时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "name" to mapOf(
                "type" to "string",
                "description" to "课程名称"
            ),
            "location" to mapOf(
                "type" to "string",
                "description" to "上课地点"
            ),
            "teacher" to mapOf(
                "type" to "string",
                "description" to "授课老师姓名"
            ),
            "day_of_week" to mapOf(
                "type" to "integer",
                "description" to "在周几上课，周一至周日分别对应1到7"
            ),
            "start_time" to mapOf(
                "type" to "integer",
                "description" to "上课的节次，上午第一节（当天第一节，第1、2小节）对应1，上午第二节（当天第二节，第3、4小节）对应2，下午第一节（当天第三节，第5、6小节）对应3，下午第二节（当天第4节，第7、8小节）对应4，晚上第一节（当天第五节，第9、10小节）对应5。数值必须为1到5，禁止出现负数、0和大于5的数。"
            ),
            "active_weeks" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "integer"
                ),
                "description" to "上课的周数，数值范围为1到20，禁止出现负数、零和大于20的数。"
            )
        ),
        "required" to listOf("active_weeks", "start_time", "day_of_week")
    )

    override suspend fun execute(arguments: String): String {
        val jsonObject = JsonParser.parseString(arguments).asJsonObject

        val name = jsonObject.get("name")?.asString ?: ""
        val location = jsonObject.get("location")?.asString ?: ""
        val teacher = jsonObject.get("teacher")?.asString
        val dayOfWeek = jsonObject.get("day_of_week")?.asInt ?: -1
        val startTime = jsonObject.get("start_time")?.asInt ?: -1
        val activeWeeksArray = jsonObject.getAsJsonArray("active_weeks")
        val activeWeeksList = activeWeeksArray?.map { it.asInt } ?: emptyList()

        if (dayOfWeek == -1 || startTime == -1 || activeWeeksList.isEmpty()) return "缺少传入参数，必须包含active_weeks，start_time和day_of_week"
        if (dayOfWeek !in 1..7 || startTime !in 1..5) return "传入参数数值范围异常，必须严格遵守参数数值范围。"

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val scheduleJson = prefs.getString("schedule_json", "")

        val gson = Gson()

        val listType = object : TypeToken<MutableList<Course>>() {}.type
        val courses: MutableList<Course> = gson.fromJson(scheduleJson, listType) ?: mutableListOf()

        val newCourse = Course(
            name = name,
            location = location,
            teacher = teacher,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            activeWeeks = activeWeeksList
        )

        courses.add(newCourse)
        prefs.edit { putString("schedule_json", gson.toJson(courses)) }
        return "添加成功"
    }
}

class DeleteCourseTool(
    private val context: Context,
    private val onConfirm: suspend (String, String) -> Boolean
) : Tool {
    override val name = "delete_course"
    override val description = "从课程表中所有周中名称、周几和节次完全相同的课程删除。仅当用户要求删除课程时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "name" to mapOf(
                "type" to "string",
                "description" to "课程名称"
            ),
            "day_of_week" to mapOf(
                "type" to "integer",
                "description" to "在周几上课，周一至周日分别对应1到7"
            ),
            "start_time" to mapOf(
                "type" to "integer",
                "description" to "课程的节次，上午第一节（当天第一节，第1、2小节）对应1，上午第二节（当天第二节，第3、4小节）对应2，下午第一节（当天第三节，第5、6小节）对应3，下午第二节（当天第4节，第7、8小节）对应4，晚上第一节（当天第五节，第9、10小节）对应5。数值必须为1到5，禁止出现负数、0和大于5的数。"
            ),
            "active_week" to mapOf(
                "type" to "integer",
                "description" to "课程的周数，数值范围为1到20，禁止出现负数、零和大于20的数。"
            )
        ),
        "required" to listOf("active_weeks", "start_time", "day_of_week", "name")
    )

    override suspend fun execute(arguments: String): String {
        val jsonObject = JsonParser.parseString(arguments).asJsonObject

        val name = jsonObject.get("name")?.asString ?: ""
        val dayOfWeek = jsonObject.get("day_of_week")?.asInt ?: -1
        val startTime = jsonObject.get("start_time")?.asInt ?: -1
        val activeWeek = jsonObject.get("active_week")?.asInt ?: -1

        if (dayOfWeek == -1 || startTime == -1 || activeWeek == -1) {
            return "缺少传入参数，必须包含active_week，start_time和day_of_week"
        }
        if (dayOfWeek !in 1..7 || startTime !in 1..5 || activeWeek !in 1..20) {
            return "传入参数数值范围异常，必须严格遵守参数数值范围。"
        }

        val isConfirmed = onConfirm("删除课程", "是否删除「$name」课程？")

        return if (isConfirmed) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val scheduleJson = prefs.getString("schedule_json", "") ?: ""

            val gson = Gson()
            val listType = object : TypeToken<MutableList<Course>>() {}.type
            val courses: MutableList<Course> = if (scheduleJson.isNotEmpty()) {
                gson.fromJson(scheduleJson, listType) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            val isRemoved = courses.removeAll { course ->
                course.name == name &&
                        course.dayOfWeek == dayOfWeek &&
                        course.startTime == startTime &&
                        course.activeWeeks.contains(activeWeek)
            }

            if (isRemoved) {
                prefs.edit { putString("schedule_json", gson.toJson(courses)) }
                "课程「$name」删除成功"
            } else {
                "未找到符合条件的课程，删除失败"
            }
        } else {
            "用户已取消删除操作"
        }
    }
}

class SetScheduleSemesterStartDate(
    private val context: Context
) : Tool {
    override val name = "set_schedule_semester_start_date"
    override val description = "修改开学日期。仅当用户要求修改开学日期时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "date" to mapOf(
                "type" to "string",
                "description" to "开学日期，格式为yyyy年MM月dd日。无论填入的日期为周几，最终实际的开学日期都将被设置为当周的周一。"
            )
        ),
        "required" to listOf("date")
    )

    override suspend fun execute(arguments: String): String {
        val jsonObject = JsonParser.parseString(arguments).asJsonObject
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        try {
            val timeStr = jsonObject.get("time")?.asString ?: ""

            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
            val date = LocalDate.parse(timeStr, formatter)

            val dateMills = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val instant = Instant.ofEpochMilli(dateMills)
            val selectedDateObj = instant.atZone(ZoneOffset.UTC).toLocalDate()
            val dayOfWeek = selectedDateObj.dayOfWeek.value
            val alignedDate = selectedDateObj.minusDays((dayOfWeek - 1).toLong())
            val newDateString = alignedDate.toString()
            repositorySetString(context.settingsStore, newDateString, SettingsRepository.SEMESTER_START_DATE)

            prefs.edit{ putString("semester_start_date", newDateString)}

            return "设置成功"
        } catch (e: Exception) {
            return "发生错误：${e.message}"
        }
    }
}

class GetScheduleUsageTool : Tool {
    override val name = "get_schedule_usage"
    override val description = "获取课程表的使用方法。仅当用户对课程表功能有疑问时调用此工具，禁止除此以外的情况下调用。"
    override val scope = ToolScope.SCHEDULE

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object"
    )

    override suspend fun execute(arguments: String): String {
        return "关于校准：尺子刻度可能并不准确，你可以点击屏幕中心的“校准”按钮，输入缩放倍数，与现实中的尺子进行校准。\n" +
                "关于使用：可以上下滑动屏幕以移动尺子相对位置，尺子的范围为0到1米，精度为1cm。"
    }
}