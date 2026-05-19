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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OutdoorGrill
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
import com.zeekrbaha.fridgechef.viewmodel.RecipesViewModel
import com.zeekrbaha.fridgechef.viewmodel.SettingsViewModel
import java.io.ByteArrayOutputStream
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

@Composable
private fun FridgeChefApp(factory: FridgeChefViewModelFactory, settingsViewModel: SettingsViewModel) {
    val catalog: CatalogViewModel = viewModel(factory = factory)
    val recipes: RecipesViewModel = viewModel(factory = factory)
    var tab by rememberSaveable { mutableStateOf(Tab.Catalog) }
    var activeBatch by remember { mutableStateOf<RecipeBatch?>(null) }
    var activeRecipe by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(Unit) {
        catalog.refreshDailyPicksIfStale()
        recipes.load()
    }

    when {
        activeRecipe != null -> RecipeDetailScreen(activeRecipe!!, onBack = { activeRecipe = null })
        activeBatch != null -> RecipeBatchScreen(activeBatch!!, onRecipe = { activeRecipe = it }, onBack = { activeBatch = null })
        else -> Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(selected = tab == Tab.Catalog, onClick = { tab = Tab.Catalog }, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Catalog") })
                    NavigationBarItem(selected = tab == Tab.Recipes, onClick = { tab = Tab.Recipes; recipes.load() }, icon = { Icon(Icons.Outlined.GridView, null) }, label = { Text("Recipes") })
                    NavigationBarItem(selected = tab == Tab.Settings, onClick = { tab = Tab.Settings }, icon = { Icon(Icons.Outlined.Tune, null) }, label = { Text("Settings") })
                }
            },
        ) { padding ->
            when (tab) {
                Tab.Catalog -> CatalogScreen(catalog, padding, onBatch = { activeBatch = it; recipes.load() })
                Tab.Recipes -> RecipesScreen(recipes, padding, onBatch = { activeBatch = it })
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
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("How to cook...") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { viewModel.generateDish(query) }) { Text("Go") }
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
                        CategoryCard(card.first, card.second, Modifier.padding(6.dp)) { viewModel.generateMeal(card.third) }
                    }
                    item {
                        CategoryCard("From my\nfridge", "Snap a photo", Modifier.padding(6.dp)) {
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
                        modifier = Modifier.size(56.dp),
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
private fun RecipesScreen(viewModel: RecipesViewModel, padding: PaddingValues, onBatch: (RecipeBatch) -> Unit) {
    val batches by viewModel.batches.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        contentPadding = PaddingValues(Space.s24),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item { Text("Recipes", style = MaterialTheme.typography.displayLarge) }
        if (batches.isEmpty()) {
            item { Text("No saved recipe batches yet.", style = MaterialTheme.typography.bodyMedium, color = secondaryText()) }
        } else {
            items(batches) { batch ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onBatch(batch) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                        Text(batch.recipes.firstOrNull()?.title ?: "Recipe batch", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("${batch.recipes.size} recipes", style = MaterialTheme.typography.labelMedium, color = secondaryText())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeBatchScreen(batch: RecipeBatch, onRecipe: (Recipe) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Recipe ideas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(Space.s24),
            verticalArrangement = Arrangement.spacedBy(Space.s12),
        ) {
            item { Text("3 recipes generated for you", style = MaterialTheme.typography.labelMedium, color = secondaryText()) }
            items(batch.recipes) { recipe -> RecipeCard(recipe, onClick = { onRecipe(recipe) }) }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(BorderStroke(1.dp, rule()), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
            Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = secondaryText())
            Row {
                Text("${recipe.ingredients.size} ingredients", style = MaterialTheme.typography.labelSmall, color = terracotta())
                Spacer(Modifier.width(Space.s16))
                Text(recipe.estimatedTime, style = MaterialTheme.typography.labelSmall, color = sage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailScreen(recipe: Recipe, onBack: () -> Unit) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = {}, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
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

@Composable private fun secondaryText() = if (androidx.compose.foundation.isSystemInDarkTheme()) com.zeekrbaha.fridgechef.ui.theme.InkSoftDark else com.zeekrbaha.fridgechef.ui.theme.InkSoftLight
@Composable private fun rule() = if (androidx.compose.foundation.isSystemInDarkTheme()) RuleDark else RuleLight
@Composable private fun sage() = if (androidx.compose.foundation.isSystemInDarkTheme()) SageDark else SageLight
@Composable private fun terracotta() = if (androidx.compose.foundation.isSystemInDarkTheme()) TerracottaDark else TerracottaLight
