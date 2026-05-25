package com.example.fajrapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fajrapp.viewmodel.SettingsViewModel
import com.example.fajrapp.ui.LanguageSelectionScreen
import com.example.fajrapp.ui.LocationScreen
import com.example.fajrapp.ui.CalculationMethodScreen
import com.example.fajrapp.ui.HijriCalendarScreen
import com.example.fajrapp.ui.TimeOffsetScreen
import com.example.fajrapp.ui.PrayerAlarmsScreen
import com.example.fajrapp.ui.PrayerAlarmAddScreen
import com.example.fajrapp.ui.PrayerAlarmEditScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fajrapp.ui.PrayerScreen
import com.example.fajrapp.ui.SettingsScreen
import com.example.fajrapp.ui.theme.FajrAppTheme
import com.example.fajrapp.viewmodel.PrayerAlarmViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissionLauncher.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
        )

        setContent {
            FajrAppTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val hazeState = remember { HazeState() }
                    val settingsViewModel: SettingsViewModel = viewModel() // Activity-scoped ViewModel
                    val prayerAlarmViewModel: PrayerAlarmViewModel = viewModel()

                    // Shared Background with Haze Source
                    Box(modifier = Modifier.fillMaxSize()) {
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(state = hazeState)
                        ) {
                            Image(
                                 painter = painterResource(id = R.drawable.background_image),
                                 contentDescription = null,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier.fillMaxSize()
                            )
                            
                            // Dimming Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF1E2939).copy(alpha = 0.7f),
                                                Color(0xFF364153).copy(alpha = 0.5f),
                                                Color(0xFF0F172B).copy(alpha = 0.8f) 
                                            )
                                        )
                                    )
                            )
                        }

                        NavHost(navController = navController, startDestination = "home") {
                            composable(
                                "home",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                PrayerScreen(
                                    hazeState = hazeState,
                                    onSettingsClick = { navController.navigate("settings") },
                                    onCalendarClick = { navController.navigate("calendar") },
                                    onClockClick = { navController.navigate("alarms") }
                                )
                            }
                            

                            composable(
                                "alarms",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                PrayerAlarmsScreen(
                                    hazeState = hazeState,
                                    viewModel = prayerAlarmViewModel,
                                    onBack = { navController.popBackStack() },
                                    onAddClick = { navController.navigate("alarm_add") },
                                    onAlarmClick = { alarmId -> navController.navigate("alarm_edit/$alarmId") }
                                )
                            }

                            composable(
                                "alarm_add",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                PrayerAlarmAddScreen(
                                    hazeState = hazeState,
                                    viewModel = prayerAlarmViewModel,
                                    onBack = { navController.popBackStack() },
                                    onAlarmAdded = { navController.popBackStack() }
                                )
                            }

                            composable(
                                "alarm_edit/{alarmId}",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) { backStackEntry ->
                                val alarmId = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull() ?: -1
                                PrayerAlarmEditScreen(
                                    alarmId = alarmId,
                                    hazeState = hazeState,
                                    viewModel = prayerAlarmViewModel,
                                    onBack = { navController.popBackStack() },
                                    onAlarmUpdated = { navController.popBackStack() }
                                )
                            }

                            composable(
                                "calendar",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                HijriCalendarScreen(
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                "settings",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                SettingsScreen(
                                    hazeState = hazeState,
                                    viewModel = settingsViewModel, // We need to ensure this instance is shared or valid
                                    onBack = { navController.popBackStack() },
                                    onLanguageClick = { navController.navigate("languages") },
                                    onLocationClick = { navController.navigate("location") },
                                    onCalculationMethodClick = { navController.navigate("calculation_method") }
                                )
                            }
                            
                            composable(
                                "languages",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                LanguageSelectionScreen(
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                "location",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                LocationScreen(
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                "calculation_method",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                CalculationMethodScreen(
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() },
                                    onTimeOffsetClick = { navController.navigate("time_offset") }
                                )
                            }

                            composable(
                                "time_offset",
                                enterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = tween(260),
                                        targetOffsetX = { fullWidth -> fullWidth }
                                    )
                                }
                            ) {
                                TimeOffsetScreen(
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
