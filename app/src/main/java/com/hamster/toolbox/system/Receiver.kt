package com.hamster.toolbox.system

import android.R
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

        //显示上课闹钟设置通知
        const val ACTION_SHOW_NOTIFICATION = "ACTION_SHOW_NOTIFICATION"
        //点击按钮
        const val ACTION_BUTTON_CLICK = "ACTION_BUTTON_CLICK"
        //上课闹钟通知检查
        const val ACTION_CLASS_ALARM_CHECK = "ACTION_CLASS_ALARM_CHECK"
        const val CLASS_ALARM = "CLASS_ALARM"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        //action类似一个标签
        val action = intent.action

        when (action) {
            ACTION_SHOW_NOTIFICATION -> {
                //发送通知
                val extraData = intent.getStringArrayExtra("extra_data")
                extraData?.let {
                    if (it.size>4) {
                        showNotification(context, it[0], it[1], it[2], arrayOf(it[3]))
                    }
                }
            }
            ACTION_BUTTON_CLICK -> {
                val extraData = intent.getStringArrayExtra("extra_data")
                extraData?.let {
                    when (it[0]) {
                        CLASS_ALARM -> {
                            val alarm = Alarm()
                            alarm.setAlarm(context, it[1].toInt(), 0, vibrate = true)
                        }
                    }
                }

                //移除弹出的消息
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            }
            ACTION_CLASS_ALARM_CHECK -> {
                curriculumCheck(context)
            }
        }
    }

    private fun showNotification(context: Context, title: String, text: String, buttonTitle: String, extraData: Array<String>?) {
        //创建渠道
        createNotificationChannel(context)

        // 修改action
        val buttonIntent = Intent(context, Receiver::class.java).apply {
            action = ACTION_BUTTON_CLICK
            // 可以在onReceive中intent.getStringExtra("extra_data")获取附带的数据
            if (extraData != null) {
                putExtra("extra_data", extraData)
            }
        }
        val buttonPendingIntent = PendingIntent.getBroadcast(
            context,
            200,
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info) // 必须设置小图标
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 设置高优先级，会有悬浮弹窗
            .setAutoCancel(true)
            // 添加按钮
            .addAction(R.drawable.ic_input_add, buttonTitle, buttonPendingIntent)

        // 发送通知
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    //标准实现 Android 8+ 必须创建渠道
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //渠道名称，用户在设置中看到的名称
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

    //定时提醒
    fun scheduleNotification(context: Context, triggerTimeMillis: Long, action: String, requestCode: Int, enable: Boolean, extraData: Array<String>?) {
        //获取系统闹钟服务
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, Receiver::class.java).apply {
            this.action = action
            if (extraData != null) {
                putExtra("extra_data", extraData)
            }
        }

        //待定意图
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode, //请求码，用于区分不同的PendingIntent
            intent,
            //FLAG_UPDATE_CURRENT：如果已有相同的请求码，那么更新而不是新建
            //FLAG_IMMUTABLE：Android 12+强制要求，表示这个Intent内容不可修改
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enable) {
            //设置提醒
            //setExactAndAllowWhileIdle: 即使手机处于低电量模式也能触发
            //不是准确时间
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

    //每日定时提醒
    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int, action: String, requestCode: Int, enable: Boolean, extraData: Array<String>?) {
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

        //计算下一次响铃时间
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

    //课程提醒检查
    @RequiresApi(Build.VERSION_CODES.O)
    private fun curriculumCheck(context: Context) {
//        val allCourses = getCurriculum(context)
//
//        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//        val startDay = prefs.getString("semester_start_date", null)
//        if (startDay == null) {
//            return
//        }
//
//        val currentDate = LocalDate.now()
//        val startDate = LocalDate.parse(
//            startDay,
//            DateTimeFormatter.ofPattern("yyyy-M-d")
//        )
//
//        val weekNumber = ChronoUnit.DAYS.between(startDate, currentDate) / 7 + 1
//        if (weekNumber <= 0) {
//            return
//        }
//
//        val currentDayOfWeek = currentDate.dayOfWeek.value
//        val coursesThisWeek = allCourses.filter { weekNumber.toInt() in it.activeWeeks }
//        val coursesForTomorrow = coursesThisWeek
//            .filter { it.dayOfWeek == currentDayOfWeek}
//            .sortedBy { it.startTimeSlot }
//
//        val hasClass = BooleanArray(4){ false }
//        val classSlot = ArrayList<Int>()
//        for(course in coursesForTomorrow) {
//            hasClass[course.startTimeSlot - 1] = true
//
//            val tomorrowTime = LocalDateTime.now()
//                .plusDays(1)
//                .withMinute(30)
//                .withSecond(0)
//                .withNano(0)
//            when(course.startTimeSlot) {
//                1 -> {
//                    tomorrowTime.withHour(7)
//                    classSlot.add(1)
//                }
//                2 -> {
//                    tomorrowTime.withHour(9)
//                    if (!hasClass[0]) {
//                        classSlot.add(2)
//                    }
//                }
//                3 -> {
//                    tomorrowTime.withHour(13)
//                    classSlot.add(3)
//                }
//                4 -> {
//                    tomorrowTime.withHour(15)
//                    if (!hasClass[2]) {
//                        classSlot.add(4)
//                    }
//                }
//            }
//
//            val isClassRemindEnabled = prefs.getBoolean("class_notification", false)
//            val triggerTime =timeToMillis(LocalDateTime.now()) - timeToMillis(tomorrowTime)
//            scheduleNotification(
//                context,
//                triggerTime,
//                ACTION_SHOW_NOTIFICATION,
//                102,
//                isClassRemindEnabled,
//                arrayOf("上课提醒", "下一节课是：" + course.name +"\n" + "上课地点为：" + course.location, "知道了", "")
//            )
//        }
//
//        val alarmNotificationTime = LocalDateTime.now()
//            .withHour(22)
//            .withMinute(0)
//            .withSecond(0)
//            .withNano(0)
//        val alarmNotificationMillis = timeToMillis(LocalDateTime.now()) - timeToMillis(alarmNotificationTime)
//        val isAlarmRemindEnabled = prefs.getBoolean("alarm_notification", false)
//        for (num in classSlot) {
//            val hour = when(num) {
//                1 -> 8
//                2 -> 10
//                3 -> 14
//                4 -> 16
//                else -> -1
//            }
//            scheduleNotification(
//                context,
//                alarmNotificationMillis,
//                ACTION_SHOW_NOTIFICATION,
//                103,
//                isAlarmRemindEnabled,
//                arrayOf("闹钟设置", "明天的$hour:00有课，是否设置闹钟？", "确认", CLASS_ALARM, hour.toString())
//            )
//        }
    }
}