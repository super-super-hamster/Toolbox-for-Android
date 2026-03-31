package com.hamster.toolbox.screen.time

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

class TimeViewModel(private val dao: UsageStatsDao) : ViewModel() {

    // 数据流
    val currentMonthStats: Flow<List<AppSessionEntity>> =
        dao.getSessionsSince(Instant.now().minus(Duration.ofDays(30)).toEpochMilli())

    // 更新数据
    fun syncUsageData(context: Context) {
        viewModelScope.launch {
            // 过去7天到现在的时间窗口
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val beginTime = calendar.timeInMillis

            // 插入详细时间数据
            val rawSessions = fetchTime(context, beginTime, endTime)
            if (rawSessions.isEmpty()) return@launch

            val sessionEntities = rawSessions.map {
                AppSessionEntity(
                    packageName = it.packageName,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    durationMillis = it.durationMillis
                )
            }
            dao.insertSessions(sessionEntities)

            // 插入按天保存的数据
            val dailyStatsList = aggregateSessionsToDaily(rawSessions)
            dao.insertOrUpdateDailyStats(dailyStatsList)

            // 清理30天前的详细时间数据
             calendar.timeInMillis = System.currentTimeMillis()
             calendar.add(Calendar.DAY_OF_YEAR, -30)
             dao.deleteOldSessions(calendar.timeInMillis)
        }
    }

    // 按天统计使用时间
    private fun aggregateSessionsToDaily(sessions: List<TimeData>): List<AppDailyEntity> {
        // 键值 = [包名,当天0点时间戳]
        val dailyMap = mutableMapOf<Pair<String, Long>, Long>()

        for (session in sessions) {
            val name = session.packageName
            val dayStartTime = getStartOfDay(session.startTime)
            val key = Pair(name, dayStartTime)

            dailyMap[key] = (dailyMap[key] ?: 0L) + session.durationMillis
        }

        return dailyMap.map { (key, totalDuration) ->
            val (pkg, dayStamp) = key
            AppDailyEntity(
                packageName = pkg,
                dateStamp = dayStamp,
                month = getMonth(dayStamp),
                totalDurationMillis = totalDuration
            )
        }
    }

    // 一天开始的时间戳
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getMonth(millis: Long): Int {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .monthValue
    }
}

fun provideTimeViewModelFactory(dao: UsageStatsDao) = viewModelFactory {
    initializer {
        TimeViewModel(dao)
    }
}