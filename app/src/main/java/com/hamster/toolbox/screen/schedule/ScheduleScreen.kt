package com.hamster.toolbox.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.SharedTiltState
import com.hamster.toolbox.utils.compose.StandardDialog
import com.hamster.toolbox.utils.compose.applySharedTilt
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.compose.squircleShape
import com.hamster.toolbox.utils.getSchedule
import com.hamster.toolbox.utils.saveSchedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// TODO: 高光当天

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current

    val totalWeeks = 20

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val semesterStartDateStr = prefs.getString("semester_start_date", null)

    val initialWeekPage = remember(semesterStartDateStr) {
        calculateCurrentWeekIndex(semesterStartDateStr, totalWeeks)
    }

    val pagerState = rememberPagerState(
        initialPage = initialWeekPage,
        pageCount = { totalWeeks }
    )

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val courseList = remember(refreshTrigger) { getSchedule(context) }
    val weekCourses = remember(courseList) {
        val array = Array(25) { Array(10) { Array<Course?>(10) { null } } }
        for (course in courseList) {
            for (week in course.activeWeeks) {
                val dayArray = array.getOrNull(week)?.getOrNull(course.dayOfWeek)
                if (dayArray != null && (course.startTime - 1) in dayArray.indices) {
                    dayArray[course.startTime - 1] = course
                }
            }
        }
        array
    }

    val sharedTiltState = rememberSharedTiltState()

    for (course in courseList) {
        for (week in course.activeWeeks) {
            val dayArray = weekCourses.getOrNull(week)?.getOrNull(course.dayOfWeek)
            if (dayArray != null && (course.startTime - 1) in dayArray.indices) {
                dayArray[course.startTime - 1] = course
            }
        }
    }

    PageColumn(sharedTiltState = sharedTiltState) {
        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp), // 左右边距
            pageSpacing = 16.dp // 卡片之间的间距
        ) { page ->
            WeekScheduleCard(
                weekNumber = page + 1,
                weekCourses.getOrNull(page + 1),
                sharedTiltState = sharedTiltState,
                semesterStartDateStr = semesterStartDateStr,
                onCourseUpdated = { oldCourse, newCourse ->
                    val newList = if (newCourse == null) {
                        // 删除课程
                        if (oldCourse != null) courseList.filter { it != oldCourse } else courseList
                    } else if (oldCourse == null) {
                        // 添加课程
                        courseList + newCourse
                    } else {
                        // 更新课程
                        courseList.map { if (it == oldCourse) newCourse else it }
                    }

                    saveSchedule(context, newList)
                    refreshTrigger++
                }
            )
        }
    }
}

@Composable
fun WeekScheduleCard(
    weekNumber: Int,
    courses: Array<Array<Course?>>?,
    sharedTiltState: SharedTiltState,
    semesterStartDateStr: String?,
    onCourseUpdated: (Course?, Course?) -> Unit
) {
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    var activeEmptySlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var addingSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val startDateObj = remember(semesterStartDateStr) {
        try {
            if (semesterStartDateStr != null && semesterStartDateStr != "未设置") {
                LocalDate.parse(semesterStartDateStr)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .applySharedTilt(sharedTiltState),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = "第 $weekNumber 周",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(20.dp))

                val days = listOf("一", "二", "三", "四", "五", "六", "日")
                days.forEachIndexed { index, day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )

                        val dateText = if (startDateObj != null) {
                            val offsetDays = ((weekNumber - 1) * 7 + index).toLong()
                            val currentDate = startDateObj.plusDays(offsetDays)
                            "${currentDate.monthValue}/${currentDate.dayOfMonth}"
                        } else {
                            "--/--"
                        }

                        Text(
                            text = dateText,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                for (i in 1..5) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = i.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        for (day in 1..7) {
                            val currentCourse = courses?.getOrNull(day)?.getOrNull(i - 1)
                            val isSlotActive = activeEmptySlot == Pair(day, i)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .clip(squircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (currentCourse != null) {
                                                selectedCourse = currentCourse
                                                activeEmptySlot = null
                                            } else {
                                                if (isSlotActive) {
                                                    addingSlot = Pair(day, i)
                                                    activeEmptySlot = null
                                                } else {
                                                    activeEmptySlot = Pair(day, i)
                                                }
                                            }
                                        }
                                    )
                                    .background(
                                        color = if (currentCourse != null) generateColor(
                                            currentCourse.name
                                        ).copy(alpha = 0.75f)
                                        else if (isSlotActive) Color.Gray.copy(alpha = 0.15f)
                                        else Color.Transparent,
                                        shape = squircleShape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (currentCourse != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = currentCourse.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 1.4.em,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = currentCourse.location,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 1.4.em
                                        )
                                    }
                                } else if (isSlotActive) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = "添加课程",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCourse?.let { course ->
        CourseEditDialog(
            initialCourse = course,
            defaultWeek = weekNumber, defaultDay = course.dayOfWeek, defaultPeriod = course.startTime,
            onDismiss = { selectedCourse = null },
            onConfirm = { old, new -> onCourseUpdated(old, new); selectedCourse = null },
            onDelete = { old -> onCourseUpdated(old, null); selectedCourse = null }
        )
    }

    addingSlot?.let { (day, period) ->
        CourseEditDialog(
            initialCourse = null,
            defaultWeek = weekNumber, defaultDay = day, defaultPeriod = period,
            onDismiss = { addingSlot = null },
            onConfirm = { _, new -> onCourseUpdated(null, new); addingSlot = null },
            onDelete = { }
        )
    }
}

@Composable
fun CourseEditDialog(
    initialCourse: Course?,
    defaultWeek: Int,
    defaultDay: Int,
    defaultPeriod: Int,
    onDismiss: () -> Unit,
    onDelete: (oldCourse: Course) -> Unit,
    onConfirm: (oldCourse: Course?, newCourse: Course) -> Unit
) {
    val course = initialCourse ?: Course(
        name = "",
        location = "",
        teacher = "",
        activeWeeks = listOf(defaultWeek),
        dayOfWeek = defaultDay,
        startTime = defaultPeriod
    )

    var courseName by remember { mutableStateOf(course.name) }
    var location by remember { mutableStateOf(course.location) }
    var teacher by remember { mutableStateOf(course.teacher ?: "") }

    var activeWeeks by remember { mutableStateOf(course.activeWeeks.toSet()) }
    var selectedDay by remember { mutableIntStateOf(course.dayOfWeek) }
    var selectedPeriod by remember { mutableIntStateOf(course.startTime) }

    var expandedPanel by remember { mutableStateOf("NONE") }

    val daysList = listOf("一", "二", "三", "四", "五", "六", "日")

    val weeksTextList = formatData(activeWeeks.toList())
    val validDayIndex = (selectedDay - 1).coerceIn(0, 6)
    val dayTextList = listOf("周" + daysList[validDayIndex])
    val periodTextList = listOf(selectedPeriod.toString())

    var toDelete by remember { mutableStateOf(false) }

    StandardDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                BasicTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    textStyle = TextStyle(
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    decorationBox = { innerTextField ->
                        if (courseName.isEmpty()) {
                            Text(
                                text = "课程名称",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )

                if (initialCourse != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "删除课程",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopStart)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                toDelete = !toDelete
                            }
                    )
                }
            }

            if (!toDelete) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("教室：", fontSize = 18.sp)
                    BasicTextField(
                        value = location,
                        onValueChange = { location = it },
                        textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("教师：", fontSize = 18.sp)
                    BasicTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("时间：在第 ", fontSize = 18.sp)
                    MdCodeText(textList = weeksTextList)
                    Text(" 周", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "修改周次",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    expandedPanel = if (expandedPanel == "WEEKS") "NONE" else "WEEKS"
                                })
                    )
                }
                if (expandedPanel == "WEEKS") {
                    MultiSelectGrid(range = 1..20, selectedItems = activeWeeks, columns = 5) { clicked ->
                        activeWeeks = toggleSetItem(activeWeeks, clicked)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(52.dp))
                    Text("的 ", fontSize = 18.sp)
                    MdCodeText(textList = dayTextList)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "修改星期",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable (
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    expandedPanel = if (expandedPanel == "DAYS") "NONE" else "DAYS"
                                })
                    )
                }
                if (expandedPanel == "DAYS") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysList.forEachIndexed { index, day ->
                            val dayValue = index + 1
                            val isSelected = (selectedDay == dayValue)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(squircleShape)
                                    .background(
                                        if (isSelected) colorResource(R.color.mikuGreen) else Color.Gray.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                    .clickable { selectedDay = dayValue }, // 单选直接赋值
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day, fontSize = 14.sp,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(52.dp))
                    Text("的第 ", fontSize = 18.sp)
                    MdCodeText(textList = periodTextList)
                    Text(" 节", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "修改节次",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable (
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    expandedPanel = if (expandedPanel == "PERIODS") "NONE" else "PERIODS"
                                })
                    )
                }
                if (expandedPanel == "PERIODS") {
                    SingleSelectGrid(
                        range = 1..12,
                        selectedItem = selectedPeriod,
                        columns = 6,
                        modifier = Modifier.padding(start = 52.dp)
                    ) { clicked ->
                        selectedPeriod = clicked
                    }
                }
            } else {
                Text(text = "确定要删除吗？", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = {
                        if (toDelete) {
                            toDelete = false
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = squircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text(
                        text = if (toDelete) "取消" else "我知道了",
                        color = Color.Black
                    )
                }

                Button(
                    onClick = {
                        if (toDelete) {
                            initialCourse?.let { onDelete(it) }
                        } else {
                            val updatedCourse = course.copy(
                                name = courseName.ifEmpty { "未命名课程" },
                                location = location,
                                teacher = teacher,
                                activeWeeks = activeWeeks.toList().sorted(),
                                dayOfWeek = selectedDay,
                                startTime = selectedPeriod
                            )
                            onConfirm(initialCourse, updatedCourse)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = squircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.btn_confirm))
                ) {
                    Text(
                        text = if (toDelete) "确认" else (if (initialCourse == null) "添加课程" else "保存修改"),
                        color = colorResource(R.color.text)
                    )
                }
            }
        }
    }
}

@Composable
fun MdCodeText(textList: List<String>, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        textList.forEach { text ->
            Text(
                text = text,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
                    .background(color = Color.Gray.copy(alpha = 0.15f), shape = squircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MultiSelectGrid(
    range: IntRange, selectedItems: Set<Int>, columns: Int,
    modifier: Modifier = Modifier, onToggle: (Int) -> Unit
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 8.dp)) {
        val chunkedItems = range.chunked(columns)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedItems.contains(item)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(squircleShape)
                            .background(
                                if (isSelected) colorResource(R.color.mikuGreen) else Color.Gray.copy(
                                    alpha = 0.1f
                                )
                            )
                            .clickable { onToggle(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(), fontSize = 14.sp,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun SingleSelectGrid(
    range: IntRange, selectedItem: Int, columns: Int,
    modifier: Modifier = Modifier, onSelect: (Int) -> Unit
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 8.dp)) {
        val chunkedItems = range.chunked(columns)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = (selectedItem == item)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(squircleShape)
                            .background(
                                if (isSelected) colorResource(R.color.mikuGreen) else Color.Gray.copy(
                                    alpha = 0.1f
                                )
                            )
                            .clickable { onSelect(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(), fontSize = 14.sp,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

fun toggleSetItem(set: Set<Int>, value: Int): Set<Int> {
    return if (set.contains(value)) set - value else set + value
}

fun formatData(weeks: List<Int>): List<String> {
    if (weeks.isEmpty()) return listOf("请选择")
    val sorted = weeks.sorted()
    val result = mutableListOf<String>()
    var start = sorted[0]
    var end = sorted[0]
    for (i in 1 until sorted.size) {
        if (sorted[i] == end + 1) {
            end = sorted[i]
        } else {
            if (start == end) result.add("$start") else result.add("$start-$end")
            start = sorted[i]
            end = sorted[i]
        }
    }
    if (start == end) result.add("$start") else result.add("$start-$end")
    return result
}

fun generateColor(string: String): Color {
    val hash = string.hashCode()

    val r = (hash and 0xFF0000 shr 16)
    val g = (hash and 0x00FF00 shr 8)
    val b = (hash and 0x0000FF)

    return Color(
        red = (r % 128 + 127) / 255f,
        green = (g % 128 + 127) / 255f,
        blue = (b % 128 + 127) / 255f,
        alpha = 0.8f
    )
}

fun calculateCurrentWeekIndex(startDateStr: String?, totalWeeks: Int): Int {
    if (startDateStr == null || startDateStr == "") return 0

    return try {
        val startDate = LocalDate.parse(startDateStr)
        val currentDate = LocalDate.now()

        val daysBetween = ChronoUnit.DAYS.between(startDate, currentDate)

        if (daysBetween < 0) {
            0
        } else {
            val weekIndex = (daysBetween / 7).toInt()
            weekIndex.coerceIn(0, totalWeeks - 1)
        }
    } catch (_: Exception) {
        0
    }
}