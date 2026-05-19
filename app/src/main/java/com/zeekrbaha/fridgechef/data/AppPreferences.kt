package com.zeekrbaha.fridgechef.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("fridgechef", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<ThemePreference> = _theme

    fun setTheme(theme: ThemePreference) {
        prefs.edit().putInt(THEME_KEY, theme.ordinal).apply()
        _theme.value = theme
    }

    fun readDailyPicks(): DailyPicks? {
        val raw = prefs.getString(DAILY_PICKS_KEY, null) ?: return null
        return runCatching { json.decodeFromString<DailyPicks>(raw) }.getOrNull()
    }

    fun writeDailyPicks(picks: DailyPicks) {
        prefs.edit().putString(DAILY_PICKS_KEY, json.encodeToString(DailyPicks.serializer(), picks)).apply()
    }

    fun dailyPicksAreFromToday(picks: DailyPicks): Boolean {
        val zone = ZoneId.systemDefault()
        val savedDate = Instant.ofEpochMilli(picks.savedAtEpochMillis).atZone(zone).toLocalDate()
        return savedDate == LocalDate.now(zone)
    }

    private fun loadTheme(): ThemePreference {
        val raw = prefs.getInt(THEME_KEY, ThemePreference.System.ordinal)
        return ThemePreference.entries.getOrElse(raw) { ThemePreference.System }
    }

    private companion object {
        const val DAILY_PICKS_KEY = "DailyPicksService.cache"
        const val THEME_KEY = "ThemeManager.themePreference"
    }
}
