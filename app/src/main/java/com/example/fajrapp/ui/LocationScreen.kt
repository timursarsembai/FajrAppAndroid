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
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajrapp.R
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun LocationScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var cityQuery by rememberSaveable { mutableStateOf("") }
    var latitudeInput by rememberSaveable { mutableStateOf(uiState.locationLatitude) }
    var longitudeInput by rememberSaveable { mutableStateOf(uiState.locationLongitude) }
    var cityMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.locationLatitude, uiState.locationLongitude) {
        if (uiState.locationLatitude.isNotBlank()) latitudeInput = uiState.locationLatitude
        if (uiState.locationLongitude.isNotBlank()) longitudeInput = uiState.locationLongitude
    }

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
                        contentDescription = stringResource(R.string.settings_location),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.settings_location),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-24).dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        SettingsItem(
            icon = Icons.Default.LocationSearching,
            title = stringResource(R.string.location_use_current),
            subtitle = if (uiState.isUpdatingLocation) {
                stringResource(R.string.settings_location_updating)
            } else {
                uiState.locationSubtitle
            },
            hazeState = hazeState,
            onClick = {
                if (!uiState.isUpdatingLocation) {
                    viewModel.updateLocationFromDevice()
                }
            }
        )

        Spacer(modifier = Modifier.size(14.dp))

        Text(
            text = stringResource(R.string.location_city_title),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = cityQuery,
                onValueChange = {
                    cityQuery = it
                    viewModel.onCityQueryChanged(it)
                    cityMenuExpanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.location_city_input_label), color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                trailingIcon = {
                    if (uiState.isSearchingCities) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.8f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = locationTextFieldColors()
            )

            val showSuggestions = cityQuery.trim().length >= 2 && (uiState.citySuggestions.isNotEmpty() || uiState.isSearchingCities)
            if (cityMenuExpanded && showSuggestions) {
                GlassContainer(
                    cornerRadius = 14.dp,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.isSearchingCities) {
                            Text(
                                text = stringResource(R.string.location_searching),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            uiState.citySuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            cityQuery = suggestion.displayName
                                            cityMenuExpanded = false
                                            viewModel.selectCitySuggestion(suggestion)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = suggestion.displayName,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        GlassActionButton(
            text = stringResource(R.string.location_city_find_button),
            icon = Icons.Default.Search,
            hazeState = hazeState,
            enabled = !uiState.isUpdatingLocation,
            onClick = {
                cityMenuExpanded = false
                viewModel.updateLocationFromCity(cityQuery)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(18.dp))

        Text(
            text = stringResource(R.string.location_coordinates_title),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = latitudeInput,
                onValueChange = {
                    latitudeInput = it
                    viewModel.clearLocationMessage()
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(R.string.location_latitude_label), color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = locationTextFieldColors()
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = longitudeInput,
                onValueChange = {
                    longitudeInput = it
                    viewModel.clearLocationMessage()
                },
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(R.string.location_longitude_label), color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = locationTextFieldColors()
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        GlassActionButton(
            text = stringResource(R.string.location_coordinates_save_button),
            hazeState = hazeState,
            enabled = !uiState.isUpdatingLocation,
            onClick = { viewModel.updateLocationFromCoordinates(latitudeInput, longitudeInput) },
            modifier = Modifier.fillMaxWidth()
        )

        uiState.locationActionMessage?.let { message ->
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.location_hint),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

@Composable
private fun locationTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White.copy(alpha = 0.8f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
    focusedLabelColor = Color.White.copy(alpha = 0.8f),
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    cursorColor = Color.White
)

@Composable
private fun GlassActionButton(
    text: String,
    hazeState: HazeState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    GlassContainer(
        cornerRadius = 16.dp,
        hazeState = hazeState,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.6f)
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.clickable { onClick() } else Modifier
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
