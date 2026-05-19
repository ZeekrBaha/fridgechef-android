package com.zeekrbaha.fridgechef.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val estimatedTime: String,
)

data class RecipeBatch(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Instant = Instant.now(),
    val inputIngredients: List<String> = emptyList(),
    val inputImageThumbnailJpeg: ByteArray? = null,
    val recipes: List<Recipe>,
)

@Serializable
data class DailyPicks(
    val breakfast: String? = null,
    val lunch: String? = null,
    val dinner: String? = null,
    val savedAtEpochMillis: Long = System.currentTimeMillis(),
)

enum class MealType(val displayName: String) {
    Breakfast("breakfast"),
    Lunch("lunch"),
    Dinner("dinner"),
}

enum class RecipeStyle(val promptFragment: String) {
    QuickEasy("quick and easy"),
    Comforting("comforting"),
    Healthy("healthy"),
    RestaurantStyle("restaurant-style"),
    OnePot("one-pot"),
    Vegetarian("vegetarian"),
}

enum class ThemePreference {
    System,
    Light,
    Dark,
}
