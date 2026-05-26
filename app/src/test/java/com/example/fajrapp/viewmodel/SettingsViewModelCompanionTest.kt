package com.example.fajrapp.viewmodel

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelCompanionTest {

    @Test
    fun `normalizeDstMode returns fallback for invalid value`() {
        val result = SettingsViewModel.normalizeDstMode("INVALID_MODE", SettingsViewModel.DST_MODE_PLUS_ONE_HOUR)
        assertEquals(SettingsViewModel.DST_MODE_PLUS_ONE_HOUR, result)
    }

    @Test
    fun `normalizeDstMode keeps known values`() {
        assertEquals(SettingsViewModel.DST_MODE_AUTO, SettingsViewModel.normalizeDstMode(SettingsViewModel.DST_MODE_AUTO))
        assertEquals(
            SettingsViewModel.DST_MODE_MINUS_ONE_HOUR,
            SettingsViewModel.normalizeDstMode(SettingsViewModel.DST_MODE_MINUS_ONE_HOUR)
        )
        assertEquals(
            SettingsViewModel.DST_MODE_PLUS_ONE_HOUR,
            SettingsViewModel.normalizeDstMode(SettingsViewModel.DST_MODE_PLUS_ONE_HOUR)
        )
    }

    @Test
    fun `toCalculationMethod maps known code and defaults unknown`() {
        assertEquals(CalculationMethod.NORTH_AMERICA, SettingsViewModel.toCalculationMethod("NORTH_AMERICA"))
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, SettingsViewModel.toCalculationMethod("UNKNOWN"))
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, SettingsViewModel.toCalculationMethod(null))
    }

    @Test
    fun `toMadhab maps shafi and defaults to hanafi`() {
        assertEquals(Madhab.SHAFI, SettingsViewModel.toMadhab("SHAFI"))
        assertEquals(Madhab.HANAFI, SettingsViewModel.toMadhab("HANAFI"))
        assertEquals(Madhab.HANAFI, SettingsViewModel.toMadhab("UNKNOWN"))
        assertEquals(Madhab.HANAFI, SettingsViewModel.toMadhab(null))
    }
}

