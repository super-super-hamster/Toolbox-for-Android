package com.hamster.toolbox.screen.gameConsole

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.glow
import com.hamster.toolbox.utils.compose.squircleShape
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

//TODO:小游戏（单人棋，数独，数织，贪吃蛇，2048，俄罗斯方块）

// 输入
enum class ConsoleInput { UP, DOWN, LEFT, RIGHT, A, B, X, Y, MENU, NONE }

enum class GameType(@param:DrawableRes val icon: Int) {
    THE2048(R.drawable.ic_2048),
}

// 屏幕状态
sealed class ScreenState {
    object Menu : ScreenState()
    data class Games(val game: GameType) : ScreenState()
}

class ConsoleViewModel : ViewModel() {
    private val _inputEvent = MutableSharedFlow<ConsoleInput>()
    val inputEvent = _inputEvent.asSharedFlow()

    fun onInput(input: ConsoleInput) {
        viewModelScope.launch {
            _inputEvent.emit(input)
        }
    }
}

@Composable
fun GameConsoleScreen(viewModel: ConsoleViewModel = viewModel()) {
    var currentScreenState by remember { mutableStateOf<ScreenState>(ScreenState.Menu) }

    LaunchedEffect(Unit) {
        viewModel.inputEvent.collect { input ->
            if (input == ConsoleInput.MENU) {
                currentScreenState = ScreenState.Menu
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.game_console_background))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 游戏机屏幕
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glow(
                    color = colorResource(R.color.game_console_screen).copy(alpha = 0.5f),
                    blurRadius = 16.dp,
                    spread = 1.dp,
                    shape = squircleShape
                )
                .clip(shape = squircleShape)
                .background(colorResource(R.color.game_console_screen))
        ) {
            Crossfade(targetState = currentScreenState, animationSpec = tween(300) , label = "ScreenState") { state ->
                when (state) {
                    is ScreenState.Menu -> {
                        MenuScreen(viewModel) { game ->
                            currentScreenState = ScreenState.Games(game)
                        }
                    }
                    is ScreenState.Games -> {
                        when (state.game) {
                            GameType.THE2048 -> {}
//                            GameType.Tetris -> {}
//                            GameType.Sudoku -> {}
//                            GameType.Nonogram -> {}
//                            GameType.Snake -> {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Handle {
            viewModel.onInput(it)
        }
    }
}