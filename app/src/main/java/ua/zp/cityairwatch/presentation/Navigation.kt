package ua.zp.cityairwatch.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.StartScreen.route) {
        composable(Screen.StartScreen.route) {
            StartScreen(navController = navController)
        }
        composable(Screen.ResultScreen.route) {
            ResultScreen()
        }
    }
}

sealed class Screen(val route: String) {
    object StartScreen : Screen("start_screen")
    object ResultScreen : Screen("result_screen")
}