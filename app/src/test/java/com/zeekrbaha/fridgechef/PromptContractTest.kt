package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.RecipeStyle
import com.zeekrbaha.fridgechef.network.Prompts
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptContractTest {
    @Test
    fun mealPromptIncludesMealAndStyle() {
        val prompt = Prompts.meal(MealType.Breakfast, RecipeStyle.Healthy)

        assertTrue(prompt.contains("breakfast"))
        assertTrue(prompt.contains("healthy"))
        assertTrue(prompt.contains("exactly 3"))
    }

    @Test
    fun dailyPicksPromptRequiresConcreteTitles() {
        val prompt = Prompts.dailyPicks()

        assertTrue(prompt.contains("breakfast, lunch, and dinner"))
        assertTrue(prompt.contains("Return JSON"))
    }
}
