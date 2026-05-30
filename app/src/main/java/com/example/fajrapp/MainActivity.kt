package com.example.fajrapp

import android.Manifest
import android.content.Context
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
import com.example.fajrapp.ui.NotificationSoundPickerScreen
import com.example.fajrapp.ui.NotificationsSettingsScreen
import com.example.fajrapp.ui.NOTIFICATION_SOUND_TARGET_GLOBAL
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fajrapp.ui.PrayerScreen
import com.example.fajrapp.ui.SettingsScreen
import com.example.fajrapp.ui.theme.FajrAppTheme
import com.example.fajrapp.viewmodel.Language
import com.example.fajrapp.viewmodel.PrayerAlarmViewModel
import com.example.fajrapp.viewmodel.PrayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var prayerViewModelRef: PrayerViewModel? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val hasLocationPermission = grantResults[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grantResults[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            prayerViewModelRef?.refreshLocationFromDevice()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val langCode = FajrApp.ensureAppLanguagePreference(newBase)
        super.attachBaseContext(FajrApp.updateBaseContextLocale(newBase, langCode))
    }

    private fun onLanguageSelected(
        language: Language,
        settingsViewModel: SettingsViewModel,
        prayerViewModel: PrayerViewModel
    ) {
        val selectedCode = normalizeLanguageCode(language.code)
        val currentCode = normalizeLanguageCode(settingsViewModel.uiState.value.selectedLanguage.code)
        if (currentCode == selectedCode) return

        settingsViewModel.setLanguage(language)
        prayerViewModel.refreshForLocaleChange()
        recreate()
    }

    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase(Locale.US)) {
            "kz" -> "kk"
            "id" -> "in"
            else -> code.lowercase(Locale.US)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!isRunningInstrumentationTest()) {
            requestPermissionLauncher.launch(
                buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
            )
        }

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
                    val prayerViewModel: PrayerViewModel = viewModel()
                    prayerViewModelRef = prayerViewModel
                    val settingsUiState by settingsViewModel.uiState.collectAsState()
                    val appLayoutDirection = when (normalizeLanguageCode(settingsUiState.selectedLanguage.code)) {
                        "ar", "fa", "ur" -> LayoutDirection.Rtl
                        else -> LayoutDirection.Ltr
                    }

                    // Shared Background with Haze Source
                    Box(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
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
                                    viewModel = prayerViewModel,
                                    appLanguageCode = normalizeLanguageCode(settingsUiState.selectedLanguage.code),
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
                                    onCalculationMethodClick = { navController.navigate("calculation_method") },
                                    onNotificationsClick = { navController.navigate("notifications") }
                                )
                            }

                            composable(
                                "notifications",
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
                                NotificationsSettingsScreen(
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() },
                                    onGlobalSoundClick = {
                                        navController.navigate("notifications_sound/$NOTIFICATION_SOUND_TARGET_GLOBAL")
                                    },
                                    onPrayerSoundClick = { prayerKey ->
                                        navController.navigate("notifications_sound/$prayerKey")
                                    }
                                )
                            }

                            composable(
                                "notifications_sound/{targetKey}",
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
                                val targetKey = backStackEntry.arguments?.getString("targetKey")
                                    ?: NOTIFICATION_SOUND_TARGET_GLOBAL
                                NotificationSoundPickerScreen(
                                    targetKey = targetKey,
                                    viewModel = settingsViewModel,
                                    hazeState = hazeState,
                                    onBack = { navController.popBackStack() },
                                    onSelected = { navController.popBackStack() }
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
                                    onBack = { navController.popBackStack() },
                                    onLanguageSelected = { language ->
                                        onLanguageSelected(language, settingsViewModel, prayerViewModel)
                                    }
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

    private fun isRunningInstrumentationTest(): Boolean {
        return runCatching {
            val registryClass = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            val getInstrumentation = registryClass.getMethod("getInstrumentation")
            getInstrumentation.invoke(null)
            true
        }.getOrDefault(false)
    }
}
