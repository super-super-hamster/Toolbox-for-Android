package com.hamster.toolbox.screen.time

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Keep
data class TimeData(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("endTime") val endTime: Long,
    @SerializedName("durationMillis") val durationMillis: Long
)

data class AppUsageState(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val durationMillis: Long,
    val duration: String,       // 格式化后的时间
    val percentage: Float       // 总时长的百分比
)

@Dao
interface UsageStatsDao {

    // 插入新的明细（忽略冲突，因为明细是不断追加的）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(sessions: List<AppSessionEntity>)

    // 获取近 N 天的明细记录
    @Query("SELECT * FROM app_sessions WHERE startTime >= :sinceTime ORDER BY startTime DESC")
    fun getSessionsSince(sinceTime: Long): Flow<List<AppSessionEntity>>

    // 清理过期数据
    @Query("DELETE FROM app_sessions WHERE endTime < :thresholdTime")
    suspend fun deleteOldSessions(thresholdTime: Long)

    // 插入或更新每日汇总。如果当天已有该 App 的记录，则替换更新其总时长
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStats(stats: List<AppDailyEntity>)

    // 获取某个月的所有应用汇总数据（供 Compose 监听）
    @Query("SELECT * FROM app_daily_stats WHERE yearMonth = :yearMonth ORDER BY totalDurationMillis DESC")
    fun getDailyStatsByMonth(yearMonth: String): Flow<List<AppDailyEntity>>

    // 查询某个特定应用过去 N 天的每日数据（用于绘制折线图/柱状图）
    @Query("SELECT * FROM app_daily_stats WHERE packageName = :pkgName AND dateStamp >= :sinceDate ORDER BY dateStamp ASC")
    fun getAppDailyTrend(pkgName: String, sinceDate: Long): Flow<List<AppDailyEntity>>
}

@Database(
    entities = [AppSessionEntity::class, AppDailyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {

    abstract fun usageStatsDao(): UsageStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppUsageDatabase? = null

        fun getDatabase(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    "app_usage_database"
                )
                    // 暂时允许在主线程进行少量查询（不推荐，最好全用协程，这里仅为方便开发初期调试）
                    // .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 自增主键
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long
)

@Entity(
    tableName = "app_daily_stats",
    primaryKeys = ["packageName", "dateStamp"]
)
data class AppDailyEntity(
    val packageName: String,
    val dateStamp: Long,        // 当天凌晨 00:00:00 的时间戳
    val yearMonth: String,      // 冗余字段，如 "2026-03"，方便按月快速查询
    val totalDurationMillis: Long
)