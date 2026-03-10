package com.hamster.toolbox.system

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            ACTION_SHOW_NOTIFICATION -> {
                val extraData = intent.getStringArrayExtra("extra_data")
                extraData?.let {
                    // 修改点：必须是 >= 4，否则 4 个元素的数组进不来
                    if (it.size >= 4) {
                        // 将第3个索引及之后的元素作为按钮的 extraData 传下去
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
                                    val alarm = Alarm() // 确保你有这个类
                                    alarm.setAlarm(context, it[1].toInt(), 0, vibrate = true)
                                }
                            }
                        }
                    }
                }
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            }
            ACTION_CLASS_ALARM_CHECK -> {
                scheduleCheck(context)
            }
        }
    }

    private fun showNotification(context: Context, title: String, text: String, buttonTitle: String, extraData: Array<String>?) {
        createNotificationChannel(context)

        val buttonIntent = Intent(context, Receiver::class.java).apply {
            action = ACTION_BUTTON_CLICK
            if (extraData != null) {
                putExtra("extra_data", extraData)
            }
        }
        val buttonPendingIntent = PendingIntent.getBroadcast(
            context,
            // 修改点：使用系统时间作为动态请求码，防止多个通知按钮互相覆盖
            System.currentTimeMillis().toInt(),
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, buttonTitle, buttonPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

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
            // 设置闹钟，必须传入绝对时间的时间戳
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
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
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleCheck(context: Context) {
        val allCourses = getSchedule(context)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val startDay = prefs.getString("semester_start_date", null) ?: return

        val currentDate = LocalDate.now()
        val startDate = LocalDate.parse(startDay, DateTimeFormatter.ofPattern("yyyy-M-d"))

        val weekNumber = ChronoUnit.DAYS.between(startDate, currentDate) / 7 + 1
        if (weekNumber <= 0) return

        // 修改点：因为是在今晚 22:00 查，所以要查“明天”的星期几
        val tomorrowDate = currentDate.plusDays(1)
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

            // 修改点：使用 var 以允许重新赋值修改时间
            var tomorrowTime = LocalDateTime.now()
                .plusDays(1)
                .withMinute(30)
                .withSecond(0)
                .withNano(0)

            // 修改点：由于 LocalDateTime 是不可变的，必须重新赋值给 tomorrowTime
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

            // 修改点：AlarmManager 需要绝对的触发时间戳，而不是相减的差值
            val triggerTime = timeToMillis(tomorrowTime)

            scheduleNotification(
                context,
                triggerTime,
                ACTION_SHOW_NOTIFICATION,
                10200 + course.startTime, // 修改点：使用动态 requestCode 防止被下一节课覆盖
                isClassRemindEnabled,
                arrayOf("上课提醒", "下一节课是：" + course.name + "\n" + "上课地点为：" + course.location, "知道了", "")
            )
        }

        // 修改点：因为是在晚上 22:00 运行 scheduleCheck，此时已经到了发送闹钟提醒的时间。
        // 所以不用再定到明天 22:00，而是直接在几秒后发送通知（避免广播拥堵，稍微延迟几秒执行）
        val nowMillis = System.currentTimeMillis()
        for ((index, num) in classSlot.withIndex()) {
            val hour = when (num) {
                1 -> 8
                2 -> 10
                3 -> 14
                4 -> 16
                else -> -1
            }
            if (hour != -1) {
                scheduleNotification(
                    context,
                    nowMillis + (index + 1) * 2000L, // 每个通知错开 2 秒，防止系统合并或覆盖
                    ACTION_SHOW_NOTIFICATION,
                    10300 + num, // 修改点：动态 requestCode
                    isAlarmRemindEnabled,
                    arrayOf("闹钟设置", "明天的$hour:00有课，是否设置闹钟？", "确认", CLASS_ALARM, hour.toString())
                )
            }
        }
    }
}