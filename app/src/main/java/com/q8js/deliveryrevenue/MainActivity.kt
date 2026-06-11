package com.q8js.deliveryrevenue

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.q8js.deliveryrevenue.ui.HomeScreen
import com.q8js.deliveryrevenue.ui.MainScreen
import com.q8js.deliveryrevenue.ui.SettingsScreen
import com.q8js.deliveryrevenue.ui.theme.DeliveryRevenueTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permissions
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DeliveryRevenueTheme {
                val navController = rememberNavController()

                val images by viewModel.imageItems.collectAsStateWithLifecycle()
                val appState by viewModel.appState.collectAsStateWithLifecycle()
                val emailState by viewModel.emailState.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateToMain = { navController.navigate("main") },
                            onNavigateToDailyReport = { navController.navigate("daily_report") }
                        )
                    }
                    composable("daily_report") {
                        com.q8js.deliveryrevenue.ui.DailyReportScreen(
                            appState = appState,
                            settings = settings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            images = images,
                            appState = appState,
                            emailState = emailState,
                            onAddImages = viewModel::addImages,
                            onRemoveImage = viewModel::removeImage,
                            onClearAll = viewModel::clearAll,
                            onProcess = viewModel::processImages,
                            onSendEmail = viewModel::sendEmail,
                            onSettingsClick = { navController.navigate("settings") },
                            onDismissEmailResult = viewModel::resetEmailState
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            current = settings,
                            onSave = viewModel::saveSettings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
