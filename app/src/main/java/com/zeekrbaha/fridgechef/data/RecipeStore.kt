package com.zeekrbaha.fridgechef.data

interface RecipeStore {
    suspend fun save(batch: RecipeBatch): RecipeBatch
    suspend fun loadBatches(): List<RecipeBatch>
    suspend fun batchById(id: String): RecipeBatch?
    suspend fun deleteAll()
}
