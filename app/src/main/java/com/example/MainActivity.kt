package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.RoutineViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RoutineViewModel by viewModels()

    // Notification Permission Launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission outcome can be logged or handled if desired
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission if Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                var currentTab by remember { mutableIntStateOf(0) }

                val tabs = listOf(
                    NavigationTab("Dashboard", Icons.Default.Dashboard, "dashboard"),
                    NavigationTab("Schedule", Icons.Default.CalendarMonth, "schedule"),
                    NavigationTab("Scanner", Icons.Default.QrCodeScanner, "scanner"),
                    NavigationTab("Progress", Icons.Default.Insights, "progress"),
                    NavigationTab("Settings", Icons.Default.Settings, "settings")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = currentTab == index,
                                    onClick = { currentTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_item_${tab.tag}")
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScan = { currentTab = 2 }
                            )
                            1 -> ScheduleScreen(
                                viewModel = viewModel
                            )
                            2 -> ScannerScreen(
                                viewModel = viewModel
                            )
                            3 -> ProgressScreen(
                                viewModel = viewModel
                            )
                            4 -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTab(
    val label: String,
    val icon: ImageVector,
    val tag: String
)
