package com.hamster.toolbox.screen.time

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

data class TimeData(
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long
)

// 不与外部字符串匹配，可以混淆
data class AppUsageState(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val durationMillis: Long,
    val duration: String,       // 格式化后的时间
    val percentage: Float       // 总时长的百分比
)

data class DailyAppUsageState(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val durationMillis: Long,
    val duration: String,
    val startTime: Long, // 相对于00:00的时间戳
    val endTime: Long,
    val color: Color
)

@Dao
interface UsageStatsDao {

    // 插入新的明细
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(sessions: List<AppSessionEntity>)

    // 获取明细记录
    @Query("SELECT * FROM app_sessions WHERE startTime >= :sinceTime ORDER BY startTime DESC")
    fun getSessionsSince(sinceTime: Long): Flow<List<AppSessionEntity>>

    // 获取每日使用时长
    @Query("SELECT * FROM app_daily_stats WHERE dateStamp >= :sinceTime")
    fun getDailyUsageSince(sinceTime: Long): Flow<List<AppDailyEntity>>

    // 清理过期数据
    @Query("DELETE FROM app_sessions WHERE endTime < :thresholdTime")
    suspend fun deleteOldSessions(thresholdTime: Long)

    // 更新每日时长
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDailyStats(stats: List<AppDailyEntity>)

    // 查询某个应用过去的每日数据
    @Query("SELECT * FROM app_daily_stats WHERE packageName = :packageName AND dateStamp >= :sinceDate ORDER BY dateStamp ASC")
    fun getAppDailyStats(packageName: String, sinceDate: Long): Flow<List<AppDailyEntity>>
}

@Database(
    entities = [AppSessionEntity::class, AppDailyEntity::class],
    version = 2,
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
                    .fallbackToDestructiveMigration(true) // 数据库版本冲突时删除旧的数据库
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(
    tableName = "app_sessions",
    indices = [
        Index(value = ["packageName", "startTime"], unique = true) // 依据包名和开始时间去重
    ]
)
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
    val month: Int,             // 冗余字段，方便按月快速查询
    val totalDurationMillis: Long
)