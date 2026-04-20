package com.hamster.toolbox.screen.diary

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.hamster.toolbox.Diary
import com.hamster.toolbox.DiaryGraph
import com.hamster.toolbox.DiaryPreview
import com.hamster.toolbox.Route
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.utils.scaleInPopEnter
import com.hamster.toolbox.utils.scaleOutExit
import com.hamster.toolbox.utils.slideInWithScaleEnter
import com.hamster.toolbox.utils.slideOutWithScalePopExit

fun NavGraphBuilder.diaryGraph(
    mainViewModel: MainViewModel,
    viewModel: DiaryViewModel,
    onNavigate: (Route) -> Unit
) {
    navigation<DiaryGraph>(startDestination = DiaryPreview) {
        composable<DiaryPreview>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            DiaryPreviewScreen(
                mainViewModel = mainViewModel,
                viewModel = viewModel,
                onNavigate = { onNavigate(it) }
            )
        }

        composable<Diary>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            DiaryScreen()
        }
    }
}