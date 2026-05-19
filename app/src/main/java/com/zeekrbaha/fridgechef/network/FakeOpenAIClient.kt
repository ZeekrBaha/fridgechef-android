package com.zeekrbaha.fridgechef.network

import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeStyle

class FakeOpenAIClient : OpenAIClient {
    override suspend fun suggestRecipes(ingredients: List<String>): List<Recipe> {
        return recipes("Fridge")
    }

    override suspend fun suggestRecipes(imageJpeg: ByteArray): List<Recipe> {
        return recipes("Photo")
    }

    override suspend fun suggestRecipes(dishName: String): List<Recipe> {
        return recipes(dishName.ifBlank { "Dish" })
    }

    override suspend fun suggestRecipes(meal: MealType, style: RecipeStyle?): List<Recipe> {
        return recipes(meal.displayName.replaceFirstChar { it.uppercase() })
    }

    override suspend fun dailyPicks(): DailyPicks {
        return DailyPicks(
            breakfast = "Avocado toast",
            lunch = "Tomato soup",
            dinner = "Lemon pasta",
        )
    }

    private fun recipes(prefix: String): List<Recipe> {
        return listOf(
            Recipe(
                title = "$prefix Classic",
                description = "A reliable $prefix recipe for everyday cooking.",
                ingredients = listOf("Salt", "Olive oil", "Herbs"),
                steps = listOf("Prep ingredients", "Cook until ready", "Serve warm"),
                estimatedTime = "25 min",
            ),
            Recipe(
                title = "$prefix Light",
                description = "A lighter $prefix recipe with simple pantry ingredients.",
                ingredients = listOf("Greens", "Lemon", "Pepper"),
                steps = listOf("Wash ingredients", "Combine gently", "Season to taste"),
                estimatedTime = "15 min",
            ),
            Recipe(
                title = "$prefix Fancy",
                description = "A polished $prefix recipe for a nicer dinner.",
                ingredients = listOf("Butter", "Garlic", "Fresh herbs"),
                steps = listOf("Build flavor", "Finish carefully", "Plate neatly"),
                estimatedTime = "40 min",
            ),
        )
    }
}
