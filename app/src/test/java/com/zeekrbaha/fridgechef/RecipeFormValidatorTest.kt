package com.zeekrbaha.fridgechef

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeFormValidatorTest {
    @Test fun blankTitle_notSaveable() {
        assertFalse(RecipeFormValidator.isSaveable("", listOf("flour"), listOf("bake")))
    }

    @Test fun whitespaceonlyTitle_notSaveable() {
        assertFalse(RecipeFormValidator.isSaveable("   ", listOf("flour"), listOf("bake")))
    }

    @Test fun noNonBlankIngredients_notSaveable() {
        assertFalse(RecipeFormValidator.isSaveable("My Recipe", listOf("", "  "), listOf("bake")))
    }

    @Test fun noNonBlankSteps_notSaveable() {
        assertFalse(RecipeFormValidator.isSaveable("My Recipe", listOf("flour"), listOf("", "  ")))
    }

    @Test fun emptyIngredientsList_notSaveable() {
        assertFalse(RecipeFormValidator.isSaveable("My Recipe", emptyList(), listOf("bake")))
    }

    @Test fun allFieldsValid_saveable() {
        assertTrue(RecipeFormValidator.isSaveable("My Recipe", listOf("flour"), listOf("bake")))
    }
}
