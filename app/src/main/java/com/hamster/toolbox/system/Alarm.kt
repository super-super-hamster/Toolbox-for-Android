package com.hamster.toolbox.system

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import com.google.gson.Gson

class Alarm {

    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        days: ArrayList<Int>? = null,
        vibrate: Boolean = false
    ) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            //设置时间
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            //设置日期
            if (!days.isNullOrEmpty()) {
                putExtra(AlarmClock.EXTRA_DAYS, days)
            }
            //设置震动
            putExtra(AlarmClock.EXTRA_VIBRATE, vibrate)
            //设置不切换到系统闹钟
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
            Toast.makeText(context, "闹钟设置成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "闹钟设置失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAlarmFromJSON(context: Context, json: String?): Boolean{
        if (json.isNullOrBlank()) {
            return false
        }

        try {
            val gson = Gson()
            val data: AlarmData = gson.fromJson(json, AlarmData::class.java)

            setAlarm(context, data.hour, data.minute, data.days, data.vibrate)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }
}