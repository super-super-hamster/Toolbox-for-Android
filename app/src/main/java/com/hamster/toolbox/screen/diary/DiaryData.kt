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
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "diary_table",
    indices = [Index(value = ["date"], unique = true)]
)
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val content: String,
    val date: Long,
    val wordCount: Int
)

@Entity(
    tableName = "diary_image_table",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE // 级联删除，日记删了，图片记录自动删
        )
    ]
)
data class DiaryImageEntity(
    @PrimaryKey(autoGenerate = true) val imageId: Long = 0,
    @ColumnInfo(index = true) val diaryId: Long, // 外键，指向日记ID
    val localPath: String, // 本地图片路径
    val position: Int
)

data class DiaryWithImages(
    @Embedded val diary: DiaryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "diaryId"
    )
    val images: List<DiaryImageEntity>
)

@Dao
interface DiaryDao {
    @Query("SELECT id, title, date, wordCount FROM diary_table ORDER BY date DESC")
    fun getAllDiaryPreviews(): Flow<List<DiaryPreviewData>>

    // 根据ID查询单条日记
    @Transaction
    @Query("SELECT * FROM diary_table WHERE id = :diaryId")
    suspend fun getDiaryById(diaryId: Long): DiaryWithImages?

    // 插入日记主表（返回生成的ID）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity): Long

    // 批量插入图片
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<DiaryImageEntity>)

    @Transaction
    @Query("SELECT * FROM diary_table WHERE date = :targetDate")
    suspend fun getDiaryByDate(targetDate: Long): DiaryWithImages?

    // 👇 2. 新增：明确的 Update 方法
    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    // 👇 3. 新增：清空某篇日记的所有旧图片（用于更新时重置图文布局）
    @Query("DELETE FROM diary_image_table WHERE diaryId = :diaryId")
    suspend fun deleteImagesByDiaryId(diaryId: Long)

    @Transaction
    suspend fun saveDiary(diary: DiaryEntity, imagesData: List<Pair<String, Int>>) {
        // 先去数据库查一下，这天是不是已经有日记了
        val existingRecord = getDiaryByDate(diary.date)
        val finalDiaryId: Long

        if (existingRecord != null) {
            // 💡 存在旧记录 -> 走更新逻辑
            finalDiaryId = existingRecord.diary.id
            // 必须把原来的 ID 赋给新对象，否则无法覆盖
            val diaryToUpdate = diary.copy(id = finalDiaryId)
            updateDiary(diaryToUpdate)

            // 因为图文混排可能增删了图片，最简单的做法是先清空这篇日记关联的旧图片表
            deleteImagesByDiaryId(finalDiaryId)
        } else {
            // 💡 不存在 -> 走全新的插入逻辑
            finalDiaryId = insertDiary(diary)
        }

        // 插入最新的图片位置信息
        if (imagesData.isNotEmpty()) {
            val newImages = imagesData.map { (path, position) ->
                DiaryImageEntity(
                    diaryId = finalDiaryId,
                    localPath = path,
                    position = position
                )
            }
            insertImages(newImages)
        }
    }

    @Delete
    suspend fun deleteDiary(diary: DiaryEntity)
}

@Database(
    entities = [DiaryEntity::class, DiaryImageEntity::class],
    version = 2,
    exportSchema = false
)
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