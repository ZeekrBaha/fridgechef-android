package com.zeekrbaha.fridgechef

object RecipeFormValidator {
    fun isSaveable(title: String, ingredients: List<String>, steps: List<String>): Boolean =
        title.trim().isNotEmpty() &&
            ingredients.any { it.trim().isNotEmpty() } &&
            steps.any { it.trim().isNotEmpty() }
}
