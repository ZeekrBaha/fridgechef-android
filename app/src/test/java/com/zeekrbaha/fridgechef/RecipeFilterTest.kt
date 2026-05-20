package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.viewmodel.RecipeFilter
import com.zeekrbaha.fridgechef.viewmodel.applyFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeFilterTest {
    private fun recipe(title: String, favorite: Boolean) =
        Recipe(title = title, description = "", ingredients = listOf("x"), steps = listOf("y"), estimatedTime = "5 min", isFavorite = favorite)

    private val batch = RecipeBatch(
        recipes = listOf(recipe("A", true), recipe("B", false), recipe("C", true)),
    )
    private val noFavorites = RecipeBatch(recipes = listOf(recipe("D", false)))

    @Test
    fun allReturnsEverythingUntouched() {
        val input = listOf(batch, noFavorites)
        assertEquals(input, applyFilter(input, RecipeFilter.All))
    }

    @Test
    fun favoritesTrimsToFavoritedRecipesAndDropsEmptyBatches() {
        val result = applyFilter(listOf(batch, noFavorites), RecipeFilter.Favorites)
        assertEquals(1, result.size)
        assertEquals(listOf("A", "C"), result.first().recipes.map { it.title })
    }
}
