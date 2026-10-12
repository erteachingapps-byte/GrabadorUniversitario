package com.edwin.grabador

import org.junit.Assert.*
import org.junit.Test

class GroqUploadPlanTest {
    @Test fun fullClassGroupsTenSegmentsIntoFourRequests() {
        assertEquals(listOf(listOf(0,1,2), listOf(3,4,5), listOf(6,7,8), listOf(9)), GroqUploadPlan.batches(List(10) { 7_200_000L }))
    }
    @Test fun sizeLimitSplitsBeforeOverflowAndKeepsOrder() {
        val sizes = listOf(12_000_000L,12_000_000L,1L,23_000_000L,2_000_000L)
        val groups = GroqUploadPlan.batches(sizes)
        assertEquals(sizes.indices.toList(), groups.flatten())
        assertTrue(groups.all { g -> g.size <= 3 && g.sumOf { sizes[it] } <= GroqUploadPlan.BUDGET })
        assertEquals(listOf(0,1), groups.first())
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedAudioIsNeverUploaded() { GroqUploadPlan.batches(listOf(24_000_001L)) }
    @Test(expected = IllegalArgumentException::class) fun emptyAudioIsNeverUploaded() { GroqUploadPlan.batches(listOf(0L)) }
    @Test fun emptySessionHasNoRequests() { assertTrue(GroqUploadPlan.batches(emptyList()).isEmpty()) }
    @Test fun fractionalRetryAfterRoundsUp() { assertTrue(GroqLimits.message("61.1").contains("2 minuto(s)")) }
    @Test fun invalidRetryAfterDoesNotInventCountdown() {
        listOf(null, "bad", "NaN", "Infinity", "-1", "0").forEach { assertTrue(GroqLimits.message(it).contains("renueve el límite")) }
    }
}
