package com.sufficienteffort.jurassicjournal.util

import com.sufficienteffort.jurassicjournal.data.model.BoostState
import kotlin.math.floor
import kotlin.math.pow

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
 *   Max total tiers : equal to the creature level (so 35 at level 35), unlocks at level 10.
 *                     Enhancements can raise the total cap further.
 */
object StatCalculator {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 35

    // Fixed multipliers vs level-26 baseline for levels 31–35.
    // Float on purpose: stat display has always used this exact arithmetic, and
    // switching to doubles shifts some level 31–35 stats by one point.
    private val HIGH_LEVEL_MULTIPLIERS = floatArrayOf(1.270f, 1.320f, 1.370f, 1.425f, 1.500f)

    // Same breakpoints as doubles, used by the sanctuary-point formula only.
    private val HIGH_LEVEL_SP_MULTIPLIERS = doubleArrayOf(1.270, 1.320, 1.370, 1.425, 1.500)

    const val BOOST_UNLOCK_LEVEL = 10
    const val MAX_BOOST_TIERS_PER_STAT = 20
    const val HEALTH_ATTACK_BOOST_PCT = 0.025   // +2.5% per tier
    const val SPEED_BOOST_FLAT = 2              // +2 speed per tier

    /** Stat value at the given level (health or attack). */
    fun scaleStat(baseAtL26: Int, level: Int): Int {
        require(level in MIN_LEVEL..MAX_LEVEL) { "Level must be $MIN_LEVEL–$MAX_LEVEL" }
        val multiplier = when {
            level <= 30 -> Math.pow(1.05, (level - 26).toDouble())
            else        -> HIGH_LEVEL_MULTIPLIERS[level - 31].toDouble()
        }
        return (baseAtL26 * multiplier).toInt()
    }

    /** Level multiplier for the sanctuary-point formula. */
    private fun spLevelMultiplier(level: Int): Double {
        require(level in MIN_LEVEL..MAX_LEVEL) { "Level must be $MIN_LEVEL–$MAX_LEVEL" }
        return if (level <= 30) 1.05.pow((level - 26).toDouble()) else HIGH_LEVEL_SP_MULTIPLIERS[level - 31]
    }

    /** Maximum total boost tiers available at this creature level (before enhancement bonuses). */
    fun maxTotalBoosts(level: Int): Int = if (level < BOOST_UNLOCK_LEVEL) 0 else level

    /** Health or attack after applying boost tiers (percentage-based). */
    fun applyPercentBoost(statAtLevel: Int, tiers: Int): Int =
        (statAtLevel * (1.0 + tiers * HEALTH_ATTACK_BOOST_PCT)).toInt()

    fun applyHealthBoost(statAtLevel: Int, tiers: Int): Int = applyPercentBoost(statAtLevel, tiers)
    fun applyAttackBoost(statAtLevel: Int, tiers: Int): Int = applyPercentBoost(statAtLevel, tiers)

    /** Speed after applying boost tiers (flat additive). */
    fun applySpeedBoost(statAtLevel: Int, tiers: Int): Int = statAtLevel + tiers * SPEED_BOOST_FLAT

    // ── Omega training points ────────────────────────────────────────────────

    // Omega dinos earn 7 training points per level (confirmed from game update notes).
    // Stats do NOT use level scaling — training points are the sole progression mechanism.
    const val OMEGA_POINTS_PER_LEVEL = 7

    /** Total training points available at this Omega creature level. */
    fun maxOmegaTrainingPoints(level: Int): Int = level * OMEGA_POINTS_PER_LEVEL

    /** Omega stat after applying allocated training points, capped at maxCap. */
    fun applyOmegaTraining(baseStat: Int, pointsAllocated: Int, gainPerPoint: Int, maxCap: Int): Int =
        minOf(baseStat + pointsAllocated * gainPerPoint, maxCap)

    // ── Sanctuary Points ─────────────────────────────────────────────────────────

    // SP boost multipliers: +1.25% per health/attack tier, +2% per speed tier.
    const val SP_HEALTH_ATTACK_PCT = 0.0125
    const val SP_SPEED_PCT = 0.02

    /** Estimated SP per sanctuary action at the given level and boost configuration. */
    fun calculateSp(spSad: Double, level: Int, healthBoosts: Int, attackBoosts: Int, speedBoosts: Int): Int {
        val spBase = spSad / 1.25
        val boostMult = 1.0 + (healthBoosts + attackBoosts) * SP_HEALTH_ATTACK_PCT + speedBoosts * SP_SPEED_PCT
        return floor(spBase * spLevelMultiplier(level) * boostMult).toInt()
    }

    fun calculateSp(spSad: Double, level: Int, boosts: BoostState): Int =
        calculateSp(spSad, level, boosts.health, boosts.attack, boosts.speed)
}
