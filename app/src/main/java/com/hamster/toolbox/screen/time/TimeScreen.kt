package com.hamster.toolbox.screen.time

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hamster.toolbox.R
import com.hamster.toolbox.compose.ExplanationItem
import com.hamster.toolbox.compose.Heatmap
import com.hamster.toolbox.compose.HorizontalLine
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.LineChart
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.StandardDialog
import com.hamster.toolbox.compose.TabItem
import com.hamster.toolbox.compose.Tabs
import com.hamster.toolbox.compose.rememberBooleanPreference
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.squircleShape
import com.hamster.toolbox.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt



@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    mainViewModel: MainViewModel,
    setLoading: (Boolean) -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()
    val context = LocalContext.current

    var selectedIndex by remember { mutableIntStateOf(2) }
    var hasPermission by remember { mutableStateOf(true) }

    var showPackageName by rememberBooleanPreference("super_hamster_show_package_name", false)

    val invisibleAppsSet by viewModel.invisibleApps.collectAsStateWithLifecycle()

    if (mainViewModel.isSetInvisibleApp) {
        BackHandler {
            mainViewModel.changeStateOfIsSetInvisibleApp()
        }
    }

    // 页面重新回到前台时执行
    LifecycleResumeEffect(Unit) {
        hasPermission = permissionCheck(context)

        // 页面离开前台或销毁时执行
        onPauseOrDispose { }
    }

    val monthUsageStateList by viewModel.monthUsageStateList.collectAsStateWithLifecycle(initialValue = emptyList())
    val maxMonthUsageDuration by viewModel.maxMonthUsageDuration.collectAsStateWithLifecycle(initialValue = 0L)

    val yearUsageStateList by viewModel.yearUsageStateList.collectAsStateWithLifecycle(initialValue = emptyList())
    val maxYearUsageDuration by viewModel.maxYearUsageDuration.collectAsStateWithLifecycle(initialValue = 0L)

    val dayUsageStateList by viewModel.dayUsageStateList.collectAsStateWithLifecycle(initialValue = emptyList())

    val tabsList = listOf(
        TabItem(title = "近12月") {
            YearView(
                viewModel = viewModel,
                showPackageName = showPackageName,
                invisibleApps = invisibleAppsSet,
                usageStateList = yearUsageStateList,
                maxDuration = maxYearUsageDuration
            )
        },
        TabItem(title = "近30天") {
            MonthView(
                viewModel = viewModel,
                showPackageName = showPackageName,
                invisibleApps = invisibleAppsSet,
                usageStateList = monthUsageStateList,
                maxDuration = maxMonthUsageDuration
            )
        },
        TabItem(title = "今天") {
            DayView(
                setLoading = setLoading,
                usageStateList = dayUsageStateList,
                invisibleApps = invisibleAppsSet
            )
        }
    )

    PageColumn(sharedTiltState = sharedTiltState) {
        if (hasPermission) {
            ItemGroup(titleState =  sharedTiltState, modifier = Modifier.weight(1f)) {
                if (mainViewModel.isSetInvisibleApp) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(yearUsageStateList, key = { it.packageName }) { app ->
                            UsageSwitchItem(
                                showPackageName = showPackageName,
                                appUsageState = app,
                                checked = !invisibleAppsSet.contains(app.packageName)
                            ) { isChecked ->
                                val newSet = invisibleAppsSet.toMutableSet()
                                if (isChecked) {
                                    newSet.remove(app.packageName)
                                } else {
                                    newSet.add(app.packageName)
                                }
                                viewModel.updateInvisibleApps(newSet)
                            }
                        }
                    }
                } else {
                    Tabs(
                        tabHorizontalPadding = 8.dp,
                        tabVerticalPadding = 8.dp,
                        tabs = tabsList,
                        selectedIndex = selectedIndex,
                        setSelectedIndex = { selectedIndex = it }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ItemGroup(titleState = sharedTiltState) {
                    ExplanationItem(title = "需要权限", content = "需要在设置中手动开启以获取权限", buttonContent = "前往设置") {
                        openSettings(context)
                    }
                }
            }
        }
    }
}

@Composable
fun YearView(
    maxDuration: Long,
    viewModel: TimeViewModel,
    showPackageName: Boolean = false,
    invisibleApps: Set<String>,
    usageStateList: List<AppUsageState>
) {
    var selectedApp by remember { mutableStateOf<AppUsageState?>(null) }

    val yearData by remember(selectedApp) {
        viewModel.getYearUsageTime(selectedApp?.packageName)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val (heatmapData, dayCount) = remember(yearData) {
        val result = mutableMapOf<Long, Int>()
        var cnt = 0

        for (data in yearData) {
            result[data.dateStamp] = if (data.totalDurationMillis <= 600000) 0 else if (data.totalDurationMillis <= 3600000) 1 else 2
            ++cnt
        }

        result to cnt
    }

    UsageRank(
        selectedApp = selectedApp,
        showPackageName = showPackageName,
        invisibleApps = invisibleApps,
        usageStateList = usageStateList,
        maxDuration = maxDuration,
        setSelectedApp = { selectedApp = it },
        selectedContent = {
            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberDrawablePainter(drawable = selectedApp?.icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(shape = squircleShape),
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    val displayText = if (showPackageName) selectedApp?.packageName else selectedApp?.name

                    displayText?.let { text ->
                        Text(
                            text = text,
                            fontSize = 32.sp,
                            lineHeight = 40.sp,
                            color = colorResource(R.color.text)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "在最近的12个月中共使用了  ",
                        fontSize = 16.sp,
                        color = colorResource(R.color.text),
                        modifier = Modifier.alignByBaseline() // 让文字按基线对齐
                    )
                    Text(
                        text = "$dayCount",
                        fontSize = 32.sp,
                        color = colorResource(R.color.text),
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = "  天",
                        fontSize = 16.sp,
                        color = colorResource(R.color.text),
                        modifier = Modifier.alignByBaseline()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.Gray,
                            shape = squircleShape
                        )
                        .padding(8.dp)
                ) {
                    Heatmap(
                        modifier = Modifier.fillMaxWidth().height(600.dp),
                        dataMap = heatmapData,
                        levelColors = listOf(
                            colorResource(R.color.mikuGreen).copy(alpha = 0.5f),
                            colorResource(R.color.mikuGreen).copy(alpha = 0.75f),
                            colorResource(R.color.mikuGreen)
                        )
                    )
                }
            }
        }
    )
}

@Composable
fun MonthView(
    viewModel: TimeViewModel,
    maxDuration: Long,
    showPackageName: Boolean = false,
    invisibleApps: Set<String>,
    usageStateList: List<AppUsageState>
) {
    var selectedApp by remember { mutableStateOf<AppUsageState?>(null) }

    var monthSelectedApp by remember { mutableStateOf<AppDailyEntity?>(null) }

    val monthData by remember(selectedApp) {
        viewModel.getMonthUsageTime(selectedApp?.packageName)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    val timeList = remember(monthData) {
        if (monthData.isEmpty()) return@remember emptyList()

        val maxTime = monthData.maxBy { it.totalDurationMillis }

        if (maxTime.totalDurationMillis <= 180000) return@remember emptyList()

        val time1 = maxTime.totalDurationMillis / 1000 / 3 * 1000
        val time2 = maxTime.totalDurationMillis / 1000 / 3 * 2000
        listOf(
            HorizontalLine(value = time1.toFloat(), label = formatMillis(time1)),
            HorizontalLine(value = time2.toFloat(), label = formatMillis(time2))
        )
    }

    UsageRank(
        selectedApp = selectedApp,
        showPackageName = showPackageName,
        invisibleApps = invisibleApps,
        usageStateList = usageStateList,
        setSelectedApp = { selectedApp = it },
        maxDuration = maxDuration,
        selectedContent = {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberDrawablePainter(drawable = selectedApp?.icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(shape = squircleShape),
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    val displayText = if (showPackageName) selectedApp?.packageName else selectedApp?.name

                    displayText?.let { text ->
                        Text(
                            text = text,
                            fontSize = 32.sp,
                            lineHeight = 40.sp, //设置行高，解决多行拥挤问题
                            color = colorResource(R.color.text)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                    .border(
                        width = 2.dp,
                        color = Color.Gray,
                        shape = squircleShape
                    )
                    .padding(8.dp)
                ) {
                    LineChart(
                        data = monthData,
                        modifier = Modifier.fillMaxWidth(),
                        yValueMapper = {
                            it.totalDurationMillis.toFloat()
                        },
                        xLabelMapper = { _, entity ->
                            dateFormat.format(Date(entity.dateStamp))
                        },
                        isScrollable = true,
                        horizontalLines = timeList,
                        onPointClick = { _, item ->
                            monthSelectedApp = item
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    shape = squircleShape,
                    colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.bg_on_dialog)),
                ) {
                    monthSelectedApp?.let { Text(
                        text = "在 ${dateFormat.format(Date(it.dateStamp))} 使用了 ${formatMillis(it.totalDurationMillis)}",
                        modifier = Modifier.padding(16.dp)
                    ) }
                }

            }
        }
    )
}

@Composable
fun DayView(
    invisibleApps: Set<String>,
    setLoading: (Boolean) -> Unit,
    usageStateList: List<DailyAppUsageState>,
    hourHeight: Dp = 240.dp
) {
    val positionedApps = remember(usageStateList) {
        calculateAppPositions(usageStateList, setLoading)
    }

    val scrollState = rememberScrollState()
    val totalHours = 24

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .width(50.dp)
                .height(hourHeight * totalHours)
        ) {
            for (i in 0 until totalHours) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "${i.toString().padStart(2, '0')}:00",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(hourHeight * totalHours)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(totalHours) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight)
                            .border(width = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    )
                }
            }

            TimelineLayout(
                positionedApps = positionedApps,
                hourHeight = hourHeight,
                modifier = Modifier.fillMaxSize(),
                invisibleApps = invisibleApps
            )
        }
    }
}

@Composable
fun TimelineLayout(
    invisibleApps: Set<String>,
    positionedApps: List<PositionedApp>,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    val dayMillis = 24 * 60 * 60 * 1000f // 一天的总毫秒数
    val hourHeightPx = with(LocalDensity.current) { hourHeight.toPx() }

    var selectedApp by remember { mutableStateOf<DailyAppUsageState?>(null) }

    Layout(
        content = {
            positionedApps.forEach { item ->
                if (!invisibleApps.contains(item.appUsage.packageName)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        color = item.appUsage.color,
                        shape = squircleShape,
                        contentColor = Color.White,
                        border = BorderStroke(1.dp, Color.LightGray),
                        onClick = {
                            selectedApp = item.appUsage
                        }
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.padding(4.dp)
                        ) {
                            if (maxHeight > 16.dp) {
                                Text(
                                    text = item.appUsage.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (item.appUsage.color.luminance() > 0.5f) Color.Black else Color.White // 根据背景颜色亮度使用不同的字体颜色
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val totalHeightPx = (24 * hourHeightPx).roundToInt()
        val totalWidthPx = constraints.maxWidth

        val place = measurables.mapIndexed { index, measurable ->
            val item = positionedApps[index]
            val app = item.appUsage

            val itemWidth = totalWidthPx / item.totalColumns
            val itemHeight = ((app.endTime - app.startTime) / dayMillis * totalHeightPx).roundToInt()

            measurable.measure(
                Constraints.fixed(width = itemWidth, height = maxOf(0, itemHeight))
            )
        }

        // 布局每个应用的绝对位置
        layout(totalWidthPx, totalHeightPx) {
            place.forEachIndexed { index, placeable ->
                val item = positionedApps[index]
                val app = item.appUsage

                val itemWidth = totalWidthPx / item.totalColumns

                val xPosition = item.columnIndex * itemWidth
                val yPosition = (app.startTime / dayMillis * totalHeightPx).roundToInt()

                placeable.placeRelative(x = xPosition, y = yPosition)
            }
        }
    }

    selectedApp?.let { app ->
        StandardDialog(onDismissRequest = { selectedApp = null }) {
            Row(
                modifier = Modifier.fillMaxWidth(0.85f).padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = app.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = app.duration,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorResource(R.color.text)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val startTimeStr = formatRelativeTime(app.startTime)
                    val endTimeStr = formatRelativeTime(app.endTime)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$startTimeStr  —  $endTimeStr",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop, // 使图片的短边适应容器的大小，超出部分裁切
                    modifier = Modifier
                        .size(72.dp)
                        .clip(shape = squircleShape)
                )
            }
        }
    }
}

@Composable
fun UsageRank(
    maxDuration: Long,
    selectedApp: AppUsageState?,
    showPackageName: Boolean = false,
    invisibleApps: Set<String>,
    usageStateList: List<AppUsageState>,
    selectedContent: @Composable () -> Unit,
    setSelectedApp: (AppUsageState?) -> Unit
) {
    if (selectedApp != null) {
        BackHandler {
            setSelectedApp(null)
        }
    }

    if (selectedApp != null) {
        selectedContent()
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(usageStateList, key = { it.packageName }) { app ->
                    if (!invisibleApps.contains(app.packageName)) {
                        UsageItem(appUsageState = app, maxDuration = maxDuration, showPackageName = showPackageName) {
                            setSelectedApp(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsageItem(
    showPackageName: Boolean = false,
    maxDuration: Long,
    appUsageState: AppUsageState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appUsageState.icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape = squircleShape),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (showPackageName) appUsageState.packageName else appUsageState.name, fontSize = 16.sp, color = colorResource(R.color.text))
                Text(text = appUsageState.duration, fontSize = 12.sp, color = colorResource(R.color.text))
            }
            LinearProgressIndicator(
                progress = { (if (maxDuration > 0) appUsageState.durationMillis.toFloat() / maxDuration.toFloat() else 0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(bottom = 4.dp),
                color = colorResource(R.color.mikuGreen),
                trackColor = Color.Gray.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = R.drawable.arrow_right_bold),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.icon)
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun UsageSwitchItem(
    showPackageName: Boolean = false,
    appUsageState: AppUsageState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.toggle_button_anim))

    // 控制动画进度
    val progress by animateFloatAsState(
        targetValue = if (checked) 0.5f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "LottieProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appUsageState.icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape = squircleShape),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = if (showPackageName) appUsageState.packageName else appUsageState.name,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = colorResource(id = R.color.text)
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(40.dp)
                .width(80.dp)
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

data class PositionedApp(
    val appUsage: DailyAppUsageState,
    var columnIndex: Int = 0,
    var totalColumns: Int = 1
)

fun calculateAppPositions(sortedUsageList: List<DailyAppUsageState>, setLoading: (Boolean) -> Unit): List<PositionedApp> {
    if (sortedUsageList.isEmpty()) return emptyList()

    setLoading(true)

    val mergedUsageList = mutableListOf<DailyAppUsageState>()
    val lastSeenAppMap = mutableMapOf<String, Int>()
    val oneMinuteMillis = 60 * 1000L

    for (app in sortedUsageList) {
        val lastIndex = lastSeenAppMap[app.packageName]
        var merged = false

        if (lastIndex != null) {
            val lastApp = mergedUsageList[lastIndex]
            val gap = app.startTime - lastApp.endTime

            if (gap <= oneMinuteMillis) {
                val mergedApp = lastApp.copy(
                    endTime = maxOf(lastApp.endTime, app.endTime),
                    durationMillis = lastApp.durationMillis + app.durationMillis,
                    duration = formatMillis(lastApp.durationMillis + app.durationMillis)
                )
                mergedUsageList[lastIndex] = mergedApp
                merged = true
            }
        }

        if (!merged) {
            mergedUsageList.add(app)
            lastSeenAppMap[app.packageName] = mergedUsageList.lastIndex
        }
    }

    val clusters = mutableListOf<MutableList<PositionedApp>>()
    var currentCluster = mutableListOf<PositionedApp>()
    var clusterEndTime = 0L

    for (app in mergedUsageList) {
        if (currentCluster.isEmpty()) {
            currentCluster.add(PositionedApp(app))
            clusterEndTime = app.endTime
        } else {
            if (app.startTime < clusterEndTime) {
                currentCluster.add(PositionedApp(app))
                clusterEndTime = maxOf(clusterEndTime, app.endTime)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(PositionedApp(app))
                clusterEndTime = app.endTime
            }
        }
    }

    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    for (cluster in clusters) {
        val columnsEndTimes = mutableListOf<Long>()

        for (positionedApp in cluster) {
            val app = positionedApp.appUsage
            var placed = false

            for (i in columnsEndTimes.indices) {
                if (app.startTime >= columnsEndTimes[i]) {
                    positionedApp.columnIndex = i
                    columnsEndTimes[i] = app.endTime
                    placed = true
                    break
                }
            }

            if (!placed) {
                positionedApp.columnIndex = columnsEndTimes.size
                columnsEndTimes.add(app.endTime)
            }
        }

        val totalCols = columnsEndTimes.size
        for (positionedApp in cluster) {
            positionedApp.totalColumns = totalCols
        }
    }

    setLoading(false)

    return clusters.flatten()
}