package com.hamster.toolbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.AiResponse
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.ai.SpeechRecognizerManager
import com.hamster.toolbox.screen.random.RandomNumberScreen
import com.hamster.toolbox.screen.ruler.RulerScreen
import com.hamster.toolbox.screen.settings.settingsGraph
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.utils.AnimationButton
import com.hamster.toolbox.utils.ButtonPro
import com.hamster.toolbox.utils.prompt.PromptLoader
import com.hamster.toolbox.utils.squircleShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//TODO:右上角显示温度/湿度，点击展开详情
//TODO:磨砂玻璃边缘弯曲效果？
//TODO:天气,向下滑动天气透明度逐渐降低
//TODO:dialog从点击位置出现？
//TODO:课程新增日期字段，可空，导入课程表时自动计算
//TODO:导入课程表前先设置开学日期
//TODO:tips页面
//TODO:标题栏把日期选择器的顶部模糊了？
//TODO:通知栏字体颜色适配
//TODO:测距
//TODO:小游戏（单人棋，数独，数织，贪吃蛇，2048，点灯游戏，俄罗斯方块）

class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechRecognizerManager
    private var isModelReady = false

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 同意权限后逻辑
        } else {
            // 不同意权限逻辑
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSpeechManager()

        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()

            var isMenuExpanded by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            val hazeState = remember { HazeState() }
            val blurRadius by animateDpAsState(
                targetValue = if (isMenuExpanded) 8.dp else 0.dp,
                animationSpec = tween(100),
                label = "blur"
            )

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val currentTitle = when {
                currentDestination?.hasRoute<RandomNumber>() == true ->"随机数"
                currentDestination?.hasRoute<Settings>() == true -> "设置"
                currentDestination?.hasRoute<SetKeywords>() == true -> "热词管理"
                currentDestination?.hasRoute<ImportCurriculum>() == true -> "导入课程"
                else -> "ToolBox"
            }

            val showTopBar = true
            val showBottomMenu = true

            val isSetKeyWordsScreen = currentDestination?.hierarchy?.any { it.hasRoute<SetKeywords>() } == true

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

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colorResource(R.color.background)) { // 覆盖原有的主题色背景
                    Scaffold { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = RandomNumber,
                                modifier = Modifier
                                    .hazeSource(state = hazeState)
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                                    .fillMaxSize()
                            ) {
                                // 随机数
                                composable<RandomNumber> {
                                    RandomNumberScreen()
                                }

                                // 尺子
                                composable<Ruler> {
                                    RulerScreen()
                                }

                                // 设置
                                settingsGraph(
                                    navController = navController,
                                    mainViewModel = mainViewModel
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
                                                tint = HazeTint(Color.White.copy(alpha = 0.2f)),
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
//                                                  IconButton(onClick = { onNavigate() }) {
//                                                      Icon(painterResource(R.drawable.ic_calendar), null, tint = Color.Gray)
//                                                  }
                                                        // 尺子
                                                        IconButton(onClick = { navController.expandMenuNavigate(Ruler) }) {
                                                            Icon(painterResource(R.drawable.ic_ruler), null, tint = Color.Gray)
                                                        }
                                                        // 随机数
                                                        IconButton(onClick = { navController.expandMenuNavigate(RandomNumber) }) {
                                                            Icon(painterResource(R.drawable.ic_numbers), null, tint = Color.Gray)
                                                        }
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
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 48.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // 搜索按钮
                                                IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_search), null, tint = Color.Gray) }

                                                // 通用按钮
                                                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                                    ButtonPro(
                                                        icon = R.drawable.ic_microphone,
                                                        onTap = {
                                                            if (isSetKeyWordsScreen) {
                                                                mainViewModel.isShowAddKeywordDialog = true
                                                            }
                                                        },
                                                        onLongPressStart = {
                                                            runWithPermission(Manifest.permission.RECORD_AUDIO, requestAudioPermissionLauncher) {
                                                                speechManager.startListening()
                                                            }
                                                        },
                                                        onLongPressEnd = {
                                                            speechManager.stopListening()
                                                        }
                                                    )
                                                }

                                                // 展开按钮
                                                AnimationButton(
                                                    animation = R.raw.ic_arrow_anim,
                                                    changed = isMenuExpanded,
                                                    onClick = { isMenuExpanded = !isMenuExpanded }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 显示顶部标题栏
                            if (showTopBar) {
                                CenterAlignedTopAppBar(
                                    title = { Text(currentTitle) },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        scrolledContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .height(96.dp)
                                        .fillMaxWidth()
                                        .hazeEffect(
                                            state = hazeState,
                                            style = HazeStyle(
                                                blurRadius = 8.dp,
                                                tint = HazeTint(Color.White.copy(alpha = 0.2f)),
                                                noiseFactor = 0.05f
                                            )
                                        )
                                )
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

    // 显示加载动画
    fun showLoading(show: Boolean) {
        // TODO：加载动画
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

    private fun runWithPermission(permission: String, launcher: ActivityResultLauncher<String>, action: () -> Unit) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            launcher.launch(permission)
        }
    }


}