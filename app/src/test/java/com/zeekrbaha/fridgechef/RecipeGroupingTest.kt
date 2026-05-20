package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeBatch
import com.zeekrbaha.fridgechef.viewmodel.groupBatches
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecipeGroupingTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 5, 20)
    private val now = today.atTime(10, 0).atZone(zone).toInstant()

    private fun batchOn(date: LocalDate): RecipeBatch = RecipeBatch(
        createdAt = date.atTime(9, 0).atZone(zone).toInstant(),
        recipes = listOf(Recipe(title = "R ${date}", description = "", ingredients = listOf("x"), steps = listOf("y"), estimatedTime = "5 min")),
    )

    @Test
    fun emptyInputProducesNoGroups() {
        assertEquals(emptyList<Any>(), groupBatches(emptyList(), now, zone))
    }

    @Test
    fun bucketsByRecencyWithExpectedTitlesAndOrder() {
        val batches = listOf(
            batchOn(today),                 // TODAY
            batchOn(today.minusDays(1)),    // YESTERDAY
            batchOn(today.minusDays(3)),    // THIS WEEK
            batchOn(today.minusDays(40)),   // a month bucket
        )
        val groups = groupBatches(batches, now, zone)

        assertEquals(listOf("TODAY", "YESTERDAY", "THIS WEEK", "APRIL 2026"), groups.map { it.title })
        assertEquals(1, groups[0].batches.size)
    }

    @Test
    fun monthsAreSortedMostRecentFirst() {
        val batches = listOf(
            batchOn(today.minusDays(40)),   // April 2026
            batchOn(today.minusDays(80)),   // March 2026 (older)
        )
        val groups = groupBatches(batches, now, zone)
        assertEquals(listOf("APRIL 2026", "MARCH 2026"), groups.map { it.title })
    }
}
