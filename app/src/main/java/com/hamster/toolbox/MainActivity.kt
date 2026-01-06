package com.hamster.toolbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.AiResponse
import com.hamster.toolbox.ai.Message
import com.hamster.toolbox.ai.SpeechRecognizerManager
import com.hamster.toolbox.screen.settings.SettingsScreen
import com.hamster.toolbox.system.Alarm
import com.hamster.toolbox.utils.ButtonPro
import com.hamster.toolbox.utils.prompt.PromptLoader
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.lib.squircleshape.CornerSmoothing
import sv.lib.squircleshape.SquircleShape

//TODO:天气
//TODO:切换界面动画
//TODO:dialog从点击位置出现？

fun NavController.navigateStandard(resId: Int, bundle: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        //弹出到导航图的起始位置，避免回退栈无限堆积
        .setPopUpTo(graph.findStartDestination().id, inclusive = false, saveState = true)
        //避免重复创建同一个页面的实例
        .setLaunchSingleTop(true)
        //尝试恢复状态
        .setRestoreState(true)
        .build()

    this.navigate(resId, bundle, navOptions)
}

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

        setContent {
            val mainViewModel: MainViewModel = viewModel()

            val navController = rememberNavController()

            val scaffoldState = rememberBottomSheetScaffoldState()

            val scope = rememberCoroutineScope()

            val hazeState = remember { HazeState() }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val currentTitle = when {
                currentDestination?.hasRoute<Settings>() == true -> "设置"
                currentDestination?.hasRoute<SetKeywords>() == true -> "热词管理"
                currentDestination?.hasRoute<ImportCurriculum>() == true -> "导入课程"
                else -> "ToolBox"
            }

            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            val showTopBar = true
            val topBarHeight = 96.dp
            val sheetHeight = screenHeight - topBarHeight

            val isSetKeyWordsScreen = currentDestination?.hierarchy?.any { it.hasRoute<SetKeywords>() } == true

            val topSquircleShape = SquircleShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                smoothing = CornerSmoothing.Medium
            )

            MaterialTheme {
                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 108.dp,
                    sheetShape = topSquircleShape,
                    sheetContainerColor = colorResource(R.color.bg_dialog),
                    containerColor = Color.Transparent,
                    topBar = { },

                    sheetDragHandle = null,

                    sheetContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sheetHeight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 48.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_search), null, tint = Color.Gray) }

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

                                IconButton(
                                    // 展开底部抽屉
                                    onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } }
                                ) {
                                    Icon(painterResource(R.drawable.ic_arrow_up), null, tint = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
//                        IconButton(onClick = { onNavigate() }) {
//                            Icon(painterResource(R.drawable.ic_calendar), null, tint = Color.Gray)
//                        }
//                        IconButton(onClick = { onNavigate(Destination.Ruler) }) {
//                            Icon(painterResource(R.drawable.ic_ruler), null, tint = Color.Gray)
//                        }
//                        IconButton(onClick = { onNavigate(Destination.Random) }) {
//                            Icon(painterResource(R.drawable.ic_numbers), null, tint = Color.Gray)
//                        }
                                IconButton(onClick = { navController.navigate(Settings) }) {
                                    Icon(painterResource(R.drawable.ic_settings), null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Settings(),
                            modifier = Modifier
                                .hazeSource(state = hazeState)
                                .fillMaxSize()
                        ) {
                            composable<Settings> { backStackEntry ->
                                val trigger = backStackEntry.arguments?.getLong("trigger")
                                val jump = backStackEntry.arguments?.getString("jump")

                                SettingsScreen(
                                    triggerTime = trigger,
                                    jumpTargetId = jump,
                                    onNavigate = { route ->
                                        navController.navigate(route)
                                    }
                                )
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
                                    .statusBarsPadding()
                            )
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