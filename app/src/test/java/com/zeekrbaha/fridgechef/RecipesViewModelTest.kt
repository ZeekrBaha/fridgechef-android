package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.RecipeSource
import com.zeekrbaha.fridgechef.data.RecipeStore
import com.zeekrbaha.fridgechef.network.FakeOpenAIClient
import com.zeekrbaha.fridgechef.viewmodel.RecipesViewModel
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
class RecipesViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun recipe(title: String, favorite: Boolean = false) =
        Recipe(title = title, description = "", ingredients = listOf("a"), steps = listOf("b"), estimatedTime = "5 min", isFavorite = favorite)

    private fun deps(store: RecipeStore) = Dependencies(FakeOpenAIClient(), store, FakePreferences())

    @Test
    fun createRecipePersistsAsUserBatch() = runTest {
        val store = FakeRecipeStore()
        val vm = RecipesViewModel(deps(store))
        vm.createRecipe(recipe("Toast"))
        advanceUntilIdle()
        val saved = store.loadBatches()
        assertEquals(1, saved.size)
        assertEquals(RecipeSource.User, saved.first().source)
    }

    @Test
    fun toggleBatchFavoriteFavoritesAllRecipesInBatch() = runTest {
        val batch = RecipeBatch(recipes = listOf(recipe("A"), recipe("B")))
        val store = FakeRecipeStore(listOf(batch))
        val vm = RecipesViewModel(deps(store))
        vm.load(); advanceUntilIdle()

        vm.toggleBatchFavorite(batch); advanceUntilIdle()

        assertTrue(store.batchById(batch.id)!!.recipes.all { it.isFavorite })
    }

    @Test
    fun toggleBatchFavoriteIsOptimisticBeforePersisting() = runTest {
        val batch = RecipeBatch(recipes = listOf(recipe("A")))
        val store = FakeRecipeStore(listOf(batch))
        val vm = RecipesViewModel(deps(store))
        vm.load(); advanceUntilIdle()

        // No advanceUntilIdle: the optimistic state must already be flipped synchronously.
        vm.toggleBatchFavorite(batch)
        assertTrue(vm.batches.value.first { it.id == batch.id }.recipes.all { it.isFavorite })
    }

    @Test
    fun toggleBatchFavoriteRevertsOnStoreError() = runTest {
        val batch = RecipeBatch(recipes = listOf(recipe("A")))
        val store = FakeRecipeStore(listOf(batch)).apply { failSetFavorite = true }
        val vm = RecipesViewModel(deps(store))
        vm.load(); advanceUntilIdle()

        var error: String? = null
        vm.toggleBatchFavorite(batch, onError = { error = it })
        advanceUntilIdle()

        assertEquals(false, vm.batches.value.first { it.id == batch.id }.recipes.first().isFavorite)
        assertNotNull(error)
    }

    @Test
    fun deletingTheOnlyRecipeCascadesItsBatch() = runTest {
        val solo = recipe("Solo")
        val batch = RecipeBatch(recipes = listOf(solo), source = RecipeSource.User)
        val store = FakeRecipeStore(listOf(batch))
        val vm = RecipesViewModel(deps(store))

        var deleted: RecipeBatch? = batch
        vm.deleteRecipe(solo.id, onDeleted = { deleted = it })
        advanceUntilIdle()

        assertNull(deleted)
        assertTrue(store.loadBatches().isEmpty())
    }
}
