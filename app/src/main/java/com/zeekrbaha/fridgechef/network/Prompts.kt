package com.zeekrbaha.fridgechef.network

import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.RecipeStyle

object Prompts {
    private const val SHAPE = "Each recipe must have: a short title, a one-paragraph description, an ingredient list (with rough quantities), step-by-step instructions, and an estimated total time as a short string like \"30 min\"."

    fun ingredients() = "You are a cooking assistant. Given a list of ingredients the user has on hand, suggest exactly 3 recipes the user can make. Prefer recipes that use as many of the provided ingredients as possible. $SHAPE"

    fun image() = "You are a cooking assistant. The image shows the contents of someone's fridge or pantry. First, identify the visible ingredients. Then suggest exactly 3 recipes the user can make from them. Prefer recipes that use as many of the visible ingredients as possible. $SHAPE Ignore non-edible items in the image."

    fun dishName() = "You are a cooking assistant. The user has named a dish they want to cook. Suggest exactly 3 variations of that dish - for example: a classic version, a healthier/lighter version, and a fancy/restaurant-style version. Each recipe must have: a short title (clearly indicating the variation), a one-paragraph description, an ingredient list (with rough quantities), step-by-step instructions, and an estimated total time as a short string like \"30 min\"."

    fun meal(meal: MealType, style: RecipeStyle?) =
        "You are a cooking assistant. Suggest exactly 3 ${meal.displayName} recipes someone could cook today." +
            (style?.let { " Make them ${it.promptFragment}." } ?: "") +
            " Vary the recipes - don't give three nearly-identical dishes. $SHAPE"

    fun dailyPicks() = "You are a cooking assistant. Suggest one specific recipe title (just the title, no description) for each meal type today: breakfast, lunch, and dinner. Pick recipes that are interesting but achievable in a normal home kitchen. Titles should be short and concrete - for example: \"Avocado toast with poached egg\" not \"A delicious breakfast option\". Return JSON with keys breakfast, lunch, dinner."
}
