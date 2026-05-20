package com.zeekrbaha.fridgechef.viewmodel

import com.zeekrbaha.fridgechef.data.RecipeBatch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RecipeGroup(val title: String, val batches: List<RecipeBatch>)

/**
 * Applies the Recipes-tab filter. Favorites keeps only batches that contain at least
 * one favorited recipe, trimming each batch down to just its favorited recipes.
 */
fun applyFilter(batches: List<RecipeBatch>, filter: RecipeFilter): List<RecipeBatch> {
    if (filter == RecipeFilter.All) return batches
    return batches.mapNotNull { batch ->
        val favorites = batch.recipes.filter { it.isFavorite }
        if (favorites.isEmpty()) null else batch.copy(recipes = favorites)
    }
}

/**
 * Groups batches into the Recipes-tab sections used by the iOS reference:
 * TODAY / YESTERDAY / THIS WEEK, then one bucket per older month ("MONTH YYYY"),
 * months sorted most-recent first. Input is assumed newest-first; order is preserved
 * within each bucket.
 */
fun groupBatches(
    batches: List<RecipeBatch>,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): List<RecipeGroup> {
    if (batches.isEmpty()) return emptyList()

    val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    val startOfYesterday = startOfToday.minus(Duration.ofDays(1))
    val startOfWeek = startOfToday.minus(Duration.ofDays(7))
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    val buckets = LinkedHashMap<String, MutableList<RecipeBatch>>()
    for (batch in batches) {
        val key = when {
            !batch.createdAt.isBefore(startOfToday) -> "TODAY"
            !batch.createdAt.isBefore(startOfYesterday) -> "YESTERDAY"
            !batch.createdAt.isBefore(startOfWeek) -> "THIS WEEK"
            else -> batch.createdAt.atZone(zone).format(monthFormatter).uppercase(Locale.US)
        }
        buckets.getOrPut(key) { mutableListOf() }.add(batch)
    }

    val fixedOrder = listOf("TODAY", "YESTERDAY", "THIS WEEK")
    val groups = mutableListOf<RecipeGroup>()
    for (key in fixedOrder) {
        buckets[key]?.let { groups.add(RecipeGroup(key, it)) }
    }
    buckets.keys
        .filter { it !in fixedOrder }
        .sortedByDescending { key -> buckets[key]?.maxOfOrNull { it.createdAt } ?: Instant.MIN }
        .forEach { groups.add(RecipeGroup(it, buckets.getValue(it))) }
    return groups
}
