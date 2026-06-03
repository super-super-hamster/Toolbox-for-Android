package com.hamster.toolbox.screen.diary

import android.content.Context
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class SegmentType(val value: Int) {
    TEXT(0),
    IMAGE(1)
}

class DiaryConverters {
    @TypeConverter
    fun fromSegmentType(value: SegmentType): Int = value.value

    @TypeConverter
    fun toSegmentType(value: Int): SegmentType = enumValues<SegmentType>().first { it.value == value }
}

@Entity(
    tableName = "diary_table",
    indices = [Index(value = ["date"], unique = true)]
)
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val date: Long,
    val wordCount: Int
)

@Entity(
    tableName = "diary_segment_table",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE // 级联删除
        )
    ],
    indices = [Index(value = ["diaryId"])]
)
data class DiarySegmentEntity(
    @PrimaryKey(autoGenerate = true) val segmentId: Long = 0,
    val diaryId: Long,
    val type: SegmentType,
    val content: String,
    val position: Int
)

data class DiaryWithSegments(
    @Embedded val diary: DiaryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "diaryId",
    )
    val segments: List<DiarySegmentEntity>
)

@Dao
interface DiaryDao {
    @Query("SELECT id, title, date, wordCount FROM diary_table ORDER BY date DESC")
    fun getAllDiaryPreviews(): Flow<List<DiaryPreviewData>>

    @Transaction
    @Query("SELECT * FROM diary_table WHERE id = :diaryId")
    fun getDiaryById(diaryId: Long): DiaryWithSegments?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<DiarySegmentEntity>)

    @Transaction
    @Query("SELECT * FROM diary_table WHERE date = :targetDate")
    fun getDiaryByDate(targetDate: Long): Flow<DiaryWithSegments?>

    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    @Query("DELETE FROM diary_segment_table WHERE diaryId = :diaryId")
    suspend fun deleteSegmentsByDiaryId(diaryId: Long)

    @Query("SELECT * FROM diary_table WHERE date = :targetDate")
    suspend fun getDiaryEntityByDate(targetDate: Long): DiaryEntity?

    @Transaction
    suspend fun saveDiary(diary: DiaryEntity, segments: List<DiarySegmentEntity>) {
        val existingRecord = getDiaryEntityByDate(diary.date)
        val finalDiaryId: Long

        if (existingRecord != null) {
            finalDiaryId = existingRecord.id
            val diaryToUpdate = diary.copy(id = finalDiaryId)
            updateDiary(diaryToUpdate)
            deleteSegmentsByDiaryId(finalDiaryId)
        } else {
            finalDiaryId = insertDiary(diary)
        }

        if (segments.isNotEmpty()) {
            val newSegments = segments.map { it.copy(diaryId = finalDiaryId) }
            insertSegments(newSegments)
        }
    }

    @Query("DELETE FROM diary_table WHERE id = :id")
    suspend fun deleteDiaryById(id: Long)
}



@Database(
    entities = [DiaryEntity::class, DiarySegmentEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(DiaryConverters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "app_diary_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 预览
data class DiaryPreviewData(
    val id: Long,
    val title: String?,
    val date: Long,
    val wordCount: Int
)