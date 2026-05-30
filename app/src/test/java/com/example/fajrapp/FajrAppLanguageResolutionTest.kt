package com.example.fajrapp

import org.junit.Assert.assertEquals
import org.junit.Test

class FajrAppLanguageResolutionTest {

    @Test
    fun `uses stored language when it is supported`() {
        val resolved = FajrApp.resolveLanguageForApp("ru", "en")
        assertEquals("ru", resolved)
    }

    @Test
    fun `normalizes aliases for stored language`() {
        assertEquals("kk", FajrApp.resolveLanguageForApp("kz", "en"))
        assertEquals("in", FajrApp.resolveLanguageForApp("id", "en"))
    }

    @Test
    fun `uses device language when stored value is missing`() {
        val resolved = FajrApp.resolveLanguageForApp(null, "ky")
        assertEquals("ky", resolved)
    }

    @Test
    fun `falls back to english when neither stored nor device language is supported`() {
        assertEquals("en", FajrApp.resolveLanguageForApp(null, "de"))
        assertEquals("en", FajrApp.resolveLanguageForApp("de", "ru"))
    }
}
