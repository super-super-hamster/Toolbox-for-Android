package com.hamster.toolbox.screen.tips

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.ColorPickerTips
import com.hamster.toolbox.DecibelMeterTips
import com.hamster.toolbox.DiaryTips
import com.hamster.toolbox.RandomNumberTips
import com.hamster.toolbox.RulerTips
import com.hamster.toolbox.ScheduleTips
import com.hamster.toolbox.TimeTips
import com.hamster.toolbox.Tips
import com.hamster.toolbox.TipsGraph
import com.hamster.toolbox.WeatherTips
import com.hamster.toolbox.utils.scaleInPopEnter
import com.hamster.toolbox.utils.scaleOutExit
import com.hamster.toolbox.utils.slideInWithScaleEnter
import com.hamster.toolbox.utils.slideOutWithScalePopExit

fun NavGraphBuilder.tipsGraph(
    navController: NavController
) {
    navigation<TipsGraph>(startDestination = Tips) {
        composable<Tips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            TipsScreen(
                onNavigate = { navController.navigate(it) }
            )
        }

        composable<WeatherTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            WeatherTipsScreen()
        }

        composable<ScheduleTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            ScheduleTipsScreen()
        }

        composable<AssistantTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            AssistantTipsScreen()
        }

        composable<ColorPickerTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            ColorPickerTipsScreen()
        }

        composable<RandomNumberTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            RandomNumberTipsScreen()
        }

        composable<RulerTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            RulerTipsScreen()
        }

        composable<TimeTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            TimeTipsScreen()
        }

        composable<DiaryTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            DiaryTipsScreen()
        }

        composable<DecibelMeterTips>(
            enterTransition = { slideInWithScaleEnter() },
            exitTransition = { scaleOutExit() },
            popEnterTransition = { scaleInPopEnter() },
            popExitTransition = { slideOutWithScalePopExit() }
        ) {
            DecibelMeterTipsScreen()
        }
    }
}