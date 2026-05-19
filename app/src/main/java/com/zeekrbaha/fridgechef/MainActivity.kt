package com.zeekrbaha.fridgechef

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.data.ThemePreference
import com.zeekrbaha.fridgechef.ui.theme.FridgeChefTheme
import com.zeekrbaha.fridgechef.ui.theme.InkDark
import com.zeekrbaha.fridgechef.ui.theme.InkLight
import com.zeekrbaha.fridgechef.ui.theme.PaperDark
import com.zeekrbaha.fridgechef.ui.theme.PaperLight
import com.zeekrbaha.fridgechef.ui.theme.RuleDark
import com.zeekrbaha.fridgechef.ui.theme.RuleLight
import com.zeekrbaha.fridgechef.ui.theme.SageDark
import com.zeekrbaha.fridgechef.ui.theme.SageLight
import com.zeekrbaha.fridgechef.ui.theme.Space
import com.zeekrbaha.fridgechef.ui.theme.TerracottaDark
import com.zeekrbaha.fridgechef.ui.theme.TerracottaLight
import com.zeekrbaha.fridgechef.viewmodel.CatalogState
import com.zeekrbaha.fridgechef.viewmodel.CatalogViewModel
import com.zeekrbaha.fridgechef.viewmodel.FridgeChefViewModelFactory
import com.zeekrbaha.fridgechef.viewmodel.RecipeFilter
import com.zeekrbaha.fridgechef.viewmodel.RecipesViewModel
import com.zeekrbaha.fridgechef.viewmodel.SettingsViewModel
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies = (application as FridgeChefApplication).dependencies
        setContent {
            val settings: SettingsViewModel = viewModel(factory = FridgeChefViewModelFactory(dependencies))
            val theme by settings.theme.collectAsState()
            FridgeChefTheme(theme) {
                FridgeChefApp(FridgeChefViewModelFactory(dependencies), settings)
            }
        }
    }
}

private enum class Tab { Catalog, Recipes, Settings }

private data class EditorTarget(val recipe: Recipe? = null, val batchId: String? = null)

@Composable
private fun FridgeChefApp(factory: FridgeChefViewModelFactory, settingsViewModel: SettingsViewModel) {
    val catalog: CatalogViewModel = viewModel(factory = factory)
    val recipes: RecipesViewModel = viewModel(factory = factory)
    var tab by rememberSaveable { mutableStateOf(Tab.Catalog) }
    var activeBatch by remember { mutableStateOf<RecipeBatch?>(null) }
    var activeRecipe by remember { mutableStateOf<Recipe?>(null) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    LaunchedEffect(Unit) {
        catalog.refreshDailyPicksIfStale()
        recipes.load()
    }

    when {
        editorTarget != null -> CreateEditRecipeScreen(
            target = editorTarget!!,
            onCancel = { editorTarget = null },
            onSave = { recipe, batchId ->
                if (batchId == null) {
                    recipes.createRecipe(recipe) {
                        editorTarget = null
                        activeBatch = it
                    }
                } else {
                    recipes.updateRecipe(recipe, batchId) { updated ->
                        editorTarget = null
                        activeRecipe = updated
                        activeBatch = activeBatch?.replacing(updated)
                    }
                }
            },
        )
        activeRecipe != null -> RecipeDetailScreen(
            recipe = activeRecipe!!,
            onBack = { activeRecipe = null },
            onEdit = { editorTarget = EditorTarget(activeRecipe, activeBatch?.id) },
            onFavorite = { favorite ->
                val current = activeRecipe ?: return@RecipeDetailScreen
                recipes.setFavorite(current, favorite) { updated ->
                    activeRecipe = updated
                    activeBatch = activeBatch?.replacing(updated)
                }
            },
        )
        activeBatch != null -> RecipeBatchScreen(
            batch = activeBatch!!,
            onRecipe = { activeRecipe = it },
            onBack = { activeBatch = null },
            onDelete = { recipe ->
                recipes.deleteRecipe(recipe.id) { updatedBatch ->
                    activeBatch = updatedBatch
                    if (updatedBatch == null) activeRecipe = null
                }
            },
        )
        else -> Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = tab == Tab.Catalog,
                        onClick = { tab = Tab.Catalog },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Catalog tab") },
                        label = { Text("Catalog") },
                        modifier = Modifier.testTag("nav.catalog"),
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Recipes,
                        onClick = { tab = Tab.Recipes; recipes.load() },
                        icon = { Icon(Icons.Outlined.GridView, contentDescription = "Recipes tab") },
                        label = { Text("Recipes") },
                        modifier = Modifier.testTag("nav.recipes"),
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Settings,
                        onClick = { tab = Tab.Settings },
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = "Settings tab") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav.settings"),
                    )
                }
            },
        ) { padding ->
            when (tab) {
                Tab.Catalog -> CatalogScreen(catalog, padding, onBatch = { activeBatch = it; recipes.load() })
                Tab.Recipes -> RecipesScreen(
                    viewModel = recipes,
                    padding = padding,
                    onBatch = { activeBatch = it },
                    onCreate = { editorTarget = EditorTarget() },
                    onDeleteBatch = { batch -> recipes.deleteBatch(batch.id) },
                )
                Tab.Settings -> SettingsScreen(settingsViewModel, padding, onCleared = { recipes.load() })
            }
        }
    }
}

@Composable
private fun CatalogScreen(viewModel: CatalogViewModel, padding: PaddingValues, onBatch: (RecipeBatch) -> Unit) {
    val state by viewModel.state.collectAsState()
    val picks by viewModel.dailyPicks.collectAsState()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.generateImage(context.contentResolver.jpegBytesForOpenAI(it)) }
    }

    LaunchedEffect(state) {
        when (val value = state) {
            is CatalogState.Loaded -> {
                onBatch(value.batch)
                viewModel.reset()
            }
            is CatalogState.Error -> error = value.message
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s24),
            verticalArrangement = Arrangement.spacedBy(Space.s24),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().testTag("catalog.dish.field"),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("How to cook...") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { viewModel.generateDish(query) }, modifier = Modifier.testTag("catalog.go.button")) { Text("Go") }
                        },
                    )
                    Text("Enter the name of any dish", style = MaterialTheme.typography.labelMedium, color = secondaryText())
                }
            }
            item { Text("Recipe Catalog", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground) }
            item {
                val cards = listOf(
                    Triple("Breakfast ideas", "Today: ${picks?.breakfast ?: "Loading..."}", MealType.Breakfast),
                    Triple("Lunch ideas", "Today: ${picks?.lunch ?: "Loading..."}", MealType.Lunch),
                    Triple("Dinner ideas", "Today: ${picks?.dinner ?: "Loading..."}", MealType.Dinner),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(292.dp),
                    userScrollEnabled = false,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    items(cards) { card ->
                        CategoryCard(card.first, card.second, Modifier.padding(6.dp).testTag("catalog.card.${card.third.displayName}")) { viewModel.generateMeal(card.third) }
                    }
                    item {
                        CategoryCard("From my\nfridge", "Snap a photo", Modifier.padding(6.dp).testTag("catalog.card.fridge")) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.generateSurprise() },
                        containerColor = terracotta(),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp).testTag("catalog.magic.button"),
                    ) { Icon(Icons.Filled.AutoAwesome, contentDescription = "Surprise me", modifier = Modifier.size(24.dp)) }
                    Text("Can't decide what to cook?\nJust press the button", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = secondaryText())
                }
            }
        }
        if (state == CatalogState.Loading) LoadingOverlay()
    }

    error?.let {
        AlertDialog(onDismissRequest = { error = null; viewModel.reset() }, confirmButton = {
            TextButton(onClick = { error = null; viewModel.reset() }) { Text("OK") }
        }, text = { Text(it) })
    }
}

@Composable
private fun CategoryCard(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(140.dp).fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Space.s16),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = secondaryText(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoadingOverlay() {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pan-rotation",
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s8)) {
            Icon(Icons.Filled.OutdoorGrill, null, Modifier.size(56.dp).rotate(rotation), tint = MaterialTheme.colorScheme.background)
            Text("Cooking up ideas...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
private fun RecipesScreen(
    viewModel: RecipesViewModel,
    padding: PaddingValues,
    onBatch: (RecipeBatch) -> Unit,
    onCreate: () -> Unit,
    onDeleteBatch: (RecipeBatch) -> Unit,
) {
    val batches by viewModel.batches.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val visibleBatches = if (filter == RecipeFilter.All) {
        batches
    } else {
        batches.mapNotNull { batch ->
            val favorites = batch.recipes.filter { it.isFavorite }
            if (favorites.isEmpty()) null else batch.copy(recipes = favorites)
        }
    }
    var pendingDelete by remember { mutableStateOf<RecipeBatch?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        contentPadding = PaddingValues(Space.s24),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recipes", style = MaterialTheme.typography.displayLarge)
                IconButton(onClick = onCreate, modifier = Modifier.testTag("recipes.create.button")) {
                    Icon(Icons.Filled.Add, contentDescription = "New recipe", tint = sage())
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
                FilterButton("All", "recipes.filter.all", filter == RecipeFilter.All) { viewModel.setFilter(RecipeFilter.All) }
                FilterButton("Favorites", "recipes.filter.favorites", filter == RecipeFilter.Favorites) { viewModel.setFilter(RecipeFilter.Favorites) }
            }
        }
        if (visibleBatches.isEmpty()) {
            item { Text("No saved recipe batches yet.", style = MaterialTheme.typography.bodyMedium, color = secondaryText()) }
        } else {
            items(visibleBatches) { batch ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onBatch(batch) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(Modifier.padding(Space.s16), verticalAlignment = Alignment.CenterVertically) {
                        if (batch.recipes.any { it.isFavorite }) {
                            Icon(Icons.Filled.Favorite, null, tint = terracotta(), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(Space.s8))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                            Text(batch.recipes.firstOrNull()?.title ?: "Recipe batch", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("${batch.recipes.size} recipes · ${batch.source.name.lowercase()}", style = MaterialTheme.typography.labelMedium, color = secondaryText())
                        }
                        IconButton(onClick = { pendingDelete = batch }, modifier = Modifier.testTag("recipes.delete.batch")) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete batch", tint = terracotta())
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { batch ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = { onDeleteBatch(batch); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title = { Text("Delete recipe batch?") },
            text = { Text("This permanently removes the saved recipes in this batch.") },
        )
    }
}

@Composable
private fun FilterButton(label: String, tag: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) sage().copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (selected) sage() else secondaryText(),
        ),
    ) { Text(label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeBatchScreen(batch: RecipeBatch, onRecipe: (Recipe) -> Unit, onBack: () -> Unit, onDelete: (Recipe) -> Unit) {
    var pendingDelete by remember { mutableStateOf<Recipe?>(null) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Recipe ideas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(Space.s24),
            verticalArrangement = Arrangement.spacedBy(Space.s12),
        ) {
            item { Text("${batch.recipes.size} recipes", style = MaterialTheme.typography.labelMedium, color = secondaryText()) }
            items(batch.recipes) { recipe -> RecipeCard(recipe, onClick = { onRecipe(recipe) }, onDelete = { pendingDelete = recipe }) }
        }
    }
    pendingDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = { onDelete(recipe); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title = { Text("Delete recipe?") },
            text = { Text("This permanently removes ${recipe.title}.") },
        )
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(BorderStroke(1.dp, rule()), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box {
            Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = secondaryText())
                Row {
                    Text("${recipe.ingredients.size} ingredients", style = MaterialTheme.typography.labelSmall, color = terracotta())
                    Spacer(Modifier.width(Space.s16))
                    Text(recipe.estimatedTime, style = MaterialTheme.typography.labelSmall, color = sage())
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(Space.s8), verticalAlignment = Alignment.CenterVertically) {
                if (recipe.isFavorite) Icon(Icons.Filled.Favorite, null, tint = terracotta(), modifier = Modifier.size(16.dp))
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("batch.delete.recipe")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete recipe", tint = terracotta(), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailScreen(recipe: Recipe, onBack: () -> Unit, onEdit: () -> Unit, onFavorite: (Boolean) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { onFavorite(!recipe.isFavorite) }, modifier = Modifier.testTag("detail.favorite.button")) {
                        Icon(
                            if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite recipe",
                            tint = terracotta(),
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("detail.edit.button")) { Icon(Icons.Filled.Edit, contentDescription = "Edit recipe", tint = sage()) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(Space.s24),
            verticalArrangement = Arrangement.spacedBy(Space.s16),
        ) {
            item { Text(recipe.title, style = MaterialTheme.typography.displayLarge) }
            item { Text(recipe.estimatedTime, style = MaterialTheme.typography.labelMedium, color = sage()) }
            item { Text(recipe.description, style = MaterialTheme.typography.bodyLarge, color = secondaryText()) }
            item { SectionList("Ingredients", recipe.ingredients) }
            item { SectionList("Steps", recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEditRecipeScreen(target: EditorTarget, onCancel: () -> Unit, onSave: (Recipe, String?) -> Unit) {
    val existing = target.recipe
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var ingredients by remember(existing?.id) { mutableStateOf(existing?.ingredients?.ifEmpty { listOf("") } ?: listOf("")) }
    var steps by remember(existing?.id) { mutableStateOf(existing?.steps?.ifEmpty { listOf("") } ?: listOf("")) }
    var estimatedTime by rememberSaveable(existing?.id) { mutableStateOf(existing?.estimatedTime.orEmpty()) }
    val isValid = title.trim().isNotEmpty() && ingredients.any { it.trim().isNotEmpty() } && steps.any { it.trim().isNotEmpty() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (existing == null) "New Recipe" else "Edit Recipe") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    TextButton(
                        enabled = isValid,
                        modifier = Modifier.testTag("create.save.button"),
                        onClick = {
                            onSave(
                                Recipe(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    ingredients = ingredients.map { it.trim() }.filter { it.isNotEmpty() },
                                    steps = steps.map { it.trim() }.filter { it.isNotEmpty() },
                                    estimatedTime = estimatedTime.trim(),
                                    isFavorite = existing?.isFavorite ?: false,
                                    updatedAtEpochMillis = existing?.updatedAtEpochMillis,
                                ),
                                target.batchId,
                            )
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(Space.s24),
            verticalArrangement = Arrangement.spacedBy(Space.s16),
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().testTag("create.title.field"),
                    textStyle = MaterialTheme.typography.titleLarge,
                    label = { Text("Title") },
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().testTag("create.description.field"),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    label = { Text("Description") },
                    minLines = 4,
                    maxLines = 8,
                )
            }
            item {
                DynamicStringSection(
                    title = "Ingredients",
                    values = ingredients,
                    addLabel = "+ Add ingredient",
                    minLines = 1,
                    onValuesChange = { ingredients = it },
                )
            }
            item {
                DynamicStringSection(
                    title = "Steps",
                    values = steps,
                    addLabel = "+ Add step",
                    minLines = 2,
                    onValuesChange = { steps = it },
                )
            }
            item {
                OutlinedTextField(
                    value = estimatedTime,
                    onValueChange = { estimatedTime = it },
                    modifier = Modifier.fillMaxWidth().testTag("create.time.field"),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    label = { Text("Estimated time") },
                    placeholder = { Text("30 min") },
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun DynamicStringSection(
    title: String,
    values: List<String>,
    addLabel: String,
    minLines: Int,
    onValuesChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        values.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onValuesChange(values.toMutableList().also { it[index] = newValue })
                    },
                    modifier = Modifier.weight(1f).testTag("${title.lowercase()}.row.$index.field"),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = minLines,
                )
                IconButton(
                    enabled = values.size > 1,
                    onClick = { onValuesChange(values.toMutableList().also { it.removeAt(index) }) },
                ) { Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = terracotta()) }
            }
        }
        TextButton(onClick = { onValuesChange(values + "") }, modifier = Modifier.testTag("${title.lowercase()}.add")) { Text(addLabel) }
    }
}

@Composable
private fun SectionList(title: String, values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        values.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel, padding: PaddingValues, onCleared: () -> Unit) {
    val theme by viewModel.theme.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        contentPadding = PaddingValues(Space.s24),
        verticalArrangement = Arrangement.spacedBy(Space.s16),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.displayLarge) }
        item { Text("OpenAI key: ${if (viewModel.hasApiKey()) "Configured" else "Missing"}", style = MaterialTheme.typography.bodyMedium, color = secondaryText()) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                Text("Theme", style = MaterialTheme.typography.titleLarge)
                ThemePreference.entries.forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.setTheme(option) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == option, onClick = { viewModel.setTheme(option) })
                        Text(option.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            TextButton(
                onClick = { viewModel.clearRecipes(onCleared) },
                colors = ButtonDefaults.textButtonColors(contentColor = terracotta()),
            ) { Text("Clear all recipes") }
        }
    }
}

private fun ContentResolver.jpegBytesForOpenAI(uri: Uri): ByteArray {
    val source = openInputStream(uri).use { BitmapFactory.decodeStream(it) }
    val maxEdge = max(source.width, source.height)
    val scaled = if (maxEdge > 1024) {
        val scale = 1024f / maxEdge.toFloat()
        Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
    } else {
        source
    }
    return ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, output)
        output.toByteArray()
    }
}

private fun RecipeBatch.replacing(recipe: Recipe): RecipeBatch {
    return copy(recipes = recipes.map { if (it.id == recipe.id) recipe else it })
}

@Composable private fun secondaryText() = if (androidx.compose.foundation.isSystemInDarkTheme()) com.zeekrbaha.fridgechef.ui.theme.InkSoftDark else com.zeekrbaha.fridgechef.ui.theme.InkSoftLight
@Composable private fun rule() = if (androidx.compose.foundation.isSystemInDarkTheme()) RuleDark else RuleLight
@Composable private fun sage() = if (androidx.compose.foundation.isSystemInDarkTheme()) SageDark else SageLight
@Composable private fun terracotta() = if (androidx.compose.foundation.isSystemInDarkTheme()) TerracottaDark else TerracottaLight
