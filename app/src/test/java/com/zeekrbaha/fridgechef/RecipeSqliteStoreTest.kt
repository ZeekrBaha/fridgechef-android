package com.zeekrbaha.fridgechef

import androidx.test.core.app.ApplicationProvider
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.RecipeSqliteStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeSqliteStoreTest {
    private lateinit var store: RecipeSqliteStore

    @Before fun setUp() {
        store = RecipeSqliteStore(ApplicationProvider.getApplicationContext())
    }

    @After fun tearDown() {
        store.close()
    }

    private fun recipe(title: String) = Recipe(
        title = title,
        description = "desc",
        ingredients = listOf("flour"),
        steps = listOf("bake"),
        estimatedTime = "20 min",
    )

    private fun batch(vararg recipes: Recipe) = RecipeBatch(recipes = recipes.toList())

    @Test fun saveAndLoadReturnsBatch() = runBlocking {
        val b = batch(recipe("Pasta"))
        store.save(b)
        val loaded = store.loadBatches()
        assertEquals(1, loaded.size)
        assertEquals("Pasta", loaded.first().recipes.first().title)
    }

    @Test fun loadBatchesEmptyReturnsEmptyList() = runBlocking {
        assertTrue(store.loadBatches().isEmpty())
    }

    @Test fun batchByIdReturnsBatchWhenFound() = runBlocking {
        val b = batch(recipe("Ramen"))
        store.save(b)
        val found = store.batchById(b.id)
        assertNotNull(found)
        assertEquals(b.id, found!!.id)
    }

    @Test fun batchByIdReturnsNullForUnknownId() = runBlocking {
        assertNull(store.batchById("does-not-exist"))
    }

    @Test fun deleteAllClearsBatchesAndRecipes() = runBlocking {
        store.save(batch(recipe("A")))
        store.save(batch(recipe("B")))
        store.deleteAll()
        assertTrue(store.loadBatches().isEmpty())
    }

    @Test fun loadBatchesOrdersNewestFirst() = runBlocking {
        val old = RecipeBatch(createdAt = Instant.ofEpochMilli(1_000), recipes = listOf(recipe("Old")))
        val new = RecipeBatch(createdAt = Instant.ofEpochMilli(9_000), recipes = listOf(recipe("New")))
        store.save(old)
        store.save(new)
        assertEquals("New", store.loadBatches().first().recipes.first().title)
    }

    @Test fun setFavoriteMarksRecipeAndPersists() = runBlocking {
        val r = recipe("Soup")
        store.save(batch(r))
        store.setFavorite(r.id, true)
        assertTrue(store.loadBatches().first().recipes.first().isFavorite)
    }

    @Test fun updateChangesRecipeFields() = runBlocking {
        val r = recipe("Original")
        val b = batch(r)
        store.save(b)
        store.update(r.copy(title = "Updated"), b.id)
        assertEquals("Updated", store.loadBatches().first().recipes.first().title)
    }

    @Test fun deleteRecipeLeavesRemainingInBatch() = runBlocking {
        val keep = recipe("Keep")
        val remove = recipe("Remove")
        val b = RecipeBatch(recipes = listOf(keep, remove))
        store.save(b)
        val remaining = store.deleteRecipe(remove.id)
        assertNotNull(remaining)
        assertEquals(1, remaining!!.recipes.size)
        assertEquals("Keep", remaining.recipes.first().title)
    }

    @Test fun deleteRecipeRemovesBatchWhenLastRecipeDeleted() = runBlocking {
        val r = recipe("Solo")
        val b = batch(r)
        store.save(b)
        val result = store.deleteRecipe(r.id)
        assertNull(result)
        assertTrue(store.loadBatches().isEmpty())
    }

    @Test fun deleteBatchRemovesBatchAndRecipes() = runBlocking {
        val b = batch(recipe("A"), recipe("B"))
        store.save(b)
        store.deleteBatch(b.id)
        assertTrue(store.loadBatches().isEmpty())
    }
}
