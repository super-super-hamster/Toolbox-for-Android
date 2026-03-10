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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.ItemGroup
import com.hamster.toolbox.utils.SharedTiltState
import com.hamster.toolbox.utils.StandardDialog
import com.hamster.toolbox.utils.applySharedTilt
import com.hamster.toolbox.utils.getSchedule
import com.hamster.toolbox.utils.rememberSharedTiltState
import com.hamster.toolbox.utils.squircleShape
import com.hamster.toolbox.utils.tiltGestureContainer

// TODO： 编辑课程信息
// TODO： 课程时间冲突检测
// TODO： 添加课程

@Composable
fun ScheduleScreen() {
    val totalWeeks = 20
    val pagerState = rememberPagerState(pageCount = { totalWeeks })

    val courseList = getSchedule(LocalContext.current)
    val weekCourses = Array(25) { Array(10) { Array<Course?>(10) { null } } }
    val sharedTiltState = rememberSharedTiltState()

    for (course in courseList) {
        for (week in course.activeWeeks) {
            val dayArray = weekCourses.getOrNull(week)?.getOrNull(course.dayOfWeek)
            if (dayArray != null && (course.startTime - 1) in dayArray.indices) {
                dayArray[course.startTime - 1] = course
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().tiltGestureContainer(sharedTiltState)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.background))
                .tiltGestureContainer(sharedTiltState)
                .padding(top = dimensionResource(R.dimen.top_padding), bottom = dimensionResource(R.dimen.bottom_padding)),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 24.dp), // 让左右边距
                pageSpacing = 16.dp // 卡片之间的间距
            ) { page ->
                WeekScheduleCard(weekNumber = page + 1, weekCourses.getOrNull(page + 1), sharedTiltState)
            }
        }
    }
}

@Composable
fun WeekScheduleCard(
    weekNumber: Int,
    courses: Array<Array<Course?>>?,
    sharedTiltState: SharedTiltState
) {
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .clip(squircleShape)
                                    .clickable(
                                        enabled = currentCourse != null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            selectedCourse = currentCourse
                                        }
                                    )
                                    .background(
                                        color = (if (currentCourse != null) generateColor(currentCourse.name).copy(alpha = 0.75f) else Color.Transparent),
                                        shape = squircleShape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (currentCourse != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = currentCourse.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = currentCourse.location,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis
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

    selectedCourse?.let { course ->
        StandardDialog(onDismissRequest = { selectedCourse = null }) {
            Column(
                modifier = Modifier.fillMaxWidth(0.85f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = course.name,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoRow(label = "上课地点", value = course.location)
                    InfoRow(label = "授课老师", value = course.teacher ?: "")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value.ifEmpty { "暂无信息" },
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
        )
    }
}

fun generateColor(string: String): Color {
    // 1. 获取字符串的哈希值
    val hash = string.hashCode()

    // 2. 提取 RGB 通道
    // 使用位运算让颜色分布更均匀
    val r = (hash and 0xFF0000 shr 16)
    val g = (hash and 0x00FF00 shr 8)
    val b = (hash and 0x0000FF)

    // 3. 亮度平衡（防止颜色太深或太浅）
    // 对于深色背景，我们希望颜色亮一点；对于白色背景，我们希望颜色偏马卡龙色
    return Color(
        red = (r % 128 + 127) / 255f,   // 限制在 127-255 范围内，保证颜色明亮
        green = (g % 128 + 127) / 255f,
        blue = (b % 128 + 127) / 255f,
        alpha = 0.8f // 设置一点透明度更好看
    )
}