package com.zeekrbaha.fridgechef

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CookbookFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun catalogRecipesAndSettingsTabsRender() {
        rule.onNodeWithText("Recipe Catalog").assertIsDisplayed()
        rule.onNodeWithText("Breakfast ideas").assertIsDisplayed()

        rule.onNodeWithText("Recipes").performClick()
        rule.onNodeWithTag("recipes.create.button").assertIsDisplayed()

        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("OpenAI key: Missing").assertIsDisplayed()
    }

    @Test
    fun userRecipeCanBeCreatedFavoritedFilteredEditedAndDeleted() {
        clearRecipes()
        createRecipe("Test Toast")

        rule.onNodeWithText("Test Toast").assertIsDisplayed()
        rule.onNodeWithText("Test Toast").performClick()
        rule.onNodeWithText("Test Toast").performClick()
        rule.onNodeWithTag("detail.favorite.button").performClick()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Back").performClick()

        rule.onNodeWithTag("recipes.filter.favorites").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Test Toast").assertIsDisplayed()

        rule.onNodeWithText("Test Toast").performClick()
        rule.onNodeWithText("Test Toast").performClick()
        rule.onNodeWithTag("detail.edit.button").performClick()
        rule.onNodeWithTag("create.title.field").performTextClearance()
        rule.onNodeWithTag("create.title.field").performTextInput("Edited Toast")
        rule.onNodeWithTag("create.save.button").performClick()
        rule.onNodeWithText("Edited Toast").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()

        rule.onNodeWithTag("batch.delete.recipe").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("No saved recipe batches yet.").assertIsDisplayed()
    }

    private fun clearRecipes() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Clear all recipes").performClick()
        rule.onNodeWithText("Recipes").performClick()
        rule.waitForIdle()
    }

    private fun createRecipe(title: String) {
        rule.onNodeWithTag("recipes.create.button").performClick()
        rule.onNodeWithTag("create.title.field").performTextInput(title)
        rule.onNodeWithTag("create.description.field").performTextInput("Simple breakfast toast.")
        rule.onNodeWithTag("ingredients.row.0.field").performTextInput("Bread")
        rule.onNodeWithTag("steps.row.0.field").performTextInput("Toast it")
        rule.onNodeWithTag("create.time.field").performTextInput("5 min")
        rule.onNodeWithTag("create.save.button").performClick()
        rule.waitForIdle()
    }
}
