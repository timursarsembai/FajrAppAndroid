package com.example.fajrapp.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajrapp.R
import com.example.fajrapp.ui.components.GlassContainer
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape

private const val MONTH_RANGE = 120

data class CalendarDayCell(
    val hijriDay: Int?,
    val gregorianDay: Int?,
    val isToday: Boolean
)

data class CalendarMonthData(
    val hijriYear: Int,
    val hijriMonth: Int,
    val gregorianMonthLabel: String,
    val gregorianYearLabel: String,
    val cells: List<CalendarDayCell>
)

@Composable
fun HijriCalendarScreen(
    hazeState: HazeState,
    onBack: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        CalendarUnavailableScreen(hazeState = hazeState, onBack = onBack)
        return
    }

    val locale = Locale.getDefault()
    val monthOffsets = remember { (-MONTH_RANGE..MONTH_RANGE).toList() }
    val zeroIndex = remember { monthOffsets.indexOf(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = zeroIndex)

    val baseMonthStart = remember {
        HijrahDate.now().with(ChronoField.DAY_OF_MONTH, 1)
    }

    val visibleOffset by remember {
        derivedStateOf { monthOffsets.getOrElse(listState.firstVisibleItemIndex) { 0 } }
    }

    val visibleMonthData = remember(baseMonthStart, visibleOffset, locale) {
        buildMonthData(
            monthStart = baseMonthStart.plus(visibleOffset.toLong(), ChronoUnit.MONTHS),
            locale = locale
        )
    }

    val weekDays = listOf(
        stringResource(R.string.calendar_week_mon),
        stringResource(R.string.calendar_week_tue),
        stringResource(R.string.calendar_week_wed),
        stringResource(R.string.calendar_week_thu),
        stringResource(R.string.calendar_week_fri),
        stringResource(R.string.calendar_week_sat),
        stringResource(R.string.calendar_week_sun)
    )

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
                .padding(bottom = 16.dp),
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
                        contentDescription = stringResource(R.string.calendar_title),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GlassContainer(
                cornerRadius = 14.dp,
                hazeState = hazeState
            ) {
                Text(
                    text = stringResource(
                        R.string.calendar_year_format,
                        visibleMonthData.hijriYear,
                        visibleMonthData.gregorianYearLabel
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(monthOffsets) { _, offset ->
                val monthData = remember(baseMonthStart, offset, locale) {
                    buildMonthData(
                        monthStart = baseMonthStart.plus(offset.toLong(), ChronoUnit.MONTHS),
                        locale = locale
                    )
                }
                MonthCalendarCard(
                    monthData = monthData,
                    weekDays = weekDays,
                    hazeState = hazeState,
                    isCurrentMonth = offset == 0
                )
            }
        }
    }
}

@Composable
private fun CalendarUnavailableScreen(
    hazeState: HazeState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
                    contentDescription = stringResource(R.string.calendar_title),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.calendar_unavailable),
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun MonthCalendarCard(
    monthData: CalendarMonthData,
    weekDays: List<String>,
    hazeState: HazeState,
    isCurrentMonth: Boolean
) {
    GlassContainer(
        cornerRadius = 20.dp,
        hazeState = hazeState,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentMonth) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = Color(0xFFFFE7A3),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            if (isCurrentMonth) {
                Text(
                    text = stringResource(R.string.calendar_current_month_badge),
                    color = Color(0xFFFFE7A3),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = getHijriMonthName(monthData.hijriMonth),
                color = if (isCurrentMonth) Color(0xFFFFF3C4) else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(${monthData.gregorianMonthLabel})",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            monthData.cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        CalendarDayCellView(cell = cell, hazeState = hazeState)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCellView(
    cell: CalendarDayCell,
    hazeState: HazeState
) {
    val textColor = if (cell.isToday) Color(0xFFFFF3A0) else Color.White

    GlassContainer(
        cornerRadius = 10.dp,
        hazeState = hazeState,
        modifier = Modifier
            .weight(1f)
            .padding(2.dp)
            .height(54.dp)
    ) {
        if (cell.hijriDay == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cell.hijriDay.toString(),
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Text(
                    text = cell.gregorianDay?.toString().orEmpty(),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

private fun buildMonthData(
    monthStart: HijrahDate,
    locale: Locale
): CalendarMonthData {
    val monthLength = monthStart.lengthOfMonth()
    val firstWeekDay = LocalDate.from(monthStart).dayOfWeek.value - 1 // Monday = 0

    val firstGregorian = LocalDate.from(monthStart)
    val lastGregorian = LocalDate.from(monthStart.with(ChronoField.DAY_OF_MONTH, monthLength.toLong()))
    val gregorianMonthLabel = buildGregorianMonthLabel(firstGregorian, lastGregorian, locale)
    val gregorianYearLabel = buildGregorianYearLabel(firstGregorian, lastGregorian)

    val todayHijri = HijrahDate.now()

    val cells = mutableListOf<CalendarDayCell>()
    repeat(firstWeekDay) {
        cells.add(CalendarDayCell(null, null, false))
    }

    for (day in 1..monthLength) {
        val hijriDate = monthStart.with(ChronoField.DAY_OF_MONTH, day.toLong())
        val gregDate = LocalDate.from(hijriDate)
        val isToday = hijriDate == todayHijri

        cells.add(
            CalendarDayCell(
                hijriDay = day,
                gregorianDay = gregDate.dayOfMonth,
                isToday = isToday
            )
        )
    }

    while (cells.size % 7 != 0) {
        cells.add(CalendarDayCell(null, null, false))
    }

    return CalendarMonthData(
        hijriYear = monthStart.get(ChronoField.YEAR),
        hijriMonth = monthStart.get(ChronoField.MONTH_OF_YEAR),
        gregorianMonthLabel = gregorianMonthLabel,
        gregorianYearLabel = gregorianYearLabel,
        cells = cells
    )
}

private fun buildGregorianMonthLabel(start: LocalDate, end: LocalDate, locale: Locale): String {
    val firstMonth = start.month.getDisplayName(TextStyle.FULL, locale)
    val secondMonth = end.month.getDisplayName(TextStyle.FULL, locale)
    return if (start.month == end.month && start.year == end.year) {
        capitalizeMonth(firstMonth, locale)
    } else {
        "${capitalizeMonth(firstMonth, locale)} - ${capitalizeMonth(secondMonth, locale)}"
    }
}

private fun buildGregorianYearLabel(start: LocalDate, end: LocalDate): String {
    return if (start.year == end.year) {
        start.year.toString()
    } else {
        "${start.year}-${end.year}"
    }
}

private fun capitalizeMonth(month: String, locale: Locale): String {
    return month.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@Composable
private fun getHijriMonthName(month: Int): String {
    return when (month) {
        1 -> stringResource(R.string.hijri_muharram)
        2 -> stringResource(R.string.hijri_safar)
        3 -> stringResource(R.string.hijri_rabi_al_awwal)
        4 -> stringResource(R.string.hijri_rabi_al_athani)
        5 -> stringResource(R.string.hijri_jumada_al_ula)
        6 -> stringResource(R.string.hijri_jumada_al_akhirah)
        7 -> stringResource(R.string.hijri_rajab)
        8 -> stringResource(R.string.hijri_shaban)
        9 -> stringResource(R.string.hijri_ramadan)
        10 -> stringResource(R.string.hijri_shawwal)
        11 -> stringResource(R.string.hijri_dhu_al_qadah)
        12 -> stringResource(R.string.hijri_dhu_al_hijjah)
        else -> ""
    }
}
