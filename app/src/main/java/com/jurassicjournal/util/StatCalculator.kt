package com.jurassicjournal.util

import kotlin.math.roundToInt

/**
 * JWA stat scaling and boost formulas.
 *
 * Level scaling (confirmed from official update notes and community sources):
 *   Levels 1–30 : stat_L26 * 1.05^(level - 26), compounding 5% per level
 *   Levels 31–35: stat_L26 * fixed multiplier (1.270 / 1.320 / 1.370 / 1.425 / 1.500)
 *   Speed, armor and crit chance are flat — they do not scale with level.
 *   (Speed only changes via boosts or Omega training points.)
 *
 * Boosts (Stat Boosts 2.0):
 *   Health / Attack : +2.5% of stat-at-level per tier (percentage-based)
 *   Speed           : +2 flat points per tier
 *   Max tiers/stat  : 20
 *   Max total tiers : min(30, level), requires level >= 10
 */
object StatCalculator {

    // Fixed multipliers vs level-26 baseline for levels 31–35
    private val HIGH_LEVEL_MULTIPLIERS = floatArrayOf(1.270f, 1.320f, 1.370f, 1.425f, 1.500f)

    const val BOOST_UNLOCK_LEVEL = 10
    const val MAX_BOOST_TIERS_TOTAL = 30
    const val MAX_BOOST_TIERS_PER_STAT = 20
    const val HEALTH_ATTACK_BOOST_PCT = 0.025f   // +2.5% per tier
    const val SPEED_BOOST_FLAT = 2               // +2 speed per tier

    /** Stat value at the given level (health, attack, or speed). */
    fun scaleStat(baseAtL26: Int, level: Int): Int {
        require(level in 1..35) { "Level must be 1–35" }
        val multiplier = when {
            level <= 30 -> Math.pow(1.05, (level - 26).toDouble()).toFloat()
            else        -> HIGH_LEVEL_MULTIPLIERS[level - 31]
        }
        return (baseAtL26 * multiplier).roundToInt()
    }

    /** Maximum total boost tiers available at this creature level. */
    fun maxTotalBoosts(level: Int): Int =
        if (level < BOOST_UNLOCK_LEVEL) 0
        else level

    /** Health after applying boost tiers (percentage-based). */
    fun applyHealthBoost(statAtLevel: Int, tiers: Int): Int =
        (statAtLevel * (1f + tiers * HEALTH_ATTACK_BOOST_PCT)).roundToInt()

    /** Attack after applying boost tiers (percentage-based). */
    fun applyAttackBoost(statAtLevel: Int, tiers: Int): Int =
        (statAtLevel * (1f + tiers * HEALTH_ATTACK_BOOST_PCT)).roundToInt()

    /** Speed after applying boost tiers (flat additive). */
    fun applySpeedBoost(statAtLevel: Int, tiers: Int): Int =
        statAtLevel + tiers * SPEED_BOOST_FLAT

    // ── Omega training points ────────────────────────────────────────────────

    // Omega dinos earn 7 training points per level (confirmed from game update notes).
    // Stats do NOT use level scaling — training points are the sole progression mechanism.
    const val OMEGA_POINTS_PER_LEVEL = 7

    /** Total training points available at this Omega creature level. */
    fun maxOmegaTrainingPoints(level: Int): Int = level * OMEGA_POINTS_PER_LEVEL

    /** Omega stat after applying allocated training points, capped at maxCap. */
    fun applyOmegaTraining(baseStat: Int, pointsAllocated: Int, gainPerPoint: Int, maxCap: Int): Int =
        minOf(baseStat + pointsAllocated * gainPerPoint, maxCap)
}
