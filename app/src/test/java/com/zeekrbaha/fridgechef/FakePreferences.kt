package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.Preferences
import com.zeekrbaha.fridgechef.data.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory Preferences for JVM unit tests (no Android Context). */
class FakePreferences(initialTheme: ThemePreference = ThemePreference.System) : Preferences {
    private val _theme = MutableStateFlow(initialTheme)
    override val theme: StateFlow<ThemePreference> = _theme

    private var picks: DailyPicks? = null
    var picksFromToday = true

    override fun setTheme(theme: ThemePreference) { _theme.value = theme }
    override fun readDailyPicks(): DailyPicks? = picks
    override fun writeDailyPicks(picks: DailyPicks) { this.picks = picks }
    override fun dailyPicksAreFromToday(picks: DailyPicks): Boolean = picksFromToday
}
