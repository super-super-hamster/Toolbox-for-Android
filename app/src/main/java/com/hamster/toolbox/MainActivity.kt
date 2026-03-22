package com.hamster.toolbox

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.ai.SpeechRecognizerManager
import com.hamster.toolbox.main.AudioSpectrumVisualizer
import com.hamster.toolbox.main.ExpandedBottomMenu
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.screen.gameConsole.GameConsoleScreen
import com.hamster.toolbox.screen.random.RandomNumberScreen
import com.hamster.toolbox.screen.ruler.RulerScreen
import com.hamster.toolbox.screen.schedule.ScheduleScreen
import com.hamster.toolbox.screen.settings.settingsGraph
import com.hamster.toolbox.screen.tips.TipsScreen
import com.hamster.toolbox.utils.compose.AnimationButton
import com.hamster.toolbox.utils.compose.ButtonPro
import com.hamster.toolbox.utils.compose.squircleShape
import com.hamster.toolbox.utils.prompt.PromptLoader
import com.hamster.toolbox.utils.scaleInPopEnter
import com.hamster.toolbox.utils.scaleOutExit
import com.hamster.toolbox.utils.slideInWithScaleEnter
import com.hamster.toolbox.utils.slideOutWithScalePopExit
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
    private val mainViewModel: MainViewModel by viewModels()

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
            val context = LocalContext.current
            val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

            // 底部菜单下标
            var selectedIndex by remember { mutableIntStateOf(0) }

            var inputText by remember { mutableStateOf("") }

            // 加载
            var showLoading by remember { mutableStateOf(false) }
            val loadingComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_anim))
            val loadingProgress by animateLottieCompositionAsState(
                composition = loadingComposition,
                iterations = LottieConstants.IterateForever
            )

            // 录音
            var showRecording by remember { mutableStateOf(false) }

            var isMenuExpanded by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            val hazeState = remember { HazeState() }
            val blurRadius by animateDpAsState(
                targetValue = if (isMenuExpanded || showLoading || showRecording) 8.dp else 0.dp,
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
                currentDestination?.hasRoute<Tips>() == true -> "Tips"
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

            val isSetKeyWordsScreen = currentDestination?.hierarchy?.any { it.hasRoute<SetKeywords>() } == true

            // 拦截返回事件
            if (isMenuExpanded || showLoading) {
                BackHandler {
                    isMenuExpanded = false
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
                                    composable<Schedule>(
                                        enterTransition = { slideInWithScaleEnter() },
                                        exitTransition = { scaleOutExit() },
                                        popEnterTransition = { scaleInPopEnter() },
                                        popExitTransition = { slideOutWithScalePopExit() }
                                    ) {
                                        ScheduleScreen()
                                    }

                                    // 随机数
                                    composable<RandomNumber>(
                                        enterTransition = { slideInWithScaleEnter() },
                                        exitTransition = { scaleOutExit() },
                                        popEnterTransition = { scaleInPopEnter() },
                                        popExitTransition = { slideOutWithScalePopExit() }
                                    ) {
                                        RandomNumberScreen()
                                    }

                                    // 尺子
                                    composable<Ruler>(
                                        enterTransition = { slideInWithScaleEnter() },
                                        exitTransition = { scaleOutExit() },
                                        popEnterTransition = { scaleInPopEnter() },
                                        popExitTransition = { slideOutWithScalePopExit() }
                                    ) {
                                        RulerScreen()
                                    }

                                    composable<Tips> (
                                        enterTransition = { slideInWithScaleEnter() },
                                        exitTransition = { scaleOutExit() },
                                        popEnterTransition = { scaleInPopEnter() },
                                        popExitTransition = { slideOutWithScalePopExit() }
                                    ) {
                                        TipsScreen()
                                    }

                                    // 游戏机
                                    composable<GameConsole>(
                                        enterTransition = { slideInWithScaleEnter() },
                                        exitTransition = { scaleOutExit() },
                                        popEnterTransition = { scaleInPopEnter() },
                                        popExitTransition = { slideOutWithScalePopExit() }
                                    ) {
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
                                                modifier = Modifier.padding(vertical = 6.dp)
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
                                                    ExpandedBottomMenu(
                                                        apiKey = prefs.getString("api_key", null),
                                                        mainViewModel = mainViewModel,
                                                        selectedIndex = selectedIndex,
                                                        setSelectedIndex = { selectedIndex = it },
                                                        inputText = inputText,
                                                        setInputText = { inputText = it },
                                                        onNavigate = { navController.expandMenuNavigate(it) })
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
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 48.dp)
                                                ) {
                                                    // 助手按钮
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .align(Alignment.CenterStart)
                                                            .clickable(
                                                                onClick = {
                                                                    selectedIndex = 0
                                                                    isMenuExpanded = !isMenuExpanded
                                                                },
                                                                indication = null,
                                                                interactionSource = remember { MutableInteractionSource() } // 必须配合 interactionSource 使用
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(painterResource(R.drawable.ic_assistant), null, tint = Color.Gray)
                                                    }

                                                    // 通用按钮
                                                    Box(modifier = Modifier.height(48.dp).width(72.dp).align(Alignment.Center), contentAlignment = Alignment.Center) {
                                                        ButtonPro(
                                                            icon = universalButtonIconId,
                                                            onTap = {
                                                                if (isSetKeyWordsScreen) {
                                                                    mainViewModel.isShowAddKeywordDialog = true
                                                                } },
                                                            onLongPressStart = {
                                                                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && isModelReady) {
//                                                                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                                    showRecording = true
                                                                    speechManager.startListening()
                                                                } else {
                                                                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                                } },
                                                            onLongPressEnd = {
                                                                showRecording = false

                                                                if (mainViewModel.speechFinalResult != "") {
                                                                    selectedIndex = 0
                                                                    isMenuExpanded = true
                                                                    inputText = mainViewModel.speechFinalResult
                                                                    mainViewModel.setSpeechFinalResult("")
                                                                }

                                                                speechManager.stopListening()
                                                            }
                                                        )
                                                    }

                                                    // 展开按钮
                                                    Box(modifier = Modifier.height(48.dp).width(72.dp).align(Alignment.CenterEnd), contentAlignment = Alignment.CenterEnd) {
                                                        AnimationButton(
                                                            animation = R.raw.ic_arrow_anim,
                                                            changed = isMenuExpanded,
                                                            onClick = {
                                                                selectedIndex = 1
                                                                isMenuExpanded = !isMenuExpanded
                                                            }
                                                        )
                                                    }
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
                                            composition = loadingComposition,
                                            progress = { loadingProgress },
                                            modifier = Modifier.size(196.dp)
                                        )
                                    }
                                }

                                // 显示录音
                                if (showRecording) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedVisibility(
                                            visible = speechManager.isRecording,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            AudioSpectrumVisualizer(
                                                spectrumFlow = speechManager.audioSpectrumFlow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(256.dp),
                                                numLines = 20,
                                                lineColor = colorResource(R.color.audio_spectrum)
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
    }

    override fun onStart() {
        super.onStart()
        PromptLoader.getPromptById(this, "assistant")?.let { AI.chatHistory.add(Message("system", it)) }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.release()
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
                        mainViewModel.setSpeechFinalResult(text)
                    }
                }
            }
        }
    }
}