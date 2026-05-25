package com.example.fajrapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fajrapp.R
import com.example.fajrapp.model.PrayerAlarm
import com.example.fajrapp.model.RepeatMode
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.PrayerAlarmOption
import com.example.fajrapp.viewmodel.PrayerAlarmViewModel
import com.example.fajrapp.viewmodel.RingtoneOption
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun PrayerAlarmsScreen(
    hazeState: HazeState,
    viewModel: PrayerAlarmViewModel = viewModel(),
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onAlarmClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val options = viewModel.prayerOptions
    val alarms = uiState.alarms
    val grouped = options.map { option ->
        option to alarms.filter { it.prayerKey == option.key }
    }.filter { it.second.isNotEmpty() }

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
            IconGlassButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.alarm_title),
                hazeState = hazeState,
                onClick = onBack
            )

            Text(
                text = stringResource(R.string.alarm_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconGlassButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.alarm_add_button),
                hazeState = hazeState,
                onClick = onAddClick
            )
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.alarm_empty),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                grouped.forEach { (option, itemsForPrayer) ->
                    item(key = "header_${option.key}") {
                        Text(
                            text = option.label,
                            color = Color(0xFFFFE7A3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(itemsForPrayer, key = { it.id }) { alarm ->
                        AlarmItem(
                            alarm = alarm,
                            options = options,
                            hazeState = hazeState,
                            onEnabledChange = { enabled -> viewModel.toggleAlarmEnabled(alarm.id, enabled) },
                            onDelete = { viewModel.deleteAlarm(alarm.id) },
                            onClick = { onAlarmClick(alarm.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerAlarmAddScreen(
    hazeState: HazeState,
    viewModel: PrayerAlarmViewModel = viewModel(),
    onBack: () -> Unit,
    onAlarmAdded: () -> Unit
) {
    AlarmFormScreen(
        title = stringResource(R.string.alarm_create_title),
        actionText = stringResource(R.string.alarm_add_button),
        hazeState = hazeState,
        viewModel = viewModel,
        initialAlarm = null,
        onBack = onBack,
        onSubmit = { prayerKey, offsetMinutes, repeatMode, repeatDays, ringtone ->
            viewModel.addAlarm(
                prayerKey = prayerKey,
                offsetMinutes = offsetMinutes,
                repeatMode = repeatMode,
                repeatDaysIso = repeatDays,
                ringtoneUri = ringtone.uri,
                ringtoneTitle = ringtone.title
            )
            onAlarmAdded()
        }
    )
}

@Composable
fun PrayerAlarmEditScreen(
    alarmId: Int,
    hazeState: HazeState,
    viewModel: PrayerAlarmViewModel = viewModel(),
    onBack: () -> Unit,
    onAlarmUpdated: () -> Unit
) {
    val alarm = viewModel.getAlarmById(alarmId)
    if (alarm == null) {
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
                IconGlassButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.alarm_edit_title),
                    hazeState = hazeState,
                    onClick = onBack
                )
                Text(
                    text = stringResource(R.string.alarm_edit_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            Text(
                text = stringResource(R.string.alarm_not_found),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        return
    }

    AlarmFormScreen(
        title = stringResource(R.string.alarm_edit_title),
        actionText = stringResource(R.string.alarm_save_button),
        hazeState = hazeState,
        viewModel = viewModel,
        initialAlarm = alarm,
        onBack = onBack,
        onSubmit = { prayerKey, offsetMinutes, repeatMode, repeatDays, ringtone ->
            viewModel.updateAlarm(
                alarmId = alarmId,
                prayerKey = prayerKey,
                offsetMinutes = offsetMinutes,
                repeatMode = repeatMode,
                repeatDaysIso = repeatDays,
                ringtoneUri = ringtone.uri,
                ringtoneTitle = ringtone.title
            )
            onAlarmUpdated()
        }
    )
}

@Composable
private fun AlarmFormScreen(
    title: String,
    actionText: String,
    hazeState: HazeState,
    viewModel: PrayerAlarmViewModel,
    initialAlarm: PrayerAlarm?,
    onBack: () -> Unit,
    onSubmit: (String, Int, String, Set<Int>, RingtoneOption) -> Unit
) {
    val prayerOptions = viewModel.prayerOptions
    val ringtoneOptions = viewModel.ringtoneOptions

    var selectedPrayer by remember(initialAlarm?.id) {
        mutableStateOf(initialAlarm?.prayerKey ?: (prayerOptions.firstOrNull()?.key ?: "fajr"))
    }
    var isBefore by remember(initialAlarm?.id) { mutableStateOf((initialAlarm?.offsetMinutes ?: -1) < 0) }
    var selectedMinutes by remember(initialAlarm?.id) {
        mutableIntStateOf(abs(initialAlarm?.offsetMinutes ?: 1).coerceIn(1, 720))
    }
    var selectedDays by remember(initialAlarm?.id) {
        mutableStateOf(
            if ((initialAlarm?.repeatMode ?: RepeatMode.WEEKLY) == RepeatMode.WEEKLY) {
                RepeatMode.ALL_WEEK_DAYS
            } else {
                initialAlarm?.repeatDaysIso ?: setOf(1)
            }
        )
    }
    var selectedRingtoneUri by remember(initialAlarm?.id) {
        mutableStateOf(initialAlarm?.ringtoneUri)
    }

    var prayerExpanded by remember { mutableStateOf(false) }
    var ringtoneExpanded by remember { mutableStateOf(false) }

    val selectedRingtone = ringtoneOptions.firstOrNull { it.uri == selectedRingtoneUri } ?: ringtoneOptions.first()

    val formScrollState = rememberScrollState()

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
            IconGlassButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = title,
                hazeState = hazeState,
                onClick = onBack
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
                .verticalScroll(formScrollState)
        ) {
            GlassContainer(
                cornerRadius = 20.dp,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrayerPickerField(
                        options = prayerOptions,
                        selectedKey = selectedPrayer,
                        expanded = prayerExpanded,
                        onExpandedChange = { prayerExpanded = it },
                        onSelect = { selectedPrayer = it },
                        hazeState = hazeState
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            text = stringResource(R.string.alarm_before),
                            selected = isBefore,
                            onClick = { isBefore = true },
                            hazeState = hazeState,
                            modifier = Modifier.weight(1f)
                        )
                        GlassChip(
                            text = stringResource(R.string.alarm_after),
                            selected = !isBefore,
                            onClick = { isBefore = false },
                            hazeState = hazeState,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = stringResource(R.string.alarm_minutes_label),
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    MinuteWheelPicker(
                        value = selectedMinutes,
                        onValueChange = { selectedMinutes = it }
                    )

                    Text(
                        text = stringResource(R.string.alarm_repeat_label),
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    RepeatSelector(
                        selectedDaysIso = selectedDays,
                        onSelectedDaysChange = { selectedDays = it },
                        hazeState = hazeState
                    )

                    Text(
                        text = stringResource(R.string.alarm_ringtone_label),
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    RingtonePickerField(
                        options = ringtoneOptions,
                        selected = selectedRingtone,
                        expanded = ringtoneExpanded,
                        onExpandedChange = { ringtoneExpanded = it },
                        onSelect = { option -> selectedRingtoneUri = option.uri },
                        hazeState = hazeState
                    )

                    GlassContainer(
                        cornerRadius = 14.dp,
                        hazeState = hazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val signedOffset = if (isBefore) -selectedMinutes else selectedMinutes
                                val days = selectedDays.ifEmpty { setOf(1) }
                                val mode = if (days.size == RepeatMode.ALL_WEEK_DAYS.size) {
                                    RepeatMode.WEEKLY
                                } else {
                                    RepeatMode.CUSTOM_DAYS
                                }
                                onSubmit(selectedPrayer, signedOffset, mode, days, selectedRingtone)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = actionText,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatSelector(
    selectedDaysIso: Set<Int>,
    onSelectedDaysChange: (Set<Int>) -> Unit,
    hazeState: HazeState
) {
    GlassChip(
        text = stringResource(R.string.alarm_repeat_weekly),
        selected = selectedDaysIso.size == RepeatMode.ALL_WEEK_DAYS.size,
        onClick = { onSelectedDaysChange(RepeatMode.ALL_WEEK_DAYS) },
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    val week = listOf(
        1 to stringResource(R.string.calendar_week_mon),
        2 to stringResource(R.string.calendar_week_tue),
        3 to stringResource(R.string.calendar_week_wed),
        4 to stringResource(R.string.calendar_week_thu),
        5 to stringResource(R.string.calendar_week_fri),
        6 to stringResource(R.string.calendar_week_sat),
        7 to stringResource(R.string.calendar_week_sun)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        week.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (iso, label) ->
                    val selected = iso in selectedDaysIso
                    GlassChip(
                        text = label,
                        selected = selected,
                        onClick = {
                            val updated = selectedDaysIso.toMutableSet()
                            if (selected) updated.remove(iso) else updated.add(iso)
                            onSelectedDaysChange(updated.ifEmpty { setOf(1) })
                        },
                        hazeState = hazeState,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MinuteWheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val minValue = 1
    val maxValue = 720
    val visibleItemsCount = 5
    val centerOffset = visibleItemsCount / 2
    val itemHeight = 52.dp
    val values = remember { (minValue..maxValue).toList() }
    val initialIndex = (value.coerceIn(minValue, maxValue) - minValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val activeMinute by remember {
        derivedStateOf {
            val visibleInfos = listState.layoutInfo.visibleItemsInfo
            if (visibleInfos.isEmpty()) {
                value.coerceIn(minValue, maxValue)
            } else {
                val viewportCenter =
                    (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                val centered = visibleInfos.minByOrNull { info ->
                    abs((info.offset + info.size / 2) - viewportCenter)
                }
                ((centered?.index ?: 0) + minValue).coerceIn(minValue, maxValue)
            }
        }
    }

    LaunchedEffect(value) {
        val clamped = value.coerceIn(minValue, maxValue)
        if (!listState.isScrollInProgress) {
            val centeredValue = listState.layoutInfo.visibleItemsInfo
                .minByOrNull { info ->
                    val viewportCenter =
                        (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                    abs((info.offset + info.size / 2) - viewportCenter)
                }
                ?.index
                ?.plus(minValue)

            if (centeredValue != clamped) {
                listState.scrollToItem((clamped - minValue).coerceIn(0, values.lastIndex))
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .map { visibleInfos ->
                if (visibleInfos.isEmpty()) return@map null
                val viewportCenter =
                    (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                val centered = visibleInfos.minByOrNull { info ->
                    abs((info.offset + info.size / 2) - viewportCenter)
                } ?: return@map null
                val raw = centered.index + minValue
                raw.coerceIn(minValue, maxValue)
            }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { picked ->
                if (picked != value) onValueChange(picked)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemsCount)
            .background(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * centerOffset),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values, key = { it }) { minute ->
                val isActive = minute == activeMinute
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = minute.toString(),
                        color = if (isActive) Color(0xFFFFE7A3) else Color.White,
                        fontSize = if (isActive) 38.sp else 34.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RingtonePickerField(
    options: List<RingtoneOption>,
    selected: RingtoneOption,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (RingtoneOption) -> Unit,
    hazeState: HazeState
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selected.title, color = Color.White, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun IconGlassButton(
    icon: ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 14.dp,
        hazeState = hazeState,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PrayerPickerField(
    options: List<PrayerAlarmOption>,
    selectedKey: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    hazeState: HazeState
) {
    val selected = options.firstOrNull { it.key == selectedKey } ?: options.first()
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selected.label, color = Color.White, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.key)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmItem(
    alarm: PrayerAlarm,
    options: List<PrayerAlarmOption>,
    hazeState: HazeState,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val prayerName = options.firstOrNull { it.key == alarm.prayerKey }?.label ?: alarm.prayerKey
    val phrase = if (alarm.offsetMinutes < 0) {
        stringResource(R.string.alarm_before_format, abs(alarm.offsetMinutes))
    } else {
        stringResource(R.string.alarm_after_format, alarm.offsetMinutes)
    }
    val repeatSummary = if (alarm.repeatMode == RepeatMode.CUSTOM_DAYS) {
        stringResource(R.string.alarm_repeat_custom_summary)
    } else {
        stringResource(R.string.alarm_repeat_weekly)
    }

    GlassContainer(
        cornerRadius = 16.dp,
        hazeState = hazeState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayerName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = phrase,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 12.sp
                )
                Text(
                    text = repeatSummary,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onEnabledChange
            )
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.alarm_delete),
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) Color(0xFFFFE7A3) else Color.White.copy(alpha = 0.25f)
    val textColor = if (selected) Color(0xFFFFE7A3) else Color.White

    GlassContainer(
        cornerRadius = 12.dp,
        hazeState = hazeState,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
