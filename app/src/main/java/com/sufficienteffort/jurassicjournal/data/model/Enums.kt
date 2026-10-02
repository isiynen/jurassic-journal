package com.sufficienteffort.jurassicjournal.data.model

enum class Rarity {
    COMMON, RARE, EPIC, LEGENDARY, UNIQUE, OMEGA, APEX
}

/** Default calculator level: 26 (server display default), raised where minLevel exceeds it. */
fun Rarity.defaultLevel(): Int = maxOf(26, minLevel())

fun Rarity.maxDna(): Int = when (this) {
    Rarity.COMMON    -> 850_000
    Rarity.RARE      -> 250_000
    Rarity.EPIC      ->  85_000
    Rarity.LEGENDARY ->  25_000
    Rarity.UNIQUE    ->   8_000
    Rarity.APEX      ->   3_000
    Rarity.OMEGA     ->  60_000
}

fun Rarity.minLevel(): Int = when (this) {
    Rarity.COMMON    -> 1
    Rarity.RARE      -> 6
    Rarity.EPIC      -> 11
    Rarity.LEGENDARY -> 16
    Rarity.UNIQUE    -> 21
    Rarity.APEX      -> 26
    Rarity.OMEGA     -> 1
}

/** "Legendary", "Apex", … */
fun Rarity.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

enum class DinoClass {
    CUNNING, CUNNING_FIERCE, CUNNING_RESILIENT, FIERCE, FIERCE_RESILIENT, RESILIENT, WILD_CARD
}

/** "Cunning Fierce", "Wild Card", … */
fun DinoClass.label(): String = name.lowercase().split('_')
    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

enum class SpawnLocation {
    NONE,
    LOCAL_AREA_1, LOCAL_AREA_2, LOCAL_AREA_3, LOCAL_AREA_4,
    PARK,
    CONTINENT_ASIA, CONTINENT_EUROPE, CONTINENT_AMERICAS,
    SHORT_RANGE,
    EVERYWHERE,
    EVERYWHERE_MONDAY, EVERYWHERE_TUESDAY, EVERYWHERE_WEDNESDAY,
    EVERYWHERE_THURSDAY, EVERYWHERE_FRIDAY, EVERYWHERE_SATURDAY, EVERYWHERE_SUNDAY,
    RAID, ARENA, STRIKE_TOWERS, ISLA_EVENTS, ALLIANCE_MISSIONS, PASS, SANCTUARY
}

fun SpawnLocation.displayName(): String = when (this) {
    SpawnLocation.LOCAL_AREA_1          -> "Zone 1"
    SpawnLocation.LOCAL_AREA_2          -> "Zone 2"
    SpawnLocation.LOCAL_AREA_3          -> "Zone 3"
    SpawnLocation.LOCAL_AREA_4          -> "Zone 4"
    SpawnLocation.PARK                  -> "Park"
    SpawnLocation.RAID                  -> "Raid"
    SpawnLocation.SANCTUARY             -> "Sanctuary"
    SpawnLocation.EVERYWHERE            -> "Everywhere"
    SpawnLocation.EVERYWHERE_MONDAY     -> "Everywhere (Monday)"
    SpawnLocation.EVERYWHERE_TUESDAY    -> "Everywhere (Tuesday)"
    SpawnLocation.EVERYWHERE_WEDNESDAY  -> "Everywhere (Wednesday)"
    SpawnLocation.EVERYWHERE_THURSDAY   -> "Everywhere (Thursday)"
    SpawnLocation.EVERYWHERE_FRIDAY     -> "Everywhere (Friday)"
    SpawnLocation.EVERYWHERE_SATURDAY   -> "Everywhere (Saturday)"
    SpawnLocation.EVERYWHERE_SUNDAY     -> "Everywhere (Sunday)"
    SpawnLocation.SHORT_RANGE           -> "Short Range"
    SpawnLocation.CONTINENT_ASIA        -> "Asia / Oceania"
    SpawnLocation.CONTINENT_EUROPE      -> "Europe"
    SpawnLocation.CONTINENT_AMERICAS    -> "Americas"
    SpawnLocation.ARENA                 -> "Arena"
    SpawnLocation.STRIKE_TOWERS         -> "Strike Towers"
    SpawnLocation.ISLA_EVENTS           -> "Isla Events"
    SpawnLocation.ALLIANCE_MISSIONS     -> "Alliance Missions"
    SpawnLocation.PASS                  -> "Pass"
    SpawnLocation.NONE                  -> ""
}

enum class HybridType {
    NON_HYBRID, HYBRID, SUPER_MEGA, GIGA_MEGA
}

enum class ProgressionSystem {
    BOOST, TRAINING_POINT
}

/**
 * The three boostable stats. [dbKey] is the value stored in `user_boosts.stat`
 * and must never change; [label] is the short UI caption.
 */
enum class BoostStat(val dbKey: String, val label: String) {
    HEALTH("health", "HP"),
    ATTACK("attack", "ATK"),
    SPEED("speed", "SPD");

    companion object {
        fun fromKey(key: String): BoostStat? = entries.firstOrNull { it.dbKey == key }
    }
}

/**
 * Omega training stats. [dbKey] matches `omega_training_configs.stat` and
 * `omega_training_allocations.stat`; [isFlat] stats show "+N", the rest "+N%".
 * Declaration order is the display order.
 */
enum class OmegaStat(val dbKey: String, val label: String, val isFlat: Boolean) {
    HEALTH("health", "HP", true),
    ATTACK("attack", "ATK", true),
    SPEED("speed", "SPD", true),
    ARMOR("armor", "ARM", false),
    CRIT_CHANCE("crit_chance", "CRIT %", false),
    CRIT_MULTIPLIER("crit_multiplier", "CRIT DMG", false);

    companion object {
        fun fromKey(key: String): OmegaStat? = entries.firstOrNull { it.dbKey == key }
    }
}

enum class MoveTriggerType {
    SELECTABLE, ON_SWAP_IN, ON_ESCAPE, ON_COUNTER, REACTIVE
}

fun MoveTriggerType.label(): String = when (this) {
    MoveTriggerType.SELECTABLE -> "Moves"
    MoveTriggerType.ON_SWAP_IN -> "Swap-In"
    MoveTriggerType.ON_COUNTER -> "Counter"
    MoveTriggerType.ON_ESCAPE  -> "On Escape"
    MoveTriggerType.REACTIVE   -> "Reactive"
}

enum class MovePriorityType {
    NORMAL, PRIORITY, LAST
}

enum class MoveUnlockType {
    DEFAULT, PURCHASE, LEVEL, ENHANCEMENT
}

enum class EnhancementType {
    STAT_BONUS, MOVE_UNLOCK, PASSIVE
}

enum class CatalystType {
    BRONZE, SILVER, GOLD
}

enum class ResistanceType {
    CRIT_REDUCTION, DOT, DAMAGE_DECREASE, REND, REDUCED_ARMOR,
    SPEED_DECREASE, STUN, SWAP_PREVENTION, TAUNT, VULNERABLE,
    RESISTANCE_DECREASE, HEAL_DECREASE, DAZE
}

fun ResistanceType.displayName(): String = when (this) {
    ResistanceType.CRIT_REDUCTION      -> "Crit Reduction"
    ResistanceType.DOT                 -> "DoT"
    ResistanceType.DAMAGE_DECREASE     -> "Damage Decrease"
    ResistanceType.REND                -> "Rend"
    ResistanceType.REDUCED_ARMOR       -> "Armor Decrease"
    ResistanceType.SPEED_DECREASE      -> "Speed Decrease"
    ResistanceType.STUN                -> "Stun"
    ResistanceType.SWAP_PREVENTION     -> "Swap Prevention"
    ResistanceType.TAUNT               -> "Taunt"
    ResistanceType.VULNERABLE          -> "Vulnerability"
    ResistanceType.RESISTANCE_DECREASE -> "Resistance Decrease"
    ResistanceType.HEAL_DECREASE       -> "Heal Decrease"
    ResistanceType.DAZE                -> "Daze"
}

enum class ResearchStatus {
    COMPLETE, PARTIAL, UNRESEARCHED
}

enum class EffectTarget {
    SELF, OPPONENT
}
