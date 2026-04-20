package com.hamster.toolbox.screen.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hamster.toolbox.utils.getStartOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

fun Long.toYearMonth(): Pair<Int, Int> {
    val dateTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return Pair(dateTime.year, dateTime.monthValue)
}

class DiaryViewModel(private val dao: DiaryDao) : ViewModel() {
    var selectedDiaryDate by mutableLongStateOf(-1)

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

    fun saveDiary(title: String?, date: Long, content: String, images: List<Pair<String, Int>>) {
        viewModelScope.launch {
            val diary = DiaryEntity(
                title = title?.ifBlank { null },
                date = getStartOfDay(date),
                content = content,
                wordCount = content.length
            )

            dao.saveDiary(diary, images)
        }
    }

    fun createDiary(title: String?, date: Long, onNavigate: () -> Unit) {
        viewModelScope.launch {
            val targetDate = getStartOfDay(date)
            val existingRecord = dao.getDiaryByDate(targetDate)

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
}

fun diaryViewModelFactory(dao: DiaryDao) = viewModelFactory {
    initializer {
        DiaryViewModel(dao)
    }
}