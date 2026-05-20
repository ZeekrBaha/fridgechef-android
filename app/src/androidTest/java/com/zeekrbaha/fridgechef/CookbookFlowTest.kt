package com.zeekrbaha.fridgechef

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CookbookFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchFreshApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("fridgechef", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitForIdle()
        clearRecipes()
        openCatalog()
    }

    @After
    fun closeApp() {
        scenario.close()
    }

    @Test
    fun catalogShowsAllEntryPointsAndSettingsShowsKeyState() {
        rule.onNodeWithText("Recipe Catalog").assertIsDisplayed()
        rule.onNodeWithTag("catalog.dish.field").assertIsDisplayed()
        rule.onNodeWithTag("catalog.card.breakfast").assertIsDisplayed()
        rule.onNodeWithTag("catalog.card.lunch").assertIsDisplayed()
        rule.onNodeWithTag("catalog.card.dinner").assertIsDisplayed()
        rule.onNodeWithTag("catalog.card.fridge").assertIsDisplayed()
        rule.onNodeWithTag("catalog.magic.button").assertIsDisplayed()

        openSettings()
        rule.onNodeWithText("OpenAI key: Configured").assertIsDisplayed()
        rule.onNodeWithText("System").assertIsDisplayed()
        rule.onNodeWithText("Light").assertIsDisplayed()
        rule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun dishGenerationMealCardsAndMagicCreateRecipeBatches() {
        clearRecipes()

        openCatalog()
        rule.onNodeWithTag("catalog.dish.field").performTextInput("Ramen")
        rule.onNodeWithTag("catalog.go.button").performClick()
        rule.onNodeWithText("3 recipes").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()

        openCatalog()
        rule.onNodeWithTag("catalog.card.breakfast").performClick()
        rule.onNodeWithText("3 recipes").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()

        openCatalog()
        rule.onNodeWithTag("catalog.magic.button").performClick()
        rule.onNodeWithText("3 recipes").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()

        openRecipes()
        rule.onNodeWithText("Ramen Classic").assertIsDisplayed()
    }

    @Test
    fun userRecipeCanBeCreatedAndPersistsAcrossTabNavigation() {
        clearRecipes()
        createRecipe("Test Toast")

        rule.onNodeWithText("Test Toast").assertIsDisplayed()
        openCatalog()
        openRecipes()
        rule.onNodeWithText("Test Toast").assertIsDisplayed()
    }

    @Test
    fun favoriteFilterShowsOnlyFavoritedRecipes() {
        clearRecipes()
        createRecipe("Favorite Toast")
        createRecipe("Plain Toast")

        openRecipes()
        rule.onNodeWithTag("recipes.favorite.favorite_toast").performClick()

        rule.onNodeWithTag("recipes.filter.favorites").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Favorite Toast").assertIsDisplayed()
        rule.onAllNodesWithText("Plain Toast").assertCountEquals(0)
    }

    @Test
    fun cancelWithUnsavedChangesShowsDiscardPrompt() {
        clearRecipes()
        openRecipes()
        rule.onNodeWithTag("recipes.create.button").performClick()
        rule.onNodeWithTag("create.title.field").performTextInput("Discard Toast")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard changes?").assertIsDisplayed()
        rule.onNodeWithText("Discard").performClick()
        rule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }

    @Test
    fun recipeCanBeEditedFromDetail() {
        clearRecipes()
        createRecipe("Original Toast")

        openRecipeDetail("Original Toast")
        rule.onNodeWithTag("detail.edit.button").performClick()
        rule.onNodeWithTag("create.title.field").performTextClearance()
        rule.onNodeWithTag("create.title.field").performTextInput("Edited Toast")
        rule.onNodeWithTag("create.save.button").assertIsEnabled()
        rule.onNodeWithTag("create.save.button").performClick()

        rule.onNodeWithText("Edited Toast").assertIsDisplayed()
        rule.onAllNodesWithText("Original Toast").assertCountEquals(0)
    }

    @Test
    fun recipeCanBeDeletedFromEditForm() {
        clearRecipes()
        createRecipe("Delete Edit Toast")

        openRecipeDetail("Delete Edit Toast")
        rule.onNodeWithTag("detail.edit.button").performClick()
        rule.onNodeWithTag("create.delete.button").performClick()
        rule.onNodeWithText("Delete this recipe?").assertIsDisplayed()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }

    @Test
    fun deletingSingleRecipeCascadesSingleRecipeBatch() {
        clearRecipes()
        createRecipe("Delete Me")

        openRecipes()
        rule.onNodeWithTag("recipes.delete.batch").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }

    @Test
    fun deletingBatchRemovesAllRecipesInBatch() {
        clearRecipes()

        openCatalog()
        rule.onNodeWithTag("catalog.dish.field").performTextInput("Pasta")
        rule.onNodeWithTag("catalog.go.button").performClick()
        rule.onNodeWithText("3 recipes").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()

        openRecipes()
        rule.onNodeWithTag("recipes.delete.batch").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }

    @Test
    fun settingsThemeCanSwitchBetweenAllModes() {
        openSettings()
        rule.onNodeWithText("Light").performClick()
        rule.onNodeWithText("Dark").performClick()
        rule.onNodeWithText("System").performClick()
        rule.onNodeWithText("Clear all recipes").assertIsDisplayed()
    }

    private fun clearRecipes() {
        openSettings()
        rule.onNodeWithText("Clear all recipes").performClick()
        rule.onNodeWithText("Clear all").performClick()
        openRecipes()
        rule.waitForIdle()
    }

    private fun createRecipe(title: String) {
        openRecipes()
        rule.onNodeWithTag("recipes.create.button").performClick()
        rule.onNodeWithTag("create.title.field").performTextInput(title)
        rule.onNodeWithTag("create.description.field").performTextInput("Simple breakfast toast.")
        rule.onNodeWithTag("ingredients.row.0.field").performTextInput("Bread")
        rule.onNodeWithTag("steps.row.0.field").performTextInput("Toast it")
        rule.onNodeWithTag("create.time.field").performScrollTo()
        rule.onNodeWithTag("create.time.field").performTextInput("5 min")
        rule.onNodeWithTag("create.save.button").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
    }

    private fun openRecipeDetail(title: String) {
        openRecipes()
        rule.onNodeWithTag("recipes.row.${title.recipeTagSlug()}", useUnmergedTree = true).performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("detail.edit.button"), 5000)
    }

    private fun openCatalog() {
        rule.onNodeWithTag("nav.catalog", useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    private fun openRecipes() {
        rule.onNodeWithTag("nav.recipes", useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    private fun openSettings() {
        rule.onNodeWithTag("nav.settings", useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    private fun String.recipeTagSlug(): String {
        return lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }
}
