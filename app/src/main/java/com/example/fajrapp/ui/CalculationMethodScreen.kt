package com.example.fajrapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajrapp.R
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun CalculationMethodScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var methodExpanded by remember { mutableStateOf(false) }
    var madhabExpanded by remember { mutableStateOf(false) }
    var dstExpanded by remember { mutableStateOf(false) }

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
                        contentDescription = stringResource(R.string.settings_calculation_method),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.settings_calculation_method),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-24).dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = stringResource(R.string.calc_method_label),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassDropdownField(
            value = uiState.calculationMethodLabel,
            expanded = methodExpanded,
            onExpandedChange = { methodExpanded = it },
            hazeState = hazeState
        ) {
            viewModel.calculationMethodOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        methodExpanded = false
                        viewModel.setCalculationMethod(option.code)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.size(18.dp))

        Text(
            text = stringResource(R.string.calc_asr_label),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassDropdownField(
            value = uiState.madhabLabel,
            expanded = madhabExpanded,
            onExpandedChange = { madhabExpanded = it },
            hazeState = hazeState
        ) {
            viewModel.madhabOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        madhabExpanded = false
                        viewModel.setMadhab(option.code)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.size(18.dp))

        Text(
            text = stringResource(R.string.calc_dst_label),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassDropdownField(
            value = uiState.dstModeLabel,
            expanded = dstExpanded,
            onExpandedChange = { dstExpanded = it },
            hazeState = hazeState
        ) {
            viewModel.dstModeOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        dstExpanded = false
                        viewModel.setDstMode(option.code)
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassDropdownField(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        GlassContainer(
            cornerRadius = 14.dp,
            hazeState = hazeState,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            menuContent()
        }
    }
}
