package com.hamster.toolbox.screen.time

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hamster.toolbox.R
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.utils.compose.ExplanationItem
import com.hamster.toolbox.utils.compose.ItemGroup
import com.hamster.toolbox.utils.compose.PageColumn
import com.hamster.toolbox.utils.compose.SharedTiltState
import com.hamster.toolbox.utils.compose.TabItem
import com.hamster.toolbox.utils.compose.Tabs
import com.hamster.toolbox.utils.compose.rememberSharedTiltState

// TODO: 热力图

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    mainViewModel: MainViewModel,
    setLoading: (Boolean) -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()
    val context = LocalContext.current

    var selectedIndex by remember { mutableIntStateOf(1) }

    var hasPermission by remember { mutableStateOf(true) }

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


    val rawStats by viewModel.currentMonthStats.collectAsState(initial = emptyList())

    // 实例化数据转换工厂
    val mapper = remember { AppUsageMapper(context) }

    // ==========================================
    // 核心优化点：在非主线程（由 derivedStateOf 触发并在 remember 中）进行数据转换
    // 因为涉及聚合和 PM 查询，不能直接在 LazyColumn 的 item 里做
    // ==========================================
    val usageStateList by remember(rawStats) {
        derivedStateOf {
            mapper.mapAndAggregate(rawStats)
        }
    }

    val tabsList = listOf(
        TabItem(title = "年") {
            YearView()
        },
        TabItem(title = "月") {
            MonthView(
                usageStateList = usageStateList
            )
        },
        TabItem(title = "日") {
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
                    .fillMaxWidth().weight(1f),
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
    usageStateList: List<AppUsageState>
) {
        LazyColumn {
            items(usageStateList, key = { it.packageName}) {
                UsageItem(appUsageState = it) { }
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
            modifier = Modifier.size(48.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(text = appUsageState.name, fontSize = 16.sp, color = colorResource(id = R.color.text))
            LinearProgressIndicator(
                progress = { appUsageState.percentage },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = colorResource(R.color.mikuGreen),
                trackColor = Color.Gray,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.arrow_right_bold),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.icon)
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}