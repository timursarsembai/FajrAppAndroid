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
import androidx.compose.material3.Icon
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
import com.example.fajrapp.viewmodel.PrayerOffsetOption
import com.example.fajrapp.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun TimeOffsetScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
                        contentDescription = stringResource(R.string.settings_time_offset),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.settings_time_offset),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-24).dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = stringResource(R.string.time_offset_hint),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        viewModel.prayerOffsetOptions.forEach { option ->
            TimeOffsetItem(
                option = option,
                value = uiState.timeOffsets[option.key] ?: 0,
                hazeState = hazeState,
                onMinusClick = { viewModel.adjustTimeOffset(option.key, -1) },
                onPlusClick = { viewModel.adjustTimeOffset(option.key, 1) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TimeOffsetItem(
    option: PrayerOffsetOption,
    value: Int,
    hazeState: HazeState,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 18.dp,
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            OffsetButton(
                label = "-",
                hazeState = hazeState,
                onClick = onMinusClick
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.time_offset_format, value),
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.width(72.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OffsetButton(
                label = "+",
                hazeState = hazeState,
                onClick = onPlusClick
            )
        }
    }
}

@Composable
private fun OffsetButton(
    label: String,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 12.dp,
        hazeState = hazeState,
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
