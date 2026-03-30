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
suspend fun fetchTime(
    context: Context,
    beginTime: Long,
    endTime: Long
): List<TimeData> = withContext(Dispatchers.IO) {

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
            android.os.Process.myUid(),
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

    fun mapAndAggregate(entities: List<AppDailyEntity>): List<AppUsageState> {
        val aggregatedMap = entities.groupBy { it.packageName }
            .mapValues { (_, dayList) ->
                dayList.sumOf { it.totalDurationMillis }
            }

        val grandTotalDuration = aggregatedMap.values.sum()

        // 使用 mapNotNull 自动过滤无效应用
        val uiStates = aggregatedMap.mapNotNull { (packageName, totalDuration) ->
            try {
                // 如果这个包名没有桌面图标，由于 <queries> 限制，这里会直接抛出异常，跳到 catch 块
                val info = packageManager.getApplicationInfo(packageName, 0)

                // 可选：如果你连带桌面图标的预装系统应用（如系统日历、计算器）也想过滤掉，保留这段判断
                val isSystemApp = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                // 白名单：如果你想保留某些系统应用，可以在这里放行（例如浏览器、相册）
                val whiteList = listOf(
                    "com.android.chrome",
                    "com.google.android.apps.photos"
                )

                // 过滤不想统计的系统应用
                if (isSystemApp && packageName !in whiteList) {
                    return@mapNotNull null
                }

                // 获取应用名称和图标
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

            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                // 完美利用限制：看不到的应用（如底层服务）直接返回 null，不显示在列表中
                return@mapNotNull null
            }
        }

        return uiStates.sortedByDescending { it.durationMillis }
    }

    /**
     * 辅助函数：将毫秒转换成“X小时 Y分钟”
     */
    private fun formatMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)

        return when {
            hours > 0 -> "${hours}小时 ${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "< 1分钟"
        }
    }
}