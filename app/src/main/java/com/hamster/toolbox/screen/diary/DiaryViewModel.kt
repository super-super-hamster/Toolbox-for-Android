package com.hamster.toolbox.screen.diary

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hamster.toolbox.utils.getStartOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId

fun Long.toYearMonth(): Pair<Int, Int> {
    val dateTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return Pair(dateTime.year, dateTime.monthValue)
}

class DiaryViewModel(private val appContext: Context, private val dao: DiaryDao) : ViewModel() {
    var selectedDiaryDate by mutableLongStateOf(-1)

    var isLocked by mutableStateOf(true)
    var previewDiaryTitle by mutableStateOf("")
    var showTitleSuggestionDialog by mutableStateOf(false)
    var titleSuggestion by mutableStateOf<List<String>>(listOf())

    val diaries: Flow<Map<Int, Map<Int, List<DiaryPreviewData>>>> =
        dao.getAllDiaryPreviews().map { list ->
            list.groupBy { it.date.toYearMonth().first }
                .toSortedMap(compareByDescending { it })
                .mapValues { yearEntry ->
                    yearEntry.value.groupBy { it.date.toYearMonth().second }
                        .toSortedMap(compareByDescending { it })
                        .mapValues { monthEntry ->
                            monthEntry.value.sortedByDescending { it.date }
                        }
                }
        }

    fun getDiary(date: Long = selectedDiaryDate): Flow<DiaryWithImages?> {
        return dao.getDiaryByDate(date)
    }

    fun getDiaryById(id: Long): DiaryWithImages? {
        return dao.getDiaryById(id)
    }

    fun saveDiary(diary: DiaryWithImages) {
        viewModelScope.launch {
            val diaryEntity = diary.diary

            val imagesData = diary.images.map { imageEntity ->
                Pair(imageEntity.localPath, imageEntity.position)
            }

            dao.saveDiary(diaryEntity, imagesData)
        }
    }

    fun createDiary(title: String?, date: Long, onNavigate: () -> Unit) {
        viewModelScope.launch {
            val targetDate = getStartOfDay(date)
            val existingRecord = dao.getDiaryEntityByDate(targetDate)

            if (existingRecord == null) {
                val diary = DiaryEntity(
                    title = title,
                    date = targetDate,
                    content = "",
                    wordCount = 0
                )
                dao.insertDiary(diary)
            }

            selectedDiaryDate = targetDate
            onNavigate()
        }
    }

    fun saveImageToLocal(uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri) ?: return@launch

                val fileName = "diary_img_${System.currentTimeMillis()}.jpg"
                val localFile = File(appContext.filesDir, fileName)

                val outputStream = FileOutputStream(localFile)
                inputStream.copyTo(outputStream)

                inputStream.close()
                outputStream.close()

                withContext(Dispatchers.Main) {
                    onSuccess(localFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDiary(diaryId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val images = diaryId?.let { getDiaryById(it) }?.images ?: emptyList()

            images.forEach { image ->
                val file = File(image.localPath)
                if (file.exists()) {
                    file.delete()
                }
            }

            if (diaryId != null) {
                dao.deleteDiaryById(diaryId)
            }
        }
    }
}

fun diaryViewModelFactory(context: Context, dao: DiaryDao) = viewModelFactory {
    initializer {
        DiaryViewModel(context, dao)
    }
}