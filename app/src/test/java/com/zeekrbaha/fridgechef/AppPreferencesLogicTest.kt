package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesLogicTest {
    @Test
    fun themePreferenceOrdinalsMatchSpec() {
        assertEquals(0, ThemePreference.System.ordinal)
        assertEquals(1, ThemePreference.Light.ordinal)
        assertEquals(2, ThemePreference.Dark.ordinal)
    }
}
