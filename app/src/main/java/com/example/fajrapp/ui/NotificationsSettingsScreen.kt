package com.example.fajrapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajrapp.R
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.NotificationSoundOption
import com.example.fajrapp.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState

const val NOTIFICATION_SOUND_TARGET_GLOBAL = "global"

@Composable
fun NotificationsSettingsScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit,
    onGlobalSoundClick: () -> Unit,
    onPrayerSoundClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassContainer(
                cornerRadius = 14.dp,
                hazeState = hazeState,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.settings_notifications),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.settings_notifications),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-24).dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassContainer(
                cornerRadius = 16.dp,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.notification_single_sound),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.notificationsUseSingleSound,
                        onCheckedChange = { enabled ->
                            viewModel.setNotificationsUseSingleSound(enabled)
                        }
                    )
                }
            }

            if (uiState.notificationsUseSingleSound) {
                NotificationSoundRow(
                    title = stringResource(R.string.notification_sound_label),
                    subtitle = uiState.notificationsGlobalSoundTitle,
                    hazeState = hazeState,
                    onClick = onGlobalSoundClick
                )
            } else {
                uiState.notificationsByPrayer.forEach { prayerOption ->
                    GlassContainer(
                        cornerRadius = 16.dp,
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = prayerOption.prayerLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.notification_enable_label),
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = prayerOption.enabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setPrayerNotificationEnabled(prayerOption.prayerKey, enabled)
                                    }
                                )
                            }

                            NotificationSoundRow(
                                title = stringResource(R.string.notification_sound_label),
                                subtitle = prayerOption.soundTitle,
                                hazeState = hazeState,
                                onClick = { onPrayerSoundClick(prayerOption.prayerKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSoundPickerScreen(
    targetKey: String,
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit,
    onSelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val options = viewModel.notificationSoundOptions
    val selectedUri = if (targetKey == NOTIFICATION_SOUND_TARGET_GLOBAL) {
        uiState.notificationsGlobalSoundUri
    } else {
        uiState.notificationsByPrayer.firstOrNull { it.prayerKey == targetKey }?.soundUri
    }

    val title = if (targetKey == NOTIFICATION_SOUND_TARGET_GLOBAL) {
        stringResource(R.string.notification_sound_label)
    } else {
        val prayerLabel = uiState.notificationsByPrayer.firstOrNull { it.prayerKey == targetKey }?.prayerLabel
            ?: targetKey
        "${stringResource(R.string.notification_sound_label)} - $prayerLabel"
    }

    val grouped = options.groupBy { it.sourceKey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassContainer(
                cornerRadius = 14.dp,
                hazeState = hazeState,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = title,
                        tint = Color.White
                    )
                }
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            grouped.forEach { (sourceKey, sourceOptions) ->
                val sourceTitle = if (sourceKey == SettingsViewModel.SOUND_SOURCE_AZAN) {
                    stringResource(R.string.notification_sounds_azan)
                } else {
                    stringResource(R.string.notification_sounds_system)
                }

                Text(
                    text = sourceTitle,
                    color = Color(0xFFFFE7A3),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                sourceOptions.forEach { option ->
                    NotificationSoundOptionItem(
                        option = option,
                        selected = option.uri == selectedUri,
                        hazeState = hazeState,
                        onClick = {
                            if (targetKey == NOTIFICATION_SOUND_TARGET_GLOBAL) {
                                viewModel.setGlobalNotificationSound(option)
                            } else {
                                viewModel.setPrayerNotificationSound(targetKey, option)
                            }
                            onSelected()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSoundRow(
    title: String,
    subtitle: String,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 14.dp,
        hazeState = hazeState,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NotificationSoundOptionItem(
    option: NotificationSoundOption,
    selected: Boolean,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 14.dp,
        hazeState = hazeState,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.title,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFFFE7A3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
