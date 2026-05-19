package com.zeekrbaha.fridgechef.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zeekrbaha.fridgechef.Dependencies
import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.RecipeStyle
import com.zeekrbaha.fridgechef.data.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Idle : CatalogState
    data object Loading : CatalogState
    data class Loaded(val batch: RecipeBatch) : CatalogState
    data class Error(val message: String) : CatalogState
}

class CatalogViewModel(private val dependencies: Dependencies) : ViewModel() {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Idle)
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private val _dailyPicks = MutableStateFlow(dependencies.preferences.readDailyPicks())
    val dailyPicks: StateFlow<DailyPicks?> = _dailyPicks.asStateFlow()

    fun refreshDailyPicksIfStale() {
        val cached = dependencies.preferences.readDailyPicks()
        if (cached != null && dependencies.preferences.dailyPicksAreFromToday(cached)) {
            _dailyPicks.value = cached
            return
        }
        viewModelScope.launch {
            runCatching { dependencies.openAIClient.dailyPicks() }
                .onSuccess {
                    dependencies.preferences.writeDailyPicks(it)
                    _dailyPicks.value = it
                }
        }
    }

    fun generateDish(dishName: String) {
        if (dishName.isBlank()) return
        run { dependencies.openAIClient.suggestRecipes(dishName.trim()) }
    }

    fun generateMeal(meal: MealType, style: RecipeStyle? = null) {
        run { dependencies.openAIClient.suggestRecipes(meal, style) }
    }

    fun generateImage(imageJpeg: ByteArray) {
        run { dependencies.openAIClient.suggestRecipes(imageJpeg) }
    }

    fun generateSurprise() {
        generateMeal(MealType.entries.random(), RecipeStyle.entries.random())
    }

    fun reset() {
        _state.value = CatalogState.Idle
    }

    private fun run(fetch: suspend () -> List<com.zeekrbaha.fridgechef.data.Recipe>) {
        viewModelScope.launch {
            _state.value = CatalogState.Loading
            runCatching {
                val batch = RecipeBatch(recipes = fetch())
                dependencies.recipeStore.save(batch)
            }.onSuccess { batch ->
                _state.value = CatalogState.Loaded(batch)
            }.onFailure { error ->
                _state.value = CatalogState.Error(error.message ?: "Could not generate recipes.")
            }
        }
    }
}

class RecipesViewModel(private val dependencies: Dependencies) : ViewModel() {
    private val _batches = MutableStateFlow<List<RecipeBatch>>(emptyList())
    val batches: StateFlow<List<RecipeBatch>> = _batches.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _batches.value = dependencies.recipeStore.loadBatches()
        }
    }
}

class SettingsViewModel(private val dependencies: Dependencies) : ViewModel() {
    val theme = dependencies.preferences.theme

    fun setTheme(themePreference: ThemePreference) {
        dependencies.preferences.setTheme(themePreference)
    }

    fun clearRecipes(onDone: () -> Unit) {
        viewModelScope.launch {
            dependencies.recipeStore.deleteAll()
            onDone()
        }
    }

    fun hasApiKey(): Boolean = dependencies.openAIClient !is com.zeekrbaha.fridgechef.network.OpenAIChatClient ||
        com.zeekrbaha.fridgechef.BuildConfig.OPENAI_API_KEY.isNotBlank()
}

class FridgeChefViewModelFactory(private val dependencies: Dependencies) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            CatalogViewModel::class.java -> CatalogViewModel(dependencies)
            RecipesViewModel::class.java -> RecipesViewModel(dependencies)
            SettingsViewModel::class.java -> SettingsViewModel(dependencies)
            else -> error("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
