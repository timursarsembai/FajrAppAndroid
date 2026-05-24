package com.example.fajrapp.ui
import com.example.fajrapp.ui.components.GlassContainer

import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fajrapp.model.PrayerData
import com.example.fajrapp.viewmodel.PrayerViewModel
import androidx.compose.ui.res.stringResource
import com.example.fajrapp.R

// Placeholder color for background since we don't have the image asset
val BackgroundColor = Color(0xFF1E2939)

@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel = viewModel(),
    hazeState: HazeState,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 2. Safe Area & Layout Container
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
            val screenHeight = maxHeight
            
            // Reference design height (e.g., standard generic phone ~800-850dp)
            val referenceHeight = 840.dp
            
            // Calculate scale factor: current height / reference height
            val scale = (screenHeight / referenceHeight).coerceIn(0.5f, 1.2f)

            // Dynamic Dimensional Helpers
            val horizontalPadding = 28.dp * scale
            val verticalPadding = 16.dp * scale
            val smallPadding = 8.dp * scale
            val spacerHeight = 16.dp * scale
            
            // Font Sizes (Scaled)
            val timerFontSize = (40 * scale).sp
            val dateBigFontSize = (14 * scale).sp
            val dateSmallFontSize = (12 * scale).sp
            val prayerNameFontSize = (22 * scale).sp
            val prayerTimeFontSize = (26 * scale).sp
            val footerFontSize = (12 * scale).sp
            
            // Icon Sizes
            val settingsIconSize = 24.dp * scale
            val locationIconSize = 16.dp * scale
            val settingsBoxSize = 48.dp * scale
            
            // Corner Radius
            val boxCornerRadius = 14.dp * scale
            val timerCornerRadius = 24.dp * scale

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- TOP SECTION: Header & Timer ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header (Date & Settings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp * scale),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassContainer(
                            modifier = Modifier.padding(end = smallPadding),
                            cornerRadius = boxCornerRadius,
                            hazeState = hazeState
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 13.dp * scale, vertical = 8.dp * scale)
                            ) {
                                Text(
                                    text = uiState.hijriDate, // FROM VIEWMODEL
                                    color = Color.White,
                                    fontSize = dateBigFontSize,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.gregorianDate, // FROM VIEWMODEL
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = dateSmallFontSize
                                )
                            }
                        }
                        
                        GlassContainer(
                            cornerRadius = boxCornerRadius,
                            modifier = Modifier.size(settingsBoxSize),
                            hazeState = hazeState
                        ) {
                            Box(modifier = Modifier.fillMaxSize().clickable { onSettingsClick() }, contentAlignment = Alignment.Center) {
                                 Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(settingsIconSize)
                                )
                            }
                        }
                    }

                    // Big Timer (Now Clock)
                    GlassContainer(
                        modifier = Modifier.padding(bottom = 24.dp * scale),
                        cornerRadius = timerCornerRadius,
                        hazeState = hazeState
                    ) {
                        Text(
                            text = uiState.currentTimeFormatted, // REAL CLOCK
                            color = Color.White,
                            fontSize = timerFontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 32.dp * scale, vertical = 16.dp * scale)
                        )
                    }
                }

                // --- MIDDLE SECTION: Prayer List ---
                val prayers = uiState.prayerTimes

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    prayers.forEach { prayer ->
                        val isNext = prayer.isNext
                        val itemScale = if (isNext) scale * 1.15f else scale // Larger (15%)
                        val titleSize = if (isNext) prayerNameFontSize * 1.15f else prayerNameFontSize
                        val timeSize = if (isNext) prayerTimeFontSize * 1.15f else prayerTimeFontSize

                        PrayerItem(
                            prayer = prayer,
                            titleSize = titleSize,
                            timeSize = timeSize,
                            scale = itemScale,
                            hazeState = hazeState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (isNext) 1.15f else 1f)
                        )
                    }
                }

                // --- BOTTOM SECTION: Location & Dua ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = spacerHeight)
                ) {
                     GlassContainer(
                        cornerRadius = timerCornerRadius,
                        modifier = Modifier.padding(bottom = 8.dp * scale),
                        hazeState = hazeState
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp * scale, vertical = 8.dp * scale),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(locationIconSize)
                            )
                            Spacer(modifier = Modifier.width(8.dp * scale))
                            Text(
                                text = uiState.locationName, // FROM VIEWMODEL
                                color = Color.White,
                                fontSize = dateBigFontSize
                            )
                        }
                    }
                    


                    Text(
                        text = stringResource(R.string.footer_dua),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = footerFontSize,
                        modifier = Modifier.padding(bottom = smallPadding)
                    )
                }
            }
        }
}

@Composable
fun PrayerItem(
    prayer: PrayerData, 
    titleSize: androidx.compose.ui.unit.TextUnit, 
    timeSize: androidx.compose.ui.unit.TextUnit,
    scale: Float, // Pass scale to adjust internal padding/icon sizes
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val verticalPadding = 2.dp * scale // Reduced vertical margin between items if needed, or controlled by parent Arrangement
    val internalPadding = 16.dp * scale
    val cornerRadius = 24.dp * scale
    val iconBoxSize = 24.dp * scale
    val iconSize = 16.dp * scale
    val checkMarkSpacer = 12.dp * scale
    val timeLeftSize = (14 * scale).sp
    val arabicSize = (16 * scale).sp

    GlassContainer(
        modifier = modifier
            // We don't need vertical padding here if parent uses SpaceEvenly, 
            // but a tiny bit ensures they don't touch if crowded.
            .padding(vertical = verticalPadding), 
        cornerRadius = cornerRadius,
        hazeState = hazeState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(internalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
             // Checkmark for all passed prayers
             if (prayer.isPassed) {
                 Box(
                     modifier = Modifier
                         .size(iconBoxSize)
                         .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                         .background(Color.White.copy(alpha = 0.1f), CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         imageVector = Icons.Default.Check,
                         contentDescription = null,
                         tint = Color.White,
                         modifier = Modifier.size(iconSize)
                     )
                 }
                 Spacer(modifier = Modifier.width(checkMarkSpacer))
             }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.name,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = prayer.arabicName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = arabicSize
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = prayer.time,
                    color = Color.White,
                    fontSize = timeSize,
                    fontWeight = FontWeight.Bold
                )
                // Countdown text for next prayer
                if (prayer.timeLeft != null) {
                    Text(
                        text = prayer.timeLeft,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = timeLeftSize
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun PrayerScreenPreview() {
    // Mock HazeState for preview
    val hazeState = remember { HazeState() }
    PrayerScreen(hazeState = hazeState, onSettingsClick = {})
}
