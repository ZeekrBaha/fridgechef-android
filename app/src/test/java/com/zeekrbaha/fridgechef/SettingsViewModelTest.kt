package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.ThemePreference
import com.zeekrbaha.fridgechef.network.BuildConfigAPIKeyProvider
import com.zeekrbaha.fridgechef.network.FakeOpenAIClient
import com.zeekrbaha.fridgechef.network.OpenAIChatClient
import com.zeekrbaha.fridgechef.viewmodel.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun deps(store: FakeRecipeStore = FakeRecipeStore()) =
        Dependencies(FakeOpenAIClient(), store, FakePreferences())

    @Test fun clearRecipes_callsOnDoneOnSuccess() = runTest {
        val store = FakeRecipeStore().apply { save(com.zeekrbaha.fridgechef.data.RecipeBatch(recipes = listOf(com.zeekrbaha.fridgechef.data.Recipe(title = "X", description = "", ingredients = listOf("a"), steps = listOf("s"), estimatedTime = "5m")))) }
        val vm = SettingsViewModel(deps(store))
        var doneCalled = false
        vm.clearRecipes(onDone = { doneCalled = true })
        advanceUntilIdle()
        assertTrue(doneCalled)
        assertTrue(store.loadBatches().isEmpty())
    }

    @Test fun clearRecipes_callsOnErrorOnFailure() = runTest {
        val failStore = FakeRecipeStore().apply { failDeleteAll = true }
        val vm = SettingsViewModel(deps(failStore))
        var errorMsg: String? = null
        vm.clearRecipes(onDone = {}, onError = { errorMsg = it })
        advanceUntilIdle()
        assertNotNull(errorMsg)
    }

    @Test fun setTheme_updatesPreferences() = runTest {
        val prefs = FakePreferences()
        val vm = SettingsViewModel(Dependencies(FakeOpenAIClient(), FakeRecipeStore(), prefs))
        vm.setTheme(ThemePreference.Dark)
        assertEquals(ThemePreference.Dark, prefs.theme.value)
    }

    @Test fun hasApiKey_returnsTrueForFakeClient() {
        val vm = SettingsViewModel(Dependencies(FakeOpenAIClient(), FakeRecipeStore(), FakePreferences()))
        assertTrue(vm.hasApiKey())
    }
}
