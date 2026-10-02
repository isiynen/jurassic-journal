package com.sufficienteffort.jurassicjournal.data.model

import com.sufficienteffort.jurassicjournal.data.user.entity.UserBoost
import com.sufficienteffort.jurassicjournal.util.StatCalculator

/** Boost tiers applied to a single dino. Shared by the detail screen, list sorting and the sanctuary planner. */
data class BoostState(
    val health: Int = 0,
    val attack: Int = 0,
    val speed: Int = 0,
) {
    val total: Int get() = health + attack + speed

    operator fun get(stat: BoostStat): Int = when (stat) {
        BoostStat.HEALTH -> health
        BoostStat.ATTACK -> attack
        BoostStat.SPEED  -> speed
    }

    fun with(stat: BoostStat, tiers: Int): BoostState = when (stat) {
        BoostStat.HEALTH -> copy(health = tiers)
        BoostStat.ATTACK -> copy(attack = tiers)
        BoostStat.SPEED  -> copy(speed = tiers)
    }

    /** Highest value [stat] may take without exceeding the per-stat cap or [totalCap]. */
    fun maxFor(stat: BoostStat, totalCap: Int): Int =
        minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, totalCap - (total - this[stat]))

    /** Trims to [cap] total tiers, keeping health first, then attack, then speed. */
    fun clampedTo(cap: Int): BoostState {
        var rem = cap.coerceAtLeast(0)
        val h = minOf(health, rem).also { rem -= it }
        val a = minOf(attack, rem).also { rem -= it }
        val s = minOf(speed,  rem)
        return BoostState(h, a, s)
    }

    fun toRows(profileId: Long, dinoId: Long): List<UserBoost> =
        BoostStat.entries.map { UserBoost(profileId, dinoId, it.dbKey, this[it]) }

    companion object {
        fun fromRows(rows: Collection<UserBoost>): BoostState {
            fun tiers(stat: BoostStat) = rows.firstOrNull { it.stat == stat.dbKey }?.boostsApplied ?: 0
            return BoostState(
                health = tiers(BoostStat.HEALTH),
                attack = tiers(BoostStat.ATTACK),
                speed  = tiers(BoostStat.SPEED),
            )
        }
    }
}
