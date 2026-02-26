package com.example.qrcord

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qrcord.data.QRHistoryDatabase
import com.example.qrcord.generator.GeneratorScreen
import com.example.qrcord.history.HistoryScreen
import com.example.qrcord.history.HistoryViewModel
import com.example.qrcord.scanner.ScannerScreen
import com.example.qrcord.settings.SettingsDataStore
import com.example.qrcord.settings.SettingsScreen
import com.example.qrcord.settings.SettingsViewModel
import com.example.qrcord.ads.BannerAdView
import com.example.qrcord.billing.BillingManager
import com.example.qrcord.ui.PrivacyNoticeDialog
import com.example.qrcord.ui.theme.QRcordTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermission()

        val billingManager = (application as QRcordApp).billingManager

        setContent {
            QRcordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRcordNavigation(
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = { requestCameraPermission() },
                        billingManager = billingManager,
                        activity = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Scanner : Screen("scanner", "スキャン", Icons.Default.QrCodeScanner)
    data object Generator : Screen("generator", "作成", Icons.Default.QrCode2)
    data object History : Screen("history", "履歴", Icons.Default.History)
    data object Settings : Screen("settings", "設定", Icons.Default.Settings)
}

@Composable
fun QRcordNavigation(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    billingManager: BillingManager,
    activity: MainActivity
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val screens = listOf(Screen.Scanner, Screen.Generator, Screen.History, Screen.Settings)
    val coroutineScope = rememberCoroutineScope()

    // Ad-free status
    val isAdFree by billingManager.isAdFree.collectAsState()

    // Initialize DataStore and ViewModels
    val settingsDataStore = remember { SettingsDataStore(context) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsDataStore)
    )
    val settingsState by settingsViewModel.uiState.collectAsState()

    // Database instance for history deletion
    val database = remember { QRHistoryDatabase.getInstance(context) }

    // Privacy notice dialog state
    var showPrivacyNotice by remember { mutableStateOf(false) }

    // Check if privacy notice should be shown
    LaunchedEffect(settingsState.privacyNoticeShown) {
        if (!settingsState.privacyNoticeShown) {
            showPrivacyNotice = true
        }
    }

    // Privacy Notice Dialog
    if (showPrivacyNotice) {
        PrivacyNoticeDialog(
            onDismiss = {
                showPrivacyNotice = false
                settingsViewModel.updatePrivacyNoticeShown(true)
            }
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                // Banner Ad (only show if not ad-free)
                if (!isAdFree) {
                    BannerAdView()
                }

                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scanner.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scanner.route) {
                ScannerScreen(
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    settingsDataStore = settingsDataStore,
                    isAdFree = isAdFree,
                    activity = activity
                )
            }
            composable(Screen.Generator.route) {
                GeneratorScreen()
            }
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateBack = null
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onClearHistory = {
                        coroutineScope.launch {
                            database.qrHistoryDao().deleteAll()
                        }
                    },
                    billingManager = billingManager,
                    activity = activity
                )
            }
        }
    }
}
