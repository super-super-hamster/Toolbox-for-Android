package com.hamster.toolbox.screen.time

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimeViewModel(private val dao: UsageStatsDao) : ViewModel() {

    // 暴露给 Compose 订阅的数据流：获取当前月份的汇总数据
    val currentMonthStats: Flow<List<AppDailyEntity>> =
        dao.getDailyStatsByMonth(getCurrentYearMonth())

    /**
     * 触发数据同步（可以在进入页面或下拉刷新时调用）
     */
    fun syncUsageData(context: Context) {
        viewModelScope.launch {
            // 1. 定义拉取时间窗口：这里以拉取“过去 7 天”为例，防止遗漏
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val beginTime = calendar.timeInMillis

            // 2. 调用我们之前写好的底层抓取方法
            val rawSessions = fetchTime(context, beginTime, endTime)
            if (rawSessions.isEmpty()) return@launch

            // 3. 将单次会话明细存入数据库 (表 A)
            val sessionEntities = rawSessions.map {
                AppSessionEntity(
                    packageName = it.packageName,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    durationMillis = it.durationMillis
                )
            }
            dao.insertSessions(sessionEntities)

            // 4. 核心逻辑：将会话按“天”聚合，并存入每日汇总表 (表 B)
            val dailyStatsList = aggregateSessionsToDaily(rawSessions)
            dao.insertOrUpdateDailyStats(dailyStatsList)

            // 5. （可选）清理 30 天前的旧明细数据，防止数据库过大
            // calendar.timeInMillis = System.currentTimeMillis()
            // calendar.add(Calendar.DAY_OF_YEAR, -30)
            // dao.deleteOldSessions(calendar.timeInMillis)
        }
    }

    /**
     * 将零散的会话明细，按天和包名进行合并累加
     */
    private fun aggregateSessionsToDaily(sessions: List<TimeData>): List<AppDailyEntity> {
        // Key: Pair(包名, 当天凌晨 0 点的时间戳), Value: 累计时长
        val dailyMap = mutableMapOf<Pair<String, Long>, Long>()

        for (session in sessions) {
            val pkg = session.packageName
            val dayStamp = getStartOfDay(session.startTime)
            val key = Pair(pkg, dayStamp)

            val currentDuration = dailyMap[key] ?: 0L
            dailyMap[key] = currentDuration + session.durationMillis
        }

        // 将 Map 转换回 Entity 列表
        return dailyMap.map { (key, totalDuration) ->
            val (pkg, dayStamp) = key
            AppDailyEntity(
                packageName = pkg,
                dateStamp = dayStamp,
                yearMonth = getYearMonth(dayStamp), // 例如 "2026-03"
                totalDurationMillis = totalDuration
            )
        }
    }

    // ================== 时间处理辅助工具 ==================

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

    private fun getYearMonth(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getCurrentYearMonth(): String {
        return getYearMonth(System.currentTimeMillis())
    }
}

fun provideTimeViewModelFactory(dao: UsageStatsDao) = viewModelFactory {
    initializer {
        TimeViewModel(dao)
    }
}