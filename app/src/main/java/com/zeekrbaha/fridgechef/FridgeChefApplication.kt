package com.zeekrbaha.fridgechef

import android.app.Application
import com.zeekrbaha.fridgechef.data.AppPreferences
import com.zeekrbaha.fridgechef.data.RecipeSqliteStore
import com.zeekrbaha.fridgechef.network.BuildConfigAPIKeyProvider
import com.zeekrbaha.fridgechef.network.FakeOpenAIClient
import com.zeekrbaha.fridgechef.network.OpenAIChatClient

class FridgeChefApplication : Application() {
    lateinit var dependencies: Dependencies
        private set

    override fun onCreate() {
        super.onCreate()
        val preferences = AppPreferences(this)
        dependencies = Dependencies(
            openAIClient = if (BuildConfig.USE_FAKE_OPENAI) FakeOpenAIClient() else OpenAIChatClient(BuildConfigAPIKeyProvider()),
            recipeStore = RecipeSqliteStore(this),
            preferences = preferences,
        )
    }
}

data class Dependencies(
    val openAIClient: com.zeekrbaha.fridgechef.network.OpenAIClient,
    val recipeStore: com.zeekrbaha.fridgechef.data.RecipeStore,
    val preferences: com.zeekrbaha.fridgechef.data.Preferences,
)
