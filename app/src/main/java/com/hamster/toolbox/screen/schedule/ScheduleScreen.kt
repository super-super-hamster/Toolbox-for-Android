package com.hamster.toolbox.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.getSchedule
import com.hamster.toolbox.utils.squircleShape

@Composable
fun ScheduleScreen() {
    val totalWeeks = 20
    val pagerState = rememberPagerState(pageCount = { totalWeeks })

    val courseList = getSchedule(LocalContext.current)
    val weekCourses = Array(25) { Array(10) { Array<Course?>(5) { null } } }

    for (course in courseList) {
        for (week in course.activeWeeks) {
            weekCourses[week][course.dayOfWeek][course.startTime - 1] = course
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_dialog))
            .padding(top = 96.dp, bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp), // 让左右边距
            pageSpacing = 16.dp // 卡片之间的间距
        ) { page ->
            WeekScheduleCard(weekNumber = page + 1, weekCourses[page + 1])
        }
    }
}

@Composable
fun WeekScheduleCard(
    weekNumber: Int,
    courses: Array<Array<Course?>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                Spacer(modifier = Modifier.width(30.dp))

                val days = listOf("一", "二", "三", "四", "五", "六", "日")
                days.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                for (i in 1..5) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
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
                            val currentCourse = courses[day][i - 1]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .background(
                                        color = if (currentCourse != null) colorResource(R.color.mikuGreen) else Color.Gray,
                                        shape = squircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentCourse != null) {
                                    Text(
                                        text ="${currentCourse.name}\n${currentCourse.location}\n${currentCourse.teacher}",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}