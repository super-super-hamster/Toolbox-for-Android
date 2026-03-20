package com.hamster.toolbox.screen.gameConsole

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.compose.squircleShape
import kotlin.math.max
import kotlin.math.min

@Composable
fun MenuScreen(
    viewModel: ConsoleViewModel,
    onGameSelected: (game: GameType) -> Unit
) {
    val games = GameType.entries
    val pagerState = rememberPagerState(pageCount = { games.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.inputEvent.collect { input ->
            when (input) {
                ConsoleInput.UP -> {
                    pagerState.animateScrollToPage(max(pagerState.currentPage - 1, 0))
                }
                ConsoleInput.DOWN -> {
                    pagerState.animateScrollToPage(min(pagerState.currentPage + 1, games.size - 1))
                }
                ConsoleInput.A -> {
                    onGameSelected(games[pagerState.currentPage])
                }
                else -> {}
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val itemSize = maxWidth * 0.8f
        val verticalPadding = (maxHeight - itemSize) / 2

        VerticalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(itemSize),
            contentPadding = PaddingValues(vertical = verticalPadding),
            pageSpacing = 24.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val game = games[page]

            Box(
                modifier = Modifier
                    .size(itemSize)
                    .background(color = colorResource(R.color.mikuGreen), shape = squircleShape)
                    .clickable{ onGameSelected(game) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(game.icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}