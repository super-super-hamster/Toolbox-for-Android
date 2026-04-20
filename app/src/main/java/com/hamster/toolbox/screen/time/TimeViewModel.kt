package com.hamster.toolbox.screen.time

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import com.hamster.toolbox.utils.getStartOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class TimeViewModel(private val appContext: Context, private val dao: UsageStatsDao) : ViewModel() {
    private val mapper = AppUsageMapper(appContext)
    private val _invisibleApps = MutableStateFlow<Set<String>>(emptySet())
    val invisibleApps: StateFlow<Set<String>> = _invisibleApps.asStateFlow()
    val currentMonthStats: Flow<List<AppDailyEntity>> =
        dao.getDailyUsageSince(Instant.now().minus(Duration.ofDays(30)).toEpochMilli())
    val currentYearStats: Flow<List<AppDailyEntity>> =
        dao.getDailyUsageSince(
            LocalDate.now()
                .minusMonths(11)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli())

    val currentDayStats: Flow<List<AppSessionEntity>> =
        dao.getSessionsSince(getStartOfDay(System.currentTimeMillis()))

    init {
        loadInvisibleApps(appContext)
    }

    fun loadInvisibleApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val hasInit = prefs.getBoolean("has_init_invisible_apps", false)
            val currentSet = prefs.getStringSet("invisible_apps", emptySet())?.toMutableSet() ?: mutableSetOf()

            if (!hasInit) {
                val packageManager = context.packageManager

                // 适配 Android 13+，并直接告诉系统我们只想要系统应用，提高查询效率
                val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_SYSTEM_ONLY.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
                }

                // 二次校验，提取包名
                val initInvisibleSet = installedApps.filter { appInfo ->
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    isSystemApp || isUpdatedSystemApp
                }.map { it.packageName }.toSet()

                currentSet.addAll(initInvisibleSet)
                prefs.edit {
                    putStringSet(
                        "invisible_apps",
                        currentSet
                    ).putBoolean("has_init_invisible_apps", true)
                }
            }
            _invisibleApps.value = currentSet
        }
    }

    fun updateInvisibleApps(newSet: Set<String>) {
        _invisibleApps.value = newSet
        viewModelScope.launch(Dispatchers.IO) {
            PreferenceManager.getDefaultSharedPreferences(appContext).edit {
                putStringSet(
                    "invisible_apps",
                    newSet
                )
            }
        }
    }

    val monthUsageStateList: Flow<List<AppUsageState>> = currentMonthStats
        .map { mapper.mapAndAggregate(it) }
        .flowOn(Dispatchers.Default)

    val maxMonthUsageDuration: Flow<Long> = combine(monthUsageStateList, invisibleApps) { usageList, invisible ->
        usageList.filter { !invisible.contains(it.packageName) }
            .maxOfOrNull { it.durationMillis } ?: 0L
    }.flowOn(Dispatchers.Default)

    val yearUsageStateList: Flow<List<AppUsageState>> = currentYearStats
        .map { mapper.mapAndAggregate(it) }
        .flowOn(Dispatchers.Default)

    val maxYearUsageDuration: Flow<Long> = combine(yearUsageStateList, invisibleApps) { usageList, invisible ->
        usageList.filter { !invisible.contains(it.packageName) }
            .maxOfOrNull { it.durationMillis } ?: 0L
    }.flowOn(Dispatchers.Default)

    val dayUsageStateList: Flow<List<DailyAppUsageState>> = currentDayStats
        .map { mapper.dailyMapAndAggregate(it) }
        .flowOn(Dispatchers.Default)

    // 更新数据
    fun syncUsageData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
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
            dao.updateDailyStats(dailyStatsList)

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

    // 获取过去30天的使用时长
    fun getMonthUsageTime(packageName: String?): Flow<List<AppDailyEntity>> {
        if (packageName == null) {
            return flowOf(emptyList())
        }

        val calendar = Calendar.getInstance()
        // 获取 30 天前凌晨 0 点的时间戳
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return dao.getAppDailyStats(packageName, calendar.timeInMillis)
    }

    // 获取过去12个月的使用时长
    fun getYearUsageTime(packageName: String?): Flow<List<AppDailyEntity>> {
        if (packageName == null) {
            return flowOf(emptyList())
        }

        return dao.getAppDailyStats(packageName, LocalDate.now()
            .minusMonths(11)
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli())
    }

    private fun getMonth(millis: Long): Int {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .monthValue
    }
}

fun timeViewModelFactory(context: Context, dao: UsageStatsDao) = viewModelFactory {
    initializer {
        TimeViewModel(context.applicationContext, dao)
    }
}