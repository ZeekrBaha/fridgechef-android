package com.zeekrbaha.fridgechef.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zeekrbaha.fridgechef.Dependencies
import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.RecipeSource
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
        launchGenerate { dependencies.openAIClient.suggestRecipes(dishName.trim()) }
    }

    fun generateMeal(meal: MealType, style: RecipeStyle? = null) {
        launchGenerate { dependencies.openAIClient.suggestRecipes(meal, style) }
    }

    fun generateImage(imageJpeg: ByteArray) {
        launchGenerate { dependencies.openAIClient.suggestRecipes(imageJpeg) }
    }

    fun generateSurprise() {
        generateMeal(MealType.entries.random(), RecipeStyle.entries.random())
    }

    fun reset() {
        _state.value = CatalogState.Idle
    }

    private fun launchGenerate(fetch: suspend () -> List<com.zeekrbaha.fridgechef.data.Recipe>) {
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

    private val _filter = MutableStateFlow(RecipeFilter.All)
    val filter: StateFlow<RecipeFilter> = _filter.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _batches.value = dependencies.recipeStore.loadBatches()
        }
    }

    fun setFilter(filter: RecipeFilter) {
        _filter.value = filter
    }

    fun visibleBatches(): List<RecipeBatch> = applyFilter(_batches.value, _filter.value)

    fun createRecipe(recipe: Recipe, onSaved: (RecipeBatch) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.save(
                    RecipeBatch(recipes = listOf(recipe), source = RecipeSource.User),
                )
            }.onSuccess { batch ->
                load()
                onSaved(batch)
            }.onFailure { error ->
                onError(error.message ?: "Couldn't save recipe.")
            }
        }
    }

    fun updateRecipe(recipe: Recipe, batchId: String, onSaved: (Recipe) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.update(recipe, batchId)
            }.onSuccess { updated ->
                load()
                onSaved(updated)
            }.onFailure { error ->
                onError(error.message ?: "Couldn't save recipe.")
            }
        }
    }

    fun setFavorite(recipe: Recipe, isFavorite: Boolean, onSaved: (Recipe) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.setFavorite(recipe.id, isFavorite)
            }.onSuccess { updated ->
                load()
                onSaved(updated)
            }.onFailure { error ->
                onError(error.message ?: "Couldn't update favorite.")
            }
        }
    }

    fun toggleBatchFavorite(batch: RecipeBatch, onSaved: (RecipeBatch) -> Unit = {}, onError: (String) -> Unit = {}) {
        val targetFavorite = !batch.recipes.all { it.isFavorite }
        // Optimistic: flip the heart immediately, then reconcile/revert from the store.
        _batches.value = _batches.value.map { current ->
            if (current.id == batch.id) current.copy(recipes = current.recipes.map { it.copy(isFavorite = targetFavorite) }) else current
        }
        viewModelScope.launch {
            runCatching {
                batch.recipes.forEach { dependencies.recipeStore.setFavorite(it.id, targetFavorite) }
            }.onSuccess {
                _batches.value = dependencies.recipeStore.loadBatches()
                onSaved(_batches.value.firstOrNull { it.id == batch.id } ?: batch)
            }.onFailure { error ->
                _batches.value = dependencies.recipeStore.loadBatches()
                onError(error.message ?: "Couldn't update favorite.")
            }
        }
    }

    fun deleteRecipe(recipeId: String, onDeleted: (RecipeBatch?) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.deleteRecipe(recipeId)
            }.onSuccess { batch ->
                load()
                onDeleted(batch)
            }.onFailure { error ->
                onError(error.message ?: "Couldn't delete recipe.")
            }
        }
    }

    fun deleteBatch(batchId: String, onDeleted: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.deleteBatch(batchId)
            }.onSuccess {
                load()
                onDeleted()
            }.onFailure { error ->
                onError(error.message ?: "Couldn't delete recipe batch.")
            }
        }
    }
}

enum class RecipeFilter {
    All,
    Favorites,
}

class SettingsViewModel(private val dependencies: Dependencies) : ViewModel() {
    val theme = dependencies.preferences.theme

    fun setTheme(themePreference: ThemePreference) {
        dependencies.preferences.setTheme(themePreference)
    }

    fun clearRecipes(onDone: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                dependencies.recipeStore.deleteAll()
            }.onSuccess {
                onDone()
            }.onFailure { error ->
                onError(error.message ?: "Couldn't clear recipes.")
            }
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
