package com.zeekrbaha.fridgechef.data

interface RecipeStore {
    suspend fun save(batch: RecipeBatch): RecipeBatch
    suspend fun loadBatches(): List<RecipeBatch>
    suspend fun batchById(id: String): RecipeBatch?
    suspend fun deleteAll()
    suspend fun update(recipe: Recipe, batchId: String): Recipe
    suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Recipe
    suspend fun deleteRecipe(recipeId: String): RecipeBatch?
    suspend fun deleteBatch(batchId: String)
}

class RecipeStoreNotFoundException : Exception("Recipe not found.")
