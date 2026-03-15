package com.hamster.toolbox.screen.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.hamster.toolbox.ImportCurriculum
import com.hamster.toolbox.MainViewModel
import com.hamster.toolbox.SetKeywords
import com.hamster.toolbox.Settings
import com.hamster.toolbox.SettingsGraph
import com.hamster.toolbox.WeatherSettings
import com.hamster.toolbox.utils.scaleInPopEnter
import com.hamster.toolbox.utils.scaleOutExit
import com.hamster.toolbox.utils.slideInWithScaleEnter
import com.hamster.toolbox.utils.slideOutWithScalePopExit

fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    navigation<SettingsGraph>(startDestination = Settings()) {
        composable<Settings>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) { backStackEntry ->
            // ???
            val args = backStackEntry.toRoute<Settings>()

            SettingsScreen(
                triggerTime = args.trigger,
                jumpTargetId = args.jumpTarget,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable<ImportCurriculum>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            ImportCurriculumScreen(
                onShowLoading = { isShowLoading ->
                    // TODO: 显示加载动画
                },
                onNavigateToSettings = {
                    // TODO：导航回settings
                },
            )
        }

        composable<SetKeywords>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            SetKeywordsScreen(
                mainViewModel = mainViewModel
            )
        }

        composable<WeatherSettings>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            WeatherSettings()
        }
    }
}