package com.jurassicjournal.data.model

enum class Rarity {
    COMMON, RARE, EPIC, LEGENDARY, UNIQUE, OMEGA, APEX
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

enum class DinoClass {
    CUNNING, CUNNING_FIERCE, CUNNING_RESILIENT, FIERCE, FIERCE_RESILIENT, RESILIENT, WILD_CARD
}

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

enum class HybridType {
    NON_HYBRID, HYBRID, SUPER_MEGA, GIGA_MEGA
}

enum class ProgressionSystem {
    BOOST, TRAINING_POINT
}

enum class MoveTriggerType {
    SELECTABLE, ON_SWAP_IN, ON_ESCAPE, ON_COUNTER, REACTIVE
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

enum class ResearchStatus {
    COMPLETE, PARTIAL, UNRESEARCHED
}

enum class EffectTarget {
    SELF, OPPONENT
}
