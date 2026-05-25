package com.example.fajrapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fajrapp.R
import com.example.fajrapp.model.PrayerData
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.PrayerViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel = viewModel(),
    hazeState: HazeState,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onClockClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val screenHeight = maxHeight
        val referenceHeight = 840.dp
        val scale = (screenHeight / referenceHeight).coerceIn(0.5f, 1.2f)

        val horizontalPadding = 28.dp * scale
        val verticalPadding = 16.dp * scale
        val smallPadding = 8.dp * scale
        val timerFontSize = (53.3f * scale).sp
        val dateBigFontSize = (14 * scale).sp
        val dateSmallFontSize = (12 * scale).sp
        val prayerNameFontSize = (22 * scale).sp
        val prayerTimeFontSize = (26 * scale).sp

        val settingsIconSize = 24.dp * scale
        val locationIconSize = 16.dp * scale
        val settingsBoxSize = 48.dp * scale

        val boxCornerRadius = 14.dp * scale
        val timerCornerRadius = 24.dp * scale

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            modifier = Modifier
                                .clickable { onCalendarClick() }
                                .padding(horizontal = 13.dp * scale, vertical = 8.dp * scale)
                        ) {
                            Text(
                                text = uiState.hijriDate,
                                color = Color.White,
                                fontSize = dateBigFontSize,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.gregorianDate,
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onSettingsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(settingsIconSize)
                            )
                        }
                    }
                }

                GlassContainer(
                    modifier = Modifier
                        .padding(bottom = 24.dp * scale)
                        .clickable { onClockClick() },
                    cornerRadius = timerCornerRadius,
                    hazeState = hazeState,
                    blurEnabled = false
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp * scale, vertical = 14.dp * scale),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LiveClockText(fontSize = timerFontSize)
                        Spacer(modifier = Modifier.height(6.dp * scale))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(locationIconSize)
                            )
                            Spacer(modifier = Modifier.width(8.dp * scale))
                            Text(
                                text = uiState.locationName,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = dateBigFontSize
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp * scale)
            ) {
                uiState.prayerTimes.forEach { prayer ->
                    val isNext = prayer.isNext
                    val itemScale = if (isNext) scale * 1.15f else scale
                    val titleSize = if (isNext) prayerNameFontSize * 1.15f else prayerNameFontSize
                    val timeSize = if (isNext) prayerTimeFontSize * 1.15f else prayerTimeFontSize

                    val isExpanded = expandedStates[prayer.key] ?: false
                    PrayerItem(
                        prayer = prayer,
                        titleSize = titleSize,
                        timeSize = timeSize,
                        scale = itemScale,
                        hazeState = hazeState,
                        isExpanded = isExpanded,
                        onToggleExpand = { expandedStates[prayer.key] = !isExpanded },
                        timerPrefix = stringResource(R.string.timer_prefix),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }
    }
}

@Composable
private fun PrayerItem(
    prayer: PrayerData,
    titleSize: TextUnit,
    timeSize: TextUnit,
    scale: Float,
    hazeState: HazeState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    timerPrefix: String,
    modifier: Modifier = Modifier
) {
    val internalPadding = 16.dp * scale
    val cornerRadius = 24.dp * scale
    val highlightColor = Color(0xFFFFE7A3)
    val iconBoxSize = 24.dp * scale
    val iconSize = 16.dp * scale
    val checkMarkSpacer = 12.dp * scale
    val timeLeftSize = (14 * scale).sp
    val arabicSize = (16 * scale).sp

    GlassContainer(
        modifier = modifier.then(
            if (prayer.isNext) {
                Modifier.border(
                    width = 1.5.dp,
                    color = Color(0xFFFFE7A3),
                    shape = RoundedCornerShape(cornerRadius)
                )
            } else {
                Modifier
            }
        ),
        cornerRadius = cornerRadius,
        hazeState = hazeState,
        blurEnabled = false
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(internalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prayer.isPassed) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clickable(enabled = false) {}
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconBoxSize)
                                .background(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(checkMarkSpacer))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prayer.name,
                        color = if (prayer.isNext) highlightColor else Color.White,
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
                        color = if (prayer.isNext) highlightColor else Color.White,
                        fontSize = timeSize,
                        fontWeight = FontWeight.Bold
                    )
                    if (prayer.isNext) {
                        NextPrayerCountdownText(
                            targetTimeMillis = prayer.timeMillis,
                            timerPrefix = timerPrefix,
                            fontSize = timeLeftSize
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp * scale))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = internalPadding, end = internalPadding, bottom = internalPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp * scale)
                ) {
                    LessonButton(
                        text = stringResource(R.string.lesson_wudu),
                        hazeState = hazeState,
                        scale = scale
                    )
                    LessonButton(
                        text = stringResource(R.string.lesson_prayer_format, prayer.name),
                        hazeState = hazeState,
                        scale = scale
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClockText(fontSize: TextUnit) {
    val value by produceState(initialValue = "00:00:00") {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            value = formatter.format(Date())
            delay(1000L)
        }
    }
    Text(
        text = value,
        color = Color.White,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun NextPrayerCountdownText(
    targetTimeMillis: Long,
    timerPrefix: String,
    fontSize: TextUnit
) {
    val value by produceState(initialValue = "$timerPrefix --:--:--", key1 = targetTimeMillis, key2 = timerPrefix) {
        while (true) {
            var diffMillis = targetTimeMillis - System.currentTimeMillis()
            if (diffMillis < 0L) diffMillis = 0L

            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            diffMillis -= TimeUnit.HOURS.toMillis(hours)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            diffMillis -= TimeUnit.MINUTES.toMillis(minutes)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)

            value = String.format(
                Locale.getDefault(),
                "$timerPrefix %02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
            delay(1000L)
        }
    }
    Text(
        text = value,
        color = Color.White.copy(alpha = 0.8f),
        fontSize = fontSize
    )
}

@Composable
private fun LessonButton(
    text: String,
    hazeState: HazeState,
    scale: Float
) {
    GlassContainer(
        cornerRadius = 14.dp * scale,
        hazeState = hazeState,
        blurEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp * scale)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold,
                fontSize = (14 * scale).sp
            )
        }
    }
}
