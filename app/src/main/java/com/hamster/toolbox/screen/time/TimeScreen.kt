package com.hamster.toolbox.screen.time

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hamster.toolbox.utils.compose.ClickItem
import com.hamster.toolbox.utils.compose.ExplanationItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.TabItem
import com.hamster.toolbox.utils.compose.Tabs
import com.hamster.toolbox.utils.compose.rememberSharedTiltState
import com.hamster.toolbox.utils.compose.squircleShape

// TODO: 热力图
// TODO: 处理跨天数据

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

    val invisibleAppsSet = prefs.getStringSet("invisible_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
    val hasInit = prefs.getBoolean("has_init_invisible_apps", false)
    if (!hasInit) {
        val initInvisibleSet = setOf("")
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
    initialApps: Set<String>,
    usageStateList: List<AppUsageState>
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // 更新UI
    var invisibleApps by remember { mutableStateOf(initialApps) }

    var isEditVisible by remember { mutableStateOf(false) }

    LazyColumn {
        items(usageStateList, key = { it.packageName}) { app ->
            if (isEditVisible) {
                UsageSwitchItem(
                    appUsageState = app,
                    checked = invisibleApps.contains(app.packageName)
                ) { isChecked ->
                    val newSet = invisibleApps.toMutableSet()
                    if (isChecked) {
                        newSet.add(app.packageName)
                    } else {
                        newSet.remove(app.packageName)
                    }
                    invisibleApps = newSet
                    prefs.edit { putStringSet("invisible_apps", invisibleApps) }
                }
            } else {
                if (invisibleApps.contains(app.packageName)) {
                    UsageItem(appUsageState = app) { }
                }
            }
        }

        item {
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
                Text(text = appUsageState.name, fontSize = 16.sp, color = colorResource(R.color.text))
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
            text = appUsageState.name,
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