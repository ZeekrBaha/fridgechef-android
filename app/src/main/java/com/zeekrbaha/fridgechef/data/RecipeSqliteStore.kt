package com.zeekrbaha.fridgechef.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Locale

class RecipeSqliteStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION), RecipeStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE recipe_batch (
                id TEXT PRIMARY KEY NOT NULL,
                created_at INTEGER NOT NULL,
                input_ingredients_json TEXT NOT NULL,
                input_image_thumbnail_jpeg BLOB,
                source TEXT NOT NULL DEFAULT 'ai'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE recipe (
                id TEXT PRIMARY KEY NOT NULL,
                batch_id TEXT NOT NULL,
                title TEXT NOT NULL,
                recipe_description TEXT NOT NULL,
                ingredients_json TEXT NOT NULL,
                steps_json TEXT NOT NULL,
                estimated_time TEXT NOT NULL,
                display_order INTEGER NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER,
                FOREIGN KEY(batch_id) REFERENCES recipe_batch(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX recipe_batch_created_at_idx ON recipe_batch(created_at DESC)")
        db.execSQL("CREATE INDEX recipe_batch_id_idx ON recipe(batch_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE recipe_batch ADD COLUMN source TEXT NOT NULL DEFAULT 'ai'")
            db.execSQL("ALTER TABLE recipe ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recipe ADD COLUMN updated_at INTEGER")
        }
    }

    override suspend fun save(batch: RecipeBatch): RecipeBatch = withContext(Dispatchers.IO) {
        writableDatabase.transaction {
            insertWithOnConflict("recipe_batch", null, batch.toBatchValues(), SQLiteDatabase.CONFLICT_REPLACE)
            delete("recipe", "batch_id = ?", arrayOf(batch.id))
            batch.recipes.forEachIndexed { index, recipe ->
                insertWithOnConflict("recipe", null, recipe.toRecipeValues(batch.id, index), SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
        batch
    }

    override suspend fun loadBatches(): List<RecipeBatch> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT * FROM recipe_batch ORDER BY created_at DESC", emptyArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.batchFromCursor())
                }
            }
        }
    }

    override suspend fun batchById(id: String): RecipeBatch? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT * FROM recipe_batch WHERE id = ?", arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) cursor.batchFromCursor() else null
        }
    }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        writableDatabase.transaction {
            delete("recipe", null, null)
            delete("recipe_batch", null, null)
        }
        Unit
    }

    override suspend fun update(recipe: Recipe, batchId: String): Recipe = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = recipe.copy(updatedAtEpochMillis = now)
        val changed = writableDatabase.update(
            "recipe",
            updated.toRecipeValues(batchId, recipeOrder(recipe.id) ?: 0),
            "id = ? AND batch_id = ?",
            arrayOf(recipe.id, batchId),
        )
        if (changed == 0) throw RecipeStoreNotFoundException()
        updated
    }

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Recipe = withContext(Dispatchers.IO) {
        val batchId = batchIdForRecipe(recipeId) ?: throw RecipeStoreNotFoundException()
        val values = ContentValues().apply { put("is_favorite", if (isFavorite) 1 else 0) }
        val changed = writableDatabase.update("recipe", values, "id = ?", arrayOf(recipeId))
        if (changed == 0) throw RecipeStoreNotFoundException()
        recipesForBatch(batchId).firstOrNull { it.id == recipeId } ?: throw RecipeStoreNotFoundException()
    }

    override suspend fun deleteRecipe(recipeId: String): RecipeBatch? = withContext(Dispatchers.IO) {
        val batchId = batchIdForRecipe(recipeId) ?: throw RecipeStoreNotFoundException()
        writableDatabase.transaction {
            delete("recipe", "id = ?", arrayOf(recipeId))
            val count = rawQuery("SELECT COUNT(*) FROM recipe WHERE batch_id = ?", arrayOf(batchId)).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            if (count == 0) {
                delete("recipe_batch", "id = ?", arrayOf(batchId))
            }
        }
        batchById(batchId)
    }

    override suspend fun deleteBatch(batchId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("recipe", "batch_id = ?", arrayOf(batchId))
        val changed = writableDatabase.delete("recipe_batch", "id = ?", arrayOf(batchId))
        if (changed == 0) throw RecipeStoreNotFoundException()
        Unit
    }

    private fun RecipeBatch.toBatchValues() = ContentValues().apply {
        put("id", id)
        put("created_at", createdAt.toEpochMilli())
        put("input_ingredients_json", json.encodeToString(inputIngredients))
        put("input_image_thumbnail_jpeg", inputImageThumbnailJpeg)
        put("source", source.name.lowercase(Locale.US))
    }

    private fun Recipe.toRecipeValues(batchId: String, index: Int) = ContentValues().apply {
        put("id", id)
        put("batch_id", batchId)
        put("title", title)
        put("recipe_description", description)
        put("ingredients_json", json.encodeToString(ingredients))
        put("steps_json", json.encodeToString(steps))
        put("estimated_time", estimatedTime)
        put("display_order", index)
        put("is_favorite", if (isFavorite) 1 else 0)
        put("updated_at", updatedAtEpochMillis)
    }

    private fun android.database.Cursor.batchFromCursor(): RecipeBatch {
        val batchId = getString(getColumnIndexOrThrow("id"))
        return RecipeBatch(
            id = batchId,
            createdAt = Instant.ofEpochMilli(getLong(getColumnIndexOrThrow("created_at"))),
            inputIngredients = json.decodeFromString(getString(getColumnIndexOrThrow("input_ingredients_json"))),
            inputImageThumbnailJpeg = getBlob(getColumnIndexOrThrow("input_image_thumbnail_jpeg")),
            recipes = recipesForBatch(batchId),
            source = sourceFromString(getString(getColumnIndexOrThrow("source"))),
        )
    }

    private fun recipesForBatch(batchId: String): List<Recipe> {
        return readableDatabase.rawQuery(
            "SELECT * FROM recipe WHERE batch_id = ? ORDER BY display_order ASC",
            arrayOf(batchId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Recipe(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            description = cursor.getString(cursor.getColumnIndexOrThrow("recipe_description")),
                            ingredients = json.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow("ingredients_json"))),
                            steps = json.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow("steps_json"))),
                            estimatedTime = cursor.getString(cursor.getColumnIndexOrThrow("estimated_time")),
                            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
                            updatedAtEpochMillis = if (cursor.isNull(cursor.getColumnIndexOrThrow("updated_at"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                        ),
                    )
                }
            }
        }
    }

    private fun batchIdForRecipe(recipeId: String): String? {
        return readableDatabase.rawQuery("SELECT batch_id FROM recipe WHERE id = ?", arrayOf(recipeId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun recipeOrder(recipeId: String): Int? {
        return readableDatabase.rawQuery("SELECT display_order FROM recipe WHERE id = ?", arrayOf(recipeId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else null
        }
    }

    private fun sourceFromString(value: String?) = when (value?.lowercase(Locale.US)) {
        "user" -> RecipeSource.User
        else -> RecipeSource.Ai
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private companion object {
        const val DB_NAME = "fridgechef.db"
        const val DB_VERSION = 2
    }
}
