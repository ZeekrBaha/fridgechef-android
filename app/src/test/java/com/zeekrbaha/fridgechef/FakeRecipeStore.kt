package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.RecipeStore
import com.zeekrbaha.fridgechef.data.RecipeStoreNotFoundException

/** In-memory RecipeStore for JVM unit tests (no Android Context / SQLite). */
class FakeRecipeStore(initial: List<RecipeBatch> = emptyList()) : RecipeStore {
    private val batches = initial.toMutableList()

    /** When true, [setFavorite] throws — used to exercise the optimistic-revert path. */
    var failSetFavorite = false

    /** When true, [deleteAll] throws — used to exercise the clear-recipes error path. */
    var failDeleteAll = false

    override suspend fun save(batch: RecipeBatch): RecipeBatch {
        batches.removeAll { it.id == batch.id }
        batches.add(0, batch)
        return batch
    }

    override suspend fun loadBatches(): List<RecipeBatch> = batches.sortedByDescending { it.createdAt }

    override suspend fun batchById(id: String): RecipeBatch? = batches.firstOrNull { it.id == id }

    override suspend fun deleteAll() {
        if (failDeleteAll) throw RuntimeException("db error")
        batches.clear()
    }

    override suspend fun update(recipe: Recipe, batchId: String): Recipe {
        val idx = batches.indexOfFirst { it.id == batchId }
        if (idx < 0) throw RecipeStoreNotFoundException()
        val batch = batches[idx]
        if (batch.recipes.none { it.id == recipe.id }) throw RecipeStoreNotFoundException()
        batches[idx] = batch.copy(recipes = batch.recipes.map { if (it.id == recipe.id) recipe else it })
        return recipe
    }

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Recipe {
        if (failSetFavorite) throw RecipeStoreNotFoundException()
        for ((i, batch) in batches.withIndex()) {
            val r = batch.recipes.firstOrNull { it.id == recipeId } ?: continue
            val updated = r.copy(isFavorite = isFavorite)
            batches[i] = batch.copy(recipes = batch.recipes.map { if (it.id == recipeId) updated else it })
            return updated
        }
        throw RecipeStoreNotFoundException()
    }

    override suspend fun deleteRecipe(recipeId: String): RecipeBatch? {
        for ((i, batch) in batches.withIndex()) {
            if (batch.recipes.none { it.id == recipeId }) continue
            val remaining = batch.recipes.filterNot { it.id == recipeId }
            return if (remaining.isEmpty()) {
                batches.removeAt(i)
                null
            } else {
                val updated = batch.copy(recipes = remaining)
                batches[i] = updated
                updated
            }
        }
        throw RecipeStoreNotFoundException()
    }

    override suspend fun deleteBatch(batchId: String) {
        if (!batches.removeAll { it.id == batchId }) throw RecipeStoreNotFoundException()
    }
}
