package com.hamster.toolbox

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.AiResponse
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.ai.SpeechRecognizerManager
import com.hamster.toolbox.screen.gameConsole.GameConsoleScreen
import com.hamster.toolbox.screen.random.RandomNumberScreen
import com.hamster.toolbox.screen.ruler.RulerScreen
import com.hamster.toolbox.screen.schedule.ScheduleScreen
import com.hamster.toolbox.screen.settings.settingsGraph
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.utils.AnimationButton
import com.hamster.toolbox.utils.ButtonPro
import com.hamster.toolbox.utils.prompt.PromptLoader
import com.hamster.toolbox.utils.squircleShape
import com.hamster.toolbox.utils.weather.Weather
import com.hamster.toolbox.utils.weather.WeatherData
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: 天气,向下滑动天气透明度逐渐降低
// TODO: tips页面
// TODO: 通知栏字体颜色适配
// TODO: 测距
// TODO: preferencesDataStore

class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechRecognizerManager
    private var isModelReady = false

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSpeechManager()

        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()

            // 加载
            var showLoading by remember { mutableStateOf(false) }
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_anim))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            var isMenuExpanded by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            val hazeState = remember { HazeState() }
            val blurRadius by animateDpAsState(
                targetValue = if (isMenuExpanded || showLoading) 8.dp else 0.dp,
                animationSpec = tween(100),
                label = "blur"
            )

            var showWeatherDetail by remember { mutableStateOf(false) }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val universalButtonIconId = when {
                currentDestination?.hasRoute<Schedule>() == true -> R.drawable.ic_add
                currentDestination?.hasRoute<SetKeywords>() == true -> R.drawable.ic_add
                else -> R.drawable.ic_microphone
            }

            val currentTitle = when {
                currentDestination?.hasRoute<Schedule>() == true -> "课程表"
                currentDestination?.hasRoute<RandomNumber>() == true ->"随机数"
                currentDestination?.hasRoute<Settings>() == true -> "设置"
                currentDestination?.hasRoute<SetKeywords>() == true -> "热词管理"
                currentDestination?.hasRoute<ImportCurriculum>() == true -> "导入课程表"
                currentDestination?.hasRoute<WeatherSettings>() == true -> "天气"
                else -> "ToolBox"
            }

            val showTopBar = when {
                currentDestination?.hasRoute<Ruler>() == true -> false
                currentDestination?.hasRoute<GameConsole>() == true -> false
                else -> true
            }

            val showBottomMenu = when {
                currentDestination?.hasRoute<Schedule>() == true -> true
                currentDestination?.hasRoute<RandomNumber>() == true -> true
                currentDestination?.hasRoute<Ruler>() == true -> false
                currentDestination?.hasRoute<Settings>() == true -> true
                currentDestination?.hasRoute<SetKeywords>() == true -> true
                currentDestination?.hasRoute<ImportCurriculum>() == true -> true
                currentDestination?.hasRoute<GameConsole>() == true -> false
                else -> true
            }

            val backdrop = rememberLayerBackdrop {
                drawContent()
            }

            var showSearchBar by remember { mutableStateOf(false) }
            var searchText by remember { mutableStateOf("") }
            val searchBarWeight by animateFloatAsState(
                targetValue = if (showSearchBar) 1f else 0.001f,
                label = "weight_anim",
                animationSpec = spring(stiffness = Spring.StiffnessLow) // 动画弹性设定
            )

            val isSetKeyWordsScreen = currentDestination?.hierarchy?.any { it.hasRoute<SetKeywords>() } == true

            // 拦截返回事件
            if (isMenuExpanded || showSearchBar || showLoading) {
                BackHandler {
                    isMenuExpanded = false
                    showSearchBar = false
                }
            }

            // 展开菜单栏导航
            fun NavHostController.expandMenuNavigate(route: Any) {
                this.navigate(route) {
                    popUpTo(this@expandMenuNavigate.graph.findStartDestination().id) {
                        saveState = true // 保留滚动状态
                    }
                    launchSingleTop = true // 同一个页面不会创建新的实例
                    restoreState = true // 恢复之前的状态
                }

                isMenuExpanded = false
                showSearchBar = false
            }

            fun setLoading(isLoading: Boolean) {
                showLoading = isLoading
            }

            MaterialTheme {
                CompositionLocalProvider( LocalOverscrollFactory provides null) { // 禁用边缘回弹和光晕效果
                    Surface(modifier = Modifier.fillMaxSize(), color = colorResource(R.color.background)) { // 覆盖原有的主题色背景
                        Scaffold { _ ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Schedule,
                                    modifier = Modifier
                                        .layerBackdrop(backdrop) // 应用玻璃效果
                                        .hazeSource(state = hazeState)
                                        .fillMaxSize()
                                ) {
                                    // 课程表
                                    composable<Schedule> {
                                        ScheduleScreen()
                                    }

                                    // 随机数
                                    composable<RandomNumber> {
                                        RandomNumberScreen()
                                    }

                                    // 尺子
                                    composable<Ruler> {
                                        RulerScreen()
                                    }

                                    // 游戏机
                                    composable<GameConsole> {
                                        GameConsoleScreen()
                                    }

                                    // 设置
                                    settingsGraph(
                                        navController = navController,
                                        mainViewModel = mainViewModel,
                                        setLoading = { isLoading ->
                                            setLoading(isLoading)
                                        }
                                    )
                                }

                                // 高斯模糊层
                                if (isMenuExpanded || blurRadius > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeEffect(
                                                state = hazeState,
                                                style = HazeStyle(
                                                    blurRadius = blurRadius,
                                                    tint = HazeTint(Color.Transparent),
                                                    noiseFactor = 0f
                                                )
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                isMenuExpanded = false
                                                showSearchBar = false
                                            }
                                    )
                                }

                                // 底部菜单栏
                                if (showBottomMenu) {
                                    Box(modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            // 展开菜单栏
                                            AnimatedVisibility(
                                                visible = isMenuExpanded,
                                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Surface(
                                                    shape = squircleShape,
                                                    color = colorResource(R.color.bg_dialog),
                                                    tonalElevation = 8.dp,
                                                    shadowElevation = 8.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(512.dp)
                                                        .padding(horizontal = 12.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            // 课程表
                                                            IconButton(onClick = { navController.expandMenuNavigate(Schedule) }) {
                                                                Icon(painterResource(R.drawable.ic_calendar), null, tint = Color.Gray)
                                                            }
                                                            // 尺子
                                                            IconButton(onClick = { navController.expandMenuNavigate(Ruler) }) {
                                                                Icon(painterResource(R.drawable.ic_ruler), null, tint = Color.Gray)
                                                            }
                                                            // 随机数
                                                            IconButton(onClick = { navController.expandMenuNavigate(RandomNumber) }) {
                                                                Icon(painterResource(R.drawable.ic_numbers), null, tint = Color.Gray)
                                                            }
                                                            // 游戏
                                                            IconButton(onClick = { navController.expandMenuNavigate(GameConsole) }) {
                                                                Icon(painterResource(R.drawable.ic_game_console), null, tint = Color.Gray)
                                                            }
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            // 设置
                                                            IconButton(onClick = { navController.expandMenuNavigate(SettingsGraph) }) {
                                                                Icon(painterResource(R.drawable.ic_settings), null, tint = Color.Gray)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // 底部菜单栏
                                            Surface(
                                                color = colorResource(R.color.bg_dialog),
                                                shape = squircleShape,
                                                tonalElevation = 8.dp,
                                                shadowElevation = 8.dp,
                                                modifier = Modifier
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    .height(64.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                // 搜索按钮
                                                Box(modifier = Modifier.fillMaxSize()) { // 不让搜索框与通用按钮和展开按钮产生碰撞
                                                    Row(
                                                        modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .height(IntrinsicSize.Min) // 保持高度一致
                                                                .then(if (showSearchBar) Modifier.weight(searchBarWeight, fill = false) else Modifier)
                                                                .clip(squircleShape)
                                                                .animateContentSize() // 自动处理宽度变化的动画
                                                                .background(
                                                                    color = if (showSearchBar) Color.Gray.copy(alpha = 0.25f) else Color.Transparent,
                                                                    shape = squircleShape
                                                                ),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(48.dp)
                                                                    .clickable(
                                                                        onClick = { showSearchBar = !showSearchBar },
                                                                        indication = null,
                                                                        interactionSource = remember { MutableInteractionSource() } // 必须配合 interactionSource 使用
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(painterResource(R.drawable.ic_search), null, tint = Color.Gray)
                                                            }

                                                            AnimatedVisibility(
                                                                visible = showSearchBar,
                                                                enter = expandHorizontally() + fadeIn(),
                                                                exit = shrinkHorizontally() + fadeOut(),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                BasicTextField(
                                                                    value = searchText,
                                                                    onValueChange = { searchText = it },
                                                                    modifier = Modifier
                                                                        .fillMaxSize(),
                                                                    singleLine = true,
                                                                    decorationBox = { innerTextField ->
                                                                        Box(contentAlignment = Alignment.CenterStart) {
                                                                            if (searchText.isEmpty()) {
                                                                                Text("请输入搜索内容...", color = Color.Gray)
                                                                            }
                                                                            innerTextField()
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // 通用按钮
                                                AnimatedVisibility(
                                                    visible = !showSearchBar,
                                                    enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                                                    exit = scaleOut(targetScale = 0.5f) + fadeOut()
                                                ) {
                                                    Box(modifier = Modifier.height(48.dp).width(72.dp), contentAlignment = Alignment.Center) {
                                                        ButtonPro(
                                                            icon = universalButtonIconId,
                                                            onTap = {
                                                                if (isSetKeyWordsScreen) {
                                                                    mainViewModel.isShowAddKeywordDialog = true
                                                                }
                                                            },
                                                            onLongPressStart = {
                                                                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                                    speechManager.startListening()
                                                                } else {
                                                                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                                }
                                                            },
                                                            onLongPressEnd = {
                                                                speechManager.stopListening()
                                                            }
                                                        )
                                                    }
                                                }

                                                // 展开按钮
                                                Box(modifier = Modifier.height(48.dp).width(72.dp).padding(horizontal = 36.dp), contentAlignment = Alignment.CenterEnd) {
                                                    AnimationButton(
                                                        animation = R.raw.ic_arrow_anim,
                                                        changed = isMenuExpanded,
                                                        onClick = {
                                                            if (showSearchBar && !isMenuExpanded) {
                                                                showSearchBar = false
                                                                return@AnimationButton
                                                            }
                                                            isMenuExpanded = !isMenuExpanded
                                                            showSearchBar = isMenuExpanded
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 显示顶部标题栏
                                if (showTopBar) {
                                    CenterAlignedTopAppBar(
                                        title = {
                                            AnimatedContent (
                                                targetState = showWeatherDetail,
                                                transitionSpec = {
                                                    if (showWeatherDetail) {
                                                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                                                (slideOutHorizontally { width -> -width } + fadeOut())
                                                    } else {
                                                        (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                                                (slideOutHorizontally { width -> width } + fadeOut())
                                                    }
                                                }
                                            ) { targetIsWeather ->
                                                if (targetIsWeather) {
                                                    Text("${WeatherData.getLocation() ?: ""} ${WeatherData.getWeatherState() ?: ""}")
                                                } else {
                                                    Text(currentTitle)
                                                }
                                            }
                                        },
                                        actions = {
                                            Weather(
                                                onClick = {
                                                    showWeatherDetail = !showWeatherDetail
                                                }
                                            )
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color.Transparent,
                                            scrolledContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .height(80.dp)
                                            .fillMaxWidth()
                                            .shadow(elevation = 0.dp)
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedCornerShape(0.dp) },
                                                effects = {
                                                    vibrancy()
                                                    blur(4f.dp.toPx())
                                                    lens(12f.dp.toPx(), 8f.dp.toPx()) },)
                                    )
                                }

                                // 显示加载
                                if (showLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                showLoading = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LottieAnimation(
                                            composition = composition,
                                            progress = { progress },
                                            modifier = Modifier.size(196.dp)
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

    override fun onStart() {
        super.onStart()
        PromptLoader.getPromptById(this, "assistant")?.let { AI.chatHistory.add(Message("system", it)) }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.release()
    }

    suspend fun assistantSend(content: String, apiKey: String) {
        try {
            val result = AI.sendToAssistant(content, apiKey)
            if (result != null) {
                assistantQuery(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun assistantQuery(response: AiResponse) {
        var isChatOrQa = false

        when(response.type) {
            "chat", "qa" -> {
//                if (navController.currentDestination?.id != R.id.nav_assistant) {
//                    showAssistantBubble(response.content)
//                }
                isChatOrQa = true
            }
            "avatar", "nickname", "signature", "semester_start_date", "import_curriculum_options",
            "curriculum_notification", "class_notification", "alarm_notification", "assistant_avatar",
            "assistant_nickname", "api_key" -> {
                settingsScrollTo(response.type)
//                if (navController.currentDestination?.id != R.id.nav_assistant) {
////                    showAssistantBubble(response.content)
//                }
            }
            "nav_curriculum" -> {
//                navController.navigateStandard(R.id.nav_curriculum)
            }
            "nav_ruler" -> {
//                navController.navigateStandard(R.id.nav_ruler)
            }
            "nav_assistant" -> {
//                navController.navigateStandard(R.id.nav_assistant)
            }
            "nav_settings" -> {
//                navController.navigateStandard(R.id.nav_settings)
            }
            "nav_random" -> {
//                navController.navigateStandard(R.id.nav_random)
            }
            "set_alarm" -> {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                val apiKey = prefs.getString("api_key", null)
                if (apiKey.isNullOrBlank()) {
//                    showAssistantBubble("需要先在设置中配置API")
                    settingsScrollTo("api_key")
                    return
                }
                val alarmResponse = AI.sendWithPrompt(this, response.content, "set_alarm", apiKey)
                val alarm = Alarm()
                alarm.setAlarmFromJSON(this, alarmResponse?.content)
            }
        }

        if (!isChatOrQa) {
            AI.chatHistory.add(Message("assistant", "好的"))
        }
    }

    fun settingsScrollTo(id: String) {
        val bundle = Bundle().apply {
            putString("assistant_find", id)
            putLong("trigger_key", System.currentTimeMillis())
        }

//        navController.navigateStandard(R.id.nav_settings, bundle)
    }

    private fun initSpeechManager() {
        speechManager = SpeechRecognizerManager(this)
        lifecycleScope.launch(Dispatchers.IO) {
            speechManager.initModel()
            isModelReady = true
        }

        // 流式输出结果
        speechManager.onPartialResult = { text ->
            Log.d("saying", text)
        }

        // 最终输出结果
        lifecycleScope.launch {
            speechManager.finalResultFlow.collect { text ->
                if (speechManager.isRecording) {
                    speechManager.stopListening()
                }

                val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                val apiKey = prefs.getString("api_key", null)
                if (apiKey.isNullOrEmpty()) {
                    // 无api key逻辑
                    return@collect
                }

                withContext(Dispatchers.IO) {
                    if (text.isNotEmpty()) {
                        assistantSend(text, apiKey)
                        Log.d("say", text)
                    }
                }
            }
        }
    }
}