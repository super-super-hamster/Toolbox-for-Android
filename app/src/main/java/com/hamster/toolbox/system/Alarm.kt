package com.hamster.toolbox.system

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class Alarm {
    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        days: ArrayList<Int>? = null,
        vibrate: Boolean = false
    ) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            // 设置时间
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            // 设置日期
            if (!days.isNullOrEmpty()) {
                putExtra(AlarmClock.EXTRA_DAYS, days)
            }
            putExtra(AlarmClock.EXTRA_VIBRATE, vibrate) // 设置震动
            putExtra(AlarmClock.EXTRA_SKIP_UI, true) // 不切换到系统闹钟
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}