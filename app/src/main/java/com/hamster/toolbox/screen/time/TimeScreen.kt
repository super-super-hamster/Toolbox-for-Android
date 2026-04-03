package com.hamster.toolbox.screen.time

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hamster.toolbox.R
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.ExplanationItem
import com.hamster.toolbox.compose.HorizontalLine
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.LineChart
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.TabItem
import com.hamster.toolbox.compose.Tabs
import com.hamster.toolbox.compose.rememberBooleanPreference
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.squircleShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: 热力图
// TODO: 处理跨天数据
// TODO: 进入时卡顿

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    mainViewModel: MainViewModel,
    setLoading: (Boolean) -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var selectedIndex by remember { mutableIntStateOf(1) }
    var hasPermission by remember { mutableStateOf(true) }

    var showPackageName by rememberBooleanPreference("super_hamster_show_package_name", false)

    val invisibleAppsSet = prefs.getStringSet("invisible_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
    val hasInit = prefs.getBoolean("has_init_invisible_apps", false)
    if (!hasInit) {
        val initInvisibleSet = setOf(
            "com.android.settings",
            "com.google.android.deskclock",
            "com.android.BBKClock",
            "com.vivo.gallery",
            "com.vivo.weather",
            "com.android.camera",
            "com.android.bbkcalculator",
            "com.vivo.ai.copilot",
            "com.vivo.translator",
            "com.bbk.appstore",
            "com.vivo.space",
            "com.vivo.assistant",
            "com.bbk.calendar",
            "com.vivo.smartshot")
        prefs.edit { putStringSet("invisible_apps", invisibleAppsSet + initInvisibleSet) }
        prefs.edit { putBoolean("has_init_invisible_apps", true) }
    }

    LaunchedEffect(mainViewModel.updateAppSessionTrigger) {
        if (hasPermission) {
            setLoading(true)
            viewModel.syncUsageData(context) // 刷新数据
            setLoading(false)
        }
    }

    // 页面重新回到前台时执行
    LifecycleResumeEffect(Unit) {
        hasPermission = permissionCheck(context)

        // 页面离开前台或销毁时执行
        onPauseOrDispose { }
    }

    // collectAsStateWithLifecycle 当应用在后台时停止计算
    val rawStats by viewModel.currentMonthStats.collectAsStateWithLifecycle(initialValue = emptyList())

    // 实例化数据转换工厂
    val mapper = remember { AppUsageMapper(context) }

    // derivedStateOf 仅在最终计算结果不同时更新
    val usageStateList by remember(rawStats) {
        derivedStateOf {
            mapper.mapAndAggregate(rawStats)
        }
    }

    val tabsList = listOf(
        TabItem(title = "近12月") {
            YearView()
        },
        TabItem(title = "近30天") {
            MonthView(
                viewModel = viewModel,
                showPackageName = showPackageName,
                initialApps = invisibleAppsSet,
                usageStateList = usageStateList
            )
        },
        TabItem(title = "今天") {
            DayView()
        }
    )

    PageColumn(sharedTiltState = sharedTiltState) {
        if (hasPermission) {
            ItemGroup(titleState =  sharedTiltState, modifier = Modifier.weight(1f)) {
                Tabs(
                    tabHorizontalPadding = 8.dp,
                    tabVerticalPadding = 8.dp,
                    tabs = tabsList,
                    selectedIndex = selectedIndex,
                    setSelectedIndex = { selectedIndex = it }
                )
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
fun YearView() {

}

@Composable
fun MonthView(
    viewModel: TimeViewModel,
    showPackageName: Boolean = false,
    initialApps: Set<String>,
    usageStateList: List<AppUsageState>
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var selectedApp by remember { mutableStateOf<AppUsageState?>(null) }

    var monthSelectedApp by remember { mutableStateOf<AppDailyEntity?>(null) }

    // 更新UI
    var invisibleApps by remember { mutableStateOf(initialApps) }

    var isEditVisible by remember { mutableStateOf(false) }

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

    if (selectedApp != null || isEditVisible) {
        BackHandler {
            selectedApp = null
            isEditVisible = false
        }
    }

    if (selectedApp != null) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberDrawablePainter(drawable = selectedApp?.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(shape = squircleShape),
                )

                Spacer(modifier = Modifier.width(24.dp))

                if (showPackageName) selectedApp?.packageName else selectedApp?.name?.let { Text(text = it, fontSize = 32.sp, color = colorResource(R.color.text)) }
            }

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
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(usageStateList, key = { it.packageName }) { app ->
                    if (isEditVisible) {
                        UsageSwitchItem(
                            showPackageName = showPackageName,
                            appUsageState = app,
                            checked = !invisibleApps.contains(app.packageName)
                        ) { isChecked ->
                            val newSet = invisibleApps.toMutableSet()
                            if (isChecked) {
                                newSet.remove(app.packageName)
                            } else {
                                newSet.add(app.packageName)
                            }
                            invisibleApps = newSet
                            prefs.edit { putStringSet("invisible_apps", invisibleApps) }
                        }
                    } else {
                        if (!invisibleApps.contains(app.packageName)) {
                            UsageItem(appUsageState = app, showPackageName = showPackageName) {
                                selectedApp = app
                            }
                        }
                    }
                }
            }

            ClickItem(title = if (isEditVisible) "返回" else "编辑可见项", icon = R.drawable.ic_invisible) {
                isEditVisible = !isEditVisible
            }
        }
    }
}

@Composable
fun DayView() {

}

@Composable
fun UsageItem(
    showPackageName: Boolean = false,
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
                progress = { appUsageState.percentage },
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