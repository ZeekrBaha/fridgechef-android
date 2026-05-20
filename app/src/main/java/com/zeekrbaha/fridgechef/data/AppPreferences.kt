package com.zeekrbaha.fridgechef.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface Preferences {
    val theme: StateFlow<ThemePreference>
    fun setTheme(theme: ThemePreference)
    fun readDailyPicks(): DailyPicks?
    fun writeDailyPicks(picks: DailyPicks)
    fun dailyPicksAreFromToday(picks: DailyPicks): Boolean
}

class AppPreferences(context: Context) : Preferences {
    private val prefs = context.getSharedPreferences("fridgechef", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _theme = MutableStateFlow(loadTheme())
    override val theme: StateFlow<ThemePreference> = _theme

    override fun setTheme(theme: ThemePreference) {
        prefs.edit().putInt(THEME_KEY, theme.ordinal).apply()
        _theme.value = theme
    }

    override fun readDailyPicks(): DailyPicks? {
        val raw = prefs.getString(DAILY_PICKS_KEY, null) ?: return null
        return runCatching { json.decodeFromString<DailyPicks>(raw) }.getOrNull()
    }

    override fun writeDailyPicks(picks: DailyPicks) {
        prefs.edit().putString(DAILY_PICKS_KEY, json.encodeToString(DailyPicks.serializer(), picks)).apply()
    }

    override fun dailyPicksAreFromToday(picks: DailyPicks): Boolean {
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
