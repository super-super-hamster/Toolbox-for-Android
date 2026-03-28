package com.hamster.toolbox.system

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.getSchedule
import com.hamster.toolbox.utils.timeToMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

class Receiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "tool_box_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_SHOW_NOTIFICATION = "ACTION_SHOW_NOTIFICATION"
        const val ACTION_BUTTON_CLICK = "ACTION_BUTTON_CLICK"
        const val ACTION_CLASS_ALARM_CHECK = "ACTION_CLASS_ALARM_CHECK"
        const val CLASS_ALARM = "CLASS_ALARM"

        fun scheduleNotification(context: Context, triggerTimeMillis: Long, action: String, requestCode: Int, enable: Boolean, extraData: Array<String>?) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, Receiver::class.java).apply {
                this.action = action
                if (extraData != null) {
                    putExtra("extra_data", extraData)
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (enable) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        10 * 60 * 1000,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun dailyNotification(context: Context, hour: Int, minute: Int, action: String, requestCode: Int, enable: Boolean, extraData: Array<String>?) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, Receiver::class.java).apply {
                this.action = action
                if (extraData != null) {
                    putExtra("extra_data", extraData)
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            val triggerTime = calendar.timeInMillis

            if (enable) {
                try {
                    scheduleNotification(context, triggerTime, action, requestCode, true, extraData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            ACTION_SHOW_NOTIFICATION -> {
                val extraData = intent.getStringArrayExtra("extra_data")
                extraData?.let {
                    if (it.size >= 4) {
                        showNotification(context, it[0], it[1], it[2], it.copyOfRange(3, it.size))
                    }
                }
            }
            ACTION_BUTTON_CLICK -> {
                val extraData = intent.getStringArrayExtra("extra_data")
                extraData?.let {
                    if (it.isNotEmpty()) {
                        when (it[0]) {
                            CLASS_ALARM -> {
                                if (it.size > 1) {
                                    val alarm = Alarm()
                                    alarm.setAlarm(context, it[1].toInt() - 1, 30, vibrate = true)
                                }
                            }
                        }
                    }
                }
                val notifId = intent.getIntExtra("notification_id", NOTIFICATION_ID)
                NotificationManagerCompat.from(context).cancel(notifId)
            }
            ACTION_CLASS_ALARM_CHECK -> {
                Log.d("debug", "check")

                // 第二天的课程检查
                dailyNotification(context, 22, 0, ACTION_CLASS_ALARM_CHECK, 10000, true, null)

                scheduleCheck(context)
            }
        }
    }

    fun showNotification(context: Context, title: String, text: String, buttonTitle: String, extraData: Array<String>?) {
        createNotificationChannel(context)

        val notificationId = System.currentTimeMillis().toInt()

        val buttonIntent = Intent(context, Receiver::class.java).apply {
            action = ACTION_BUTTON_CLICK
            putExtra("notification_id", notificationId)
            if (extraData != null) {
                putExtra("extra_data", extraData)
            }
        }
        val buttonPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, buttonTitle, buttonPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "提醒通知"
        val descriptionText = "用于任务提醒"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleCheck(context: Context) {
        val allCourses = getSchedule(context)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val startDay = prefs.getString("semester_start_date", null) ?: return

        val tomorrowDate = LocalDate.now().plusDays(1)
        val startDate = LocalDate.parse(startDay, DateTimeFormatter.ofPattern("yyyy-M-d"))

        val weekNumber = ChronoUnit.DAYS.between(startDate, tomorrowDate) / 7 + 1
        if (weekNumber <= 0) return

        val tomorrowDayOfWeek = tomorrowDate.dayOfWeek.value

        val coursesThisWeek = allCourses.filter { weekNumber.toInt() in it.activeWeeks }
        val coursesForTomorrow = coursesThisWeek
            .filter { it.dayOfWeek == tomorrowDayOfWeek }
            .sortedBy { it.startTime }

        val hasClass = BooleanArray(4) { false }
        val classSlot = ArrayList<Int>()

        val isClassRemindEnabled = prefs.getBoolean("class_notification", false)
        val isAlarmRemindEnabled = prefs.getBoolean("alarm_notification", false)

        for (course in coursesForTomorrow) {
            hasClass[course.startTime - 1] = true

            var tomorrowTime = LocalDateTime.now()
                .plusDays(1)
                .withMinute(30)
                .withSecond(0)
                .withNano(0)

            when (course.startTime) {
                1 -> {
                    tomorrowTime = tomorrowTime.withHour(7)
                    classSlot.add(1)
                }
                2 -> {
                    tomorrowTime = tomorrowTime.withHour(9)
                    if (!hasClass[0]) classSlot.add(2)
                }
                3 -> {
                    tomorrowTime = tomorrowTime.withHour(13)
                    classSlot.add(3)
                }
                4 -> {
                    tomorrowTime = tomorrowTime.withHour(15)
                    if (!hasClass[2]) classSlot.add(4)
                }
            }

            val triggerTime = timeToMillis(tomorrowTime)

            scheduleNotification(
                context,
                triggerTime,
                ACTION_SHOW_NOTIFICATION,
                10200 + course.startTime,
                isClassRemindEnabled,
                arrayOf("上课提醒", "下一节课是 " + course.name + "\n" + "在 " + course.location, "知道了", "")
            )
        }

        if (!isAlarmRemindEnabled) return

        for ((_, num) in classSlot.withIndex()) {
            val hour = when (num) {
                1 -> 8
                2 -> 10
                3 -> 14
                4 -> 16
                else -> -1
            }
            if (hour != -1) {
                showNotification(
                    context,
                    "闹钟设置",
                    "明天的$hour:00有课，是否设置闹钟？",
                    "确认",
                    arrayOf(CLASS_ALARM, hour.toString())
                )
            }
        }
    }
}