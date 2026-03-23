package com.hamster.toolbox.screen.tips

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.hamster.toolbox.AssistantTips
import com.hamster.toolbox.ScheduleTips
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
    }
}