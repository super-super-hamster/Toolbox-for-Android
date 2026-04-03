package com.hamster.toolbox.screen.time

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// 获取时间数据
suspend fun fetchTime(context: Context, beginTime: Long, endTime: Long): List<TimeData> = withContext(Dispatchers.IO) {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usageStatsManager.queryEvents(beginTime, endTime)

    val timeList = mutableListOf<TimeData>()
    val packageStartTimes = mutableMapOf<String, Long>()

    val event = UsageEvents.Event()

    // 遍历事件流
    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        val packageName = event.packageName
        val timestamp = event.timeStamp

        when (event.eventType) {
            // 应用进入前台
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                packageStartTimes[packageName] = timestamp
            }

            // 应用退到后台
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                val startTime = packageStartTimes[packageName]
                if (startTime != null) {
                    val duration = timestamp - startTime

                    // 过滤极短时间
                    if (duration > 1000L) {
                        timeList.add(
                            TimeData(
                                packageName = packageName,
                                startTime = startTime,
                                endTime = timestamp,
                                durationMillis = duration
                            )
                        )
                    }
                    packageStartTimes.remove(packageName)
                }
            }
        }
    }

    return@withContext timeList
}

@Suppress("DEPRECATION")
fun permissionCheck(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }

    return mode == AppOpsManager.MODE_ALLOWED
}

fun openSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

class AppUsageMapper(context: Context) {
    val packageManager: PackageManager = context.packageManager

    fun mapAndAggregate(entities: List<AppSessionEntity>): List<AppUsageState> {
        val aggregatedMap = entities.groupBy { it.packageName }
            .mapValues { (_, dayList) ->
                dayList.sumOf { it.durationMillis }
            }

        val grandTotalDuration = aggregatedMap.values.sum()

        val uiStates = aggregatedMap.mapNotNull { (packageName, totalDuration) ->
            try {
                // 如果这个包名没有桌面图标，由于 <queries> 限制，这里会直接抛出异常
                val info = packageManager.getApplicationInfo(packageName, 0)

                val appName = packageManager.getApplicationLabel(info).toString()
                val appIcon = packageManager.getApplicationIcon(info)

                AppUsageState(
                    packageName = packageName,
                    name = appName,
                    icon = appIcon,
                    durationMillis = totalDuration,
                    duration = formatMillis(totalDuration),
                    percentage = if (grandTotalDuration > 0) totalDuration.toFloat() / grandTotalDuration else 0f
                )

            } catch (_: PackageManager.NameNotFoundException) {
                // 没有图标的应用，如底层服务将返回空
                return@mapNotNull null
            }
        }

        return uiStates.sortedByDescending { it.durationMillis }
    }
}

// 毫秒转小时分钟
fun formatMillis(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)

    return when {
        hours > 0 -> "${hours}小时 ${minutes}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> "< 1分钟"
    }
}