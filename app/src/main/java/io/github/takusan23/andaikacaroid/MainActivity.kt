package io.github.takusan23.andaikacaroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.andaikacaroid.ui.screen.HdrVideoToUltraHdrScreen
import io.github.takusan23.andaikacaroid.ui.screen.HomeScreen
import io.github.takusan23.andaikacaroid.ui.screen.NavigationPaths
import io.github.takusan23.andaikacaroid.ui.screen.UltraHdrToHdrVideoScreen
import io.github.takusan23.andaikacaroid.ui.screen.setting.LicenseScreen
import io.github.takusan23.andaikacaroid.ui.screen.setting.SettingScreen
import io.github.takusan23.andaikacaroid.ui.theme.UltraikaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltraikaTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationPaths.Home.path) {

        composable(NavigationPaths.Home.path) {
            HomeScreen(
                onMenuClick = { navController.navigate(NavigationPaths.Setting.path) },
                onNavigate = { navController.navigate(it.path) }
            )
        }

        composable(NavigationPaths.HdrVideoToUltraHdr.path) {
            HdrVideoToUltraHdrScreen()
        }

        composable(NavigationPaths.UltraHdrToHdrVideo.path) {
            UltraHdrToHdrVideoScreen()
        }

        composable(NavigationPaths.Setting.path) {
            SettingScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it.path) }
            )
        }

        composable(NavigationPaths.License.path) {
            LicenseScreen(onBack = { navController.popBackStack() })
        }
    }
}