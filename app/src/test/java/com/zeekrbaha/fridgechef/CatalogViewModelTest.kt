package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeStyle
import com.zeekrbaha.fridgechef.network.FakeOpenAIClient
import com.zeekrbaha.fridgechef.network.OpenAIClient
import com.zeekrbaha.fridgechef.network.OpenAIError
import com.zeekrbaha.fridgechef.viewmodel.CatalogState
import com.zeekrbaha.fridgechef.viewmodel.CatalogViewModel
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
class CatalogViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun deps(
        client: OpenAIClient = FakeOpenAIClient(),
        prefs: FakePreferences = FakePreferences(),
    ) = Dependencies(client, FakeRecipeStore(), prefs)

    // --- generate paths ---

    @Test fun generateDish_savesLoadedBatch() = runTest {
        val vm = CatalogViewModel(deps())
        vm.generateDish("Pasta")
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Loaded)
    }

    @Test fun generateDish_blankName_staysIdle() = runTest {
        val vm = CatalogViewModel(deps())
        vm.generateDish("   ")
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Idle)
    }

    @Test fun generateMeal_transitionsToLoaded() = runTest {
        val vm = CatalogViewModel(deps())
        vm.generateMeal(MealType.Lunch)
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Loaded)
    }

    @Test fun generateImage_transitionsToLoaded() = runTest {
        val vm = CatalogViewModel(deps())
        vm.generateImage(byteArrayOf(1, 2, 3))
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Loaded)
    }

    @Test fun generateSurprise_transitionsToLoaded() = runTest {
        val vm = CatalogViewModel(deps())
        vm.generateSurprise()
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Loaded)
    }

    @Test fun clientError_transitionsToError() = runTest {
        val errorClient = object : OpenAIClient {
            override suspend fun suggestRecipes(ingredients: List<String>): List<Recipe> = throw OpenAIError.Server
            override suspend fun suggestRecipes(imageJpeg: ByteArray): List<Recipe> = throw OpenAIError.Server
            override suspend fun suggestRecipes(dishName: String): List<Recipe> = throw OpenAIError.Server
            override suspend fun suggestRecipes(meal: MealType, style: RecipeStyle?): List<Recipe> = throw OpenAIError.Server
            override suspend fun dailyPicks(): DailyPicks = throw OpenAIError.Server
        }
        val vm = CatalogViewModel(deps(client = errorClient))
        vm.generateDish("Ramen")
        advanceUntilIdle()
        assertTrue(vm.state.value is CatalogState.Error)
    }

    // --- daily picks ---

    @Test fun refreshDailyPicks_coldFetch_populatesStateFlow() = runTest {
        val prefs = FakePreferences().apply { picksFromToday = false }
        val vm = CatalogViewModel(deps(prefs = prefs))
        vm.refreshDailyPicksIfStale()
        advanceUntilIdle()
        assertNotNull(vm.dailyPicks.value)
        assertEquals("Avocado toast", vm.dailyPicks.value?.breakfast)
    }

    @Test fun refreshDailyPicks_cacheHit_usesStoredValue() = runTest {
        val cached = DailyPicks(breakfast = "Cached Oats", lunch = "Cached Soup", dinner = "Cached Pasta")
        val prefs = FakePreferences().apply {
            writeDailyPicks(cached)
            picksFromToday = true
        }
        val vm = CatalogViewModel(deps(prefs = prefs))
        vm.refreshDailyPicksIfStale()
        advanceUntilIdle()
        assertEquals("Cached Oats", vm.dailyPicks.value?.breakfast)
    }

    @Test fun refreshDailyPicks_staleCache_fetchesFresh() = runTest {
        val stale = DailyPicks(breakfast = "Stale", lunch = "Stale", dinner = "Stale")
        val prefs = FakePreferences().apply {
            writeDailyPicks(stale)
            picksFromToday = false
        }
        val vm = CatalogViewModel(deps(prefs = prefs))
        vm.refreshDailyPicksIfStale()
        advanceUntilIdle()
        assertEquals("Avocado toast", vm.dailyPicks.value?.breakfast)
    }

    @Test fun refreshDailyPicks_apiError_doesNotThrow() = runTest {
        val errorClient = object : OpenAIClient {
            override suspend fun suggestRecipes(ingredients: List<String>): List<Recipe> = emptyList()
            override suspend fun suggestRecipes(imageJpeg: ByteArray): List<Recipe> = emptyList()
            override suspend fun suggestRecipes(dishName: String): List<Recipe> = emptyList()
            override suspend fun suggestRecipes(meal: MealType, style: RecipeStyle?): List<Recipe> = emptyList()
            override suspend fun dailyPicks(): DailyPicks = throw OpenAIError.Server
        }
        val prefs = FakePreferences().apply { picksFromToday = false }
        val vm = CatalogViewModel(deps(client = errorClient, prefs = prefs))
        vm.refreshDailyPicksIfStale()
        advanceUntilIdle()
        assertNull(vm.dailyPicks.value)
    }
}
