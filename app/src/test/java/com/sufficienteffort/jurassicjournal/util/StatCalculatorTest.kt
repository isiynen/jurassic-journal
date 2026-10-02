package com.sufficienteffort.jurassicjournal.util

import com.sufficienteffort.jurassicjournal.data.model.BoostState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StatCalculatorTest {

    @Test
    fun `level 26 is the baseline`() {
        assertEquals(3000, StatCalculator.scaleStat(3000, 26))
    }

    @Test
    fun `levels below 30 compound five percent per level`() {
        assertEquals((3000 * 1.05 * 1.05).toInt(), StatCalculator.scaleStat(3000, 28))
        assertEquals((3000 / 1.05).toInt(), StatCalculator.scaleStat(3000, 25))
    }

    @Test
    fun `levels 31 to 35 use the original float multipliers`() {
        // Mirrors the historical arithmetic exactly (float table widened to double, then truncated).
        assertEquals((1000 * 1.270f.toDouble()).toInt(), StatCalculator.scaleStat(1000, 31))
        assertEquals((1000 * 1.425f.toDouble()).toInt(), StatCalculator.scaleStat(1000, 34))
        assertEquals(1500, StatCalculator.scaleStat(1000, 35))
    }

    @Test
    fun `out of range level throws`() {
        assertThrows(IllegalArgumentException::class.java) { StatCalculator.scaleStat(100, 0) }
        assertThrows(IllegalArgumentException::class.java) { StatCalculator.scaleStat(100, 36) }
    }

    @Test
    fun `boost cap is zero before level 10 then equals level`() {
        assertEquals(0, StatCalculator.maxTotalBoosts(9))
        assertEquals(10, StatCalculator.maxTotalBoosts(10))
        assertEquals(35, StatCalculator.maxTotalBoosts(35))
    }

    @Test
    fun `percent boost adds two and a half percent per tier`() {
        assertEquals(1500, StatCalculator.applyPercentBoost(1000, 20))
        assertEquals(1000, StatCalculator.applyPercentBoost(1000, 0))
    }

    @Test
    fun `speed boost adds two per tier`() {
        assertEquals(150, StatCalculator.applySpeedBoost(110, 20))
    }

    @Test
    fun `omega training caps at maxCap`() {
        assertEquals(120, StatCalculator.applyOmegaTraining(100, 10, 5, 120))
        assertEquals(110, StatCalculator.applyOmegaTraining(100, 2, 5, 120))
    }

    @Test
    fun `sanctuary points at level 26 unboosted equals sad over 1_25`() {
        assertEquals(100, StatCalculator.calculateSp(125.0, 26, 0, 0, 0))
    }

    @Test
    fun `sanctuary boost overload matches explicit tiers`() {
        val boosts = BoostState(health = 5, attack = 5, speed = 20)
        assertEquals(
            StatCalculator.calculateSp(125.0, 30, 5, 5, 20),
            StatCalculator.calculateSp(125.0, 30, boosts),
        )
        // 1 + 10*0.0125 + 20*0.02 = 1.525
        assertEquals((100 * 1.05 * 1.05 * 1.05 * 1.05 * 1.525).toInt(), StatCalculator.calculateSp(125.0, 30, boosts))
    }
}
