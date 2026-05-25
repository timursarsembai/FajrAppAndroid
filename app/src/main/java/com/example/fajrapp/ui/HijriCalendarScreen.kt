package com.example.fajrapp.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape

enum class IslamicHolidayType(val hijriMonth: Int, val hijriDay: Int) {
    EID_AL_FITR(hijriMonth = 10, hijriDay = 1),
    EID_AL_ADHA(hijriMonth = 12, hijriDay = 10)
}

data class IslamicHolidayInfo(
    val type: IslamicHolidayType,
    val hijriYear: Int,
    val gregorianDate: LocalDate
)

data class CalendarDayCell(
    val hijriDay: Int?,
    val gregorianDay: Int?,
    val isToday: Boolean,
    val holidayType: IslamicHolidayType? = null
)

data class CalendarMonthData(
    val hijriYear: Int,
    val hijriMonth: Int,
    val gregorianMonthLabel: String,
    val gregorianYearLabel: String,
    val weeks: List<List<CalendarDayCell>>,
    val holidays: List<IslamicHolidayInfo>
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
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }

    val baseMonthStart = remember {
        HijrahDate.now().with(ChronoField.DAY_OF_MONTH, 1)
    }

    val visibleMonthData = remember(baseMonthStart, monthOffset, locale) {
        buildMonthData(
            monthStart = baseMonthStart.plus(monthOffset.toLong(), ChronoUnit.MONTHS),
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

            Text(
                text = stringResource(R.string.calendar_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        GlassContainer(
            cornerRadius = 14.dp,
            hazeState = hazeState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        MonthCalendarCard(
            monthData = visibleMonthData,
            weekDays = weekDays,
            hazeState = hazeState,
            isCurrentMonth = monthOffset == 0,
            onPreviousMonth = { monthOffset -= 1 },
            onNextMonth = { monthOffset += 1 },
            onResetToCurrent = { monthOffset = 0 }
        )
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
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrent: () -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getHijriMonthName(monthData.hijriMonth),
                        color = if (isCurrentMonth) Color(0xFFFFF3C4) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${getHijriMonthArabicName(monthData.hijriMonth)})",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = "(${monthData.gregorianMonthLabel})",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isCurrentMonth) {
                        MonthSwitchButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.calendar_reset_month),
                            hazeState = hazeState,
                            onClick = onResetToCurrent
                        )
                    }
                    MonthSwitchButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.calendar_prev_month),
                        hazeState = hazeState,
                        onClick = onPreviousMonth
                    )
                    MonthSwitchButton(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                        hazeState = hazeState,
                        onClick = onNextMonth
                    )
                }
            }

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

            monthData.weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        CalendarDayCellView(cell = cell)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HolidayInfoBlock(
                holidays = monthData.holidays,
                hazeState = hazeState
            )
        }
    }
}

@Composable
private fun MonthSwitchButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    GlassContainer(
        cornerRadius = 10.dp,
        hazeState = hazeState,
        modifier = Modifier.size(34.dp)
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
private fun RowScope.CalendarDayCellView(
    cell: CalendarDayCell
) {
    val isHoliday = cell.holidayType != null
    val hijriTextColor = when {
        isHoliday -> Color(0xFFFFE7A3)
        cell.isToday -> Color(0xFFFFF3A0)
        else -> Color.White
    }
    val gregorianTextColor = if (isHoliday) {
        Color(0xFFFFE7A3).copy(alpha = 0.9f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(2.dp)
            .aspectRatio(0.95f)
            .background(
                color = if (isHoliday) Color(0x22FFE7A3) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isHoliday) 1.4.dp else 1.dp,
                color = when {
                    isHoliday -> Color(0xCCFFE7A3)
                    cell.isToday -> Color(0x66FFE7A3)
                    else -> Color.White.copy(alpha = 0.22f)
                },
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        if (cell.hijriDay == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cell.hijriDay.toString(),
                    color = hijriTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp
                )
                Text(
                    text = cell.gregorianDay?.toString().orEmpty(),
                    color = gregorianTextColor,
                    fontSize = 10.sp,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HolidayInfoBlock(
    holidays: List<IslamicHolidayInfo>,
    hazeState: HazeState
) {
    val locale = Locale.getDefault()
    val gregorianFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }

    GlassContainer(
        cornerRadius = 14.dp,
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_holidays_title),
                color = Color(0xFFFFE7A3),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            holidays.forEach { holiday ->
                val title = when (holiday.type) {
                    IslamicHolidayType.EID_AL_FITR -> stringResource(R.string.calendar_holiday_eid_al_fitr)
                    IslamicHolidayType.EID_AL_ADHA -> stringResource(R.string.calendar_holiday_eid_al_adha)
                }
                val hijriMonthName = getHijriMonthName(holiday.type.hijriMonth)
                val gregorianDateText = holiday.gregorianDate.format(gregorianFormatter)

                Text(
                    text = stringResource(
                        R.string.calendar_holiday_line_format,
                        title,
                        holiday.type.hijriDay,
                        hijriMonthName,
                        holiday.hijriYear,
                        gregorianDateText
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun buildMonthData(
    monthStart: HijrahDate,
    locale: Locale
): CalendarMonthData {
    val hijriYear = monthStart.get(ChronoField.YEAR)
    val hijriMonth = monthStart.get(ChronoField.MONTH_OF_YEAR)
    val monthLength = monthStart.lengthOfMonth()
    val firstWeekDay = LocalDate.from(monthStart).dayOfWeek.value - 1 // Monday = 0
    val holidays = buildIslamicHolidaysForYear(hijriYear)
    val holidayByMonthAndDay = holidays.associateBy { it.type.hijriMonth to it.type.hijriDay }

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
        val holidayType = holidayByMonthAndDay[hijriMonth to day]?.type

        cells.add(
            CalendarDayCell(
                hijriDay = day,
                gregorianDay = gregDate.dayOfMonth,
                isToday = isToday,
                holidayType = holidayType
            )
        )
    }

    while (cells.size % 7 != 0) {
        cells.add(CalendarDayCell(null, null, false))
    }
    val weeks = cells.chunked(7)

    return CalendarMonthData(
        hijriYear = hijriYear,
        hijriMonth = hijriMonth,
        gregorianMonthLabel = gregorianMonthLabel,
        gregorianYearLabel = gregorianYearLabel,
        weeks = weeks,
        holidays = holidays
    )
}

private fun buildIslamicHolidaysForYear(hijriYear: Int): List<IslamicHolidayInfo> {
    return IslamicHolidayType.entries
        .mapNotNull { holidayType ->
            try {
                val hijriDate = HijrahDate.of(hijriYear, holidayType.hijriMonth, holidayType.hijriDay)
                IslamicHolidayInfo(
                    type = holidayType,
                    hijriYear = hijriYear,
                    gregorianDate = LocalDate.from(hijriDate)
                )
            } catch (_: DateTimeException) {
                null
            }
        }
        .sortedBy { it.gregorianDate }
}

private fun buildGregorianMonthLabel(start: LocalDate, end: LocalDate, locale: Locale): String {
    val firstMonth = getStandaloneMonthName(start.month, locale)
    val secondMonth = getStandaloneMonthName(end.month, locale)
    return if (start.month == end.month && start.year == end.year) {
        capitalizeMonth(firstMonth, locale)
    } else {
        "${capitalizeMonth(firstMonth, locale)} - ${capitalizeMonth(secondMonth, locale)}"
    }
}

private fun getStandaloneMonthName(month: java.time.Month, locale: Locale): String {
    val standalone = month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
    if (standalone.isNotBlank()) return standalone
    return month.getDisplayName(TextStyle.FULL, locale)
}

private fun getHijriMonthArabicName(month: Int): String {
    return when (month) {
        1 -> "مُحَرَّم"
        2 -> "صَفَر"
        3 -> "رَبِيع ٱلْأَوَّل"
        4 -> "رَبِيع ٱلثَّانِي"
        5 -> "جُمَادَىٰ ٱلْأُولَىٰ"
        6 -> "جُمَادَىٰ ٱلثَّانِيَة"
        7 -> "رَجَب"
        8 -> "شَعْبَان"
        9 -> "رَمَضَان"
        10 -> "شَوَّال"
        11 -> "ذُو ٱلْقَعْدَة"
        12 -> "ذُو ٱلْحِجَّة"
        else -> ""
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
