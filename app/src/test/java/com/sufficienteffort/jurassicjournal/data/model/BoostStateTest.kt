package com.sufficienteffort.jurassicjournal.data.model

import com.sufficienteffort.jurassicjournal.data.user.entity.UserBoost
import org.junit.Assert.assertEquals
import org.junit.Test

class BoostStateTest {

    @Test
    fun `maxFor respects per-stat cap and remaining total`() {
        val b = BoostState(health = 10, attack = 10, speed = 5)
        // total 25; cap 30 leaves 5 more for any stat on top of its current value
        assertEquals(15, b.maxFor(BoostStat.HEALTH, 30))
        assertEquals(10, b.maxFor(BoostStat.SPEED, 30))
        // per-stat cap of 20 wins when there is plenty of room
        assertEquals(20, b.maxFor(BoostStat.HEALTH, 100))
    }

    @Test
    fun `clampedTo trims speed first then attack then health`() {
        val b = BoostState(health = 10, attack = 10, speed = 10)
        assertEquals(BoostState(10, 10, 5), b.clampedTo(25))
        assertEquals(BoostState(10, 5, 0), b.clampedTo(15))
        assertEquals(BoostState(0, 0, 0), b.clampedTo(0))
        assertEquals(b, b.clampedTo(30))
    }

    @Test
    fun `rows round trip`() {
        val original = BoostState(health = 3, attack = 7, speed = 20)
        val rows = original.toRows(profileId = 2, dinoId = 99)
        assertEquals(3, rows.size)
        assertEquals(setOf("health", "attack", "speed"), rows.map { it.stat }.toSet())
        assertEquals(original, BoostState.fromRows(rows))
    }

    @Test
    fun `fromRows ignores unknown stats and defaults missing ones to zero`() {
        val rows = listOf(UserBoost(1, 1, "speed", 4), UserBoost(1, 1, "mystery", 9))
        assertEquals(BoostState(speed = 4), BoostState.fromRows(rows))
    }
}
