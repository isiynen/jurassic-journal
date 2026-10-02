package com.sufficienteffort.jurassicjournal.ui.dino

import com.sufficienteffort.jurassicjournal.data.game.entity.DinoBaseStat
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoResistance
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoSearchResult
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.displayName
import com.sufficienteffort.jurassicjournal.util.StatCalculator

/**
 * Stat sort modes for the dino list. [bucketSize] groups results under range
 * headers; null means one header per distinct value.
 */
enum class StatSortMode(val label: String, val bucketSize: Int?) {
    DAMAGE("Damage", 200),
    HEALTH("Health", 500),
    SPEED("Speed", null),
    ARMOR("Armor", 5),
    CRIT("Crit", 5),
    SANCTUARY("Max SP", null),
    MY_SP("My SP", null),
}

sealed class DinoListItem {
    data class Header(val label: String) : DinoListItem()
    data class Item(val result: DinoSearchResult) : DinoListItem()
}

/** Pure sorting/grouping of search results; no coroutines or Room so it is unit-testable. */
object DinoSorter {

    /** Level 35 with the SP-maximising boost split used for the "Max SP" sort. */
    val MAX_SP_BOOSTS = BoostState(health = 0, attack = 15, speed = 20)
    private const val DEFAULT_LEVEL = 26

    /** Per-profile data needed to evaluate a dino's current stats. */
    data class UserDinoData(val level: Int?, val boosts: BoostState)

    fun sort(
        results: List<DinoSearchResult>,
        statSort: StatSortMode?,
        resistanceSort: ResistanceType?,
        stats: Map<Long, DinoBaseStat>,
        sanctuary: Map<Long, DinoSanctuaryPoint>,
        userData: Map<Long, UserDinoData>,
        resistances: Map<Long, List<DinoResistance>>,
    ): List<DinoListItem> = when {
        statSort != null -> {
            val scored = results.mapNotNull { r ->
                statValue(r, statSort, stats, sanctuary, userData)?.let { r to it }
            }
            group(scored.sortedByDescending { it.second }) { value -> statHeader(statSort, value) }
        }
        resistanceSort != null -> {
            val scored = results.map { r ->
                r to (resistances[r.dino.id]?.firstOrNull { it.resistType == resistanceSort }?.percentage ?: 0)
            }
            group(scored.sortedByDescending { it.second }) { pct -> "${resistanceSort.displayName()} ${(pct / 5) * 5}%" }
        }
        else -> results.map { DinoListItem.Item(it) }
    }

    /** Null means the dino has no value for this mode and is excluded (e.g. non-sanctuary dinos under an SP sort). */
    private fun statValue(
        r: DinoSearchResult,
        mode: StatSortMode,
        stats: Map<Long, DinoBaseStat>,
        sanctuary: Map<Long, DinoSanctuaryPoint>,
        userData: Map<Long, UserDinoData>,
    ): Int? {
        val user = userData[r.dino.id]
        val boosts = user?.boosts ?: BoostState()
        return when (mode) {
            StatSortMode.SANCTUARY -> sanctuary[r.dino.id]?.let {
                StatCalculator.calculateSp(it.spSad, StatCalculator.MAX_LEVEL, MAX_SP_BOOSTS)
            }
            StatSortMode.MY_SP -> sanctuary[r.dino.id]?.let {
                StatCalculator.calculateSp(it.spSad, user?.level ?: DEFAULT_LEVEL, boosts)
            }
            else -> {
                val base = stats[r.dino.id] ?: return 0
                val isOmega = r.dino.progressionSystem == ProgressionSystem.TRAINING_POINT
                val level = if (isOmega) DEFAULT_LEVEL else user?.level ?: DEFAULT_LEVEL
                when (mode) {
                    StatSortMode.DAMAGE -> {
                        val scaled = if (isOmega) base.baseAttack else StatCalculator.scaleStat(base.baseAttack, level)
                        StatCalculator.applyPercentBoost(scaled, boosts.attack)
                    }
                    StatSortMode.HEALTH -> {
                        val scaled = if (isOmega) base.baseHealth else StatCalculator.scaleStat(base.baseHealth, level)
                        StatCalculator.applyPercentBoost(scaled, boosts.health)
                    }
                    StatSortMode.SPEED -> StatCalculator.applySpeedBoost(base.speed, boosts.speed)
                    StatSortMode.ARMOR -> base.armor.toInt()
                    StatSortMode.CRIT  -> base.critChance.toInt()
                    StatSortMode.SANCTUARY, StatSortMode.MY_SP -> null
                }
            }
        }
    }

    private fun statHeader(mode: StatSortMode, value: Int): String {
        val bucketSize = mode.bucketSize ?: return "${mode.label} $value"
        val bucket = (value / bucketSize) * bucketSize
        return when (mode) {
            StatSortMode.DAMAGE, StatSortMode.HEALTH -> {
                val lo = if (bucket == 0) 1 else bucket
                "${mode.label} $lo–${bucket + bucketSize - 1}"
            }
            else -> "${mode.label} $bucket%"
        }
    }

    /** Emits a header whenever the header label changes between consecutive (already sorted) entries. */
    private fun group(
        sorted: List<Pair<DinoSearchResult, Int>>,
        headerFor: (Int) -> String,
    ): List<DinoListItem> {
        val items = ArrayList<DinoListItem>(sorted.size + 16)
        var currentHeader: String? = null
        for ((result, value) in sorted) {
            val header = headerFor(value)
            if (header != currentHeader) {
                currentHeader = header
                items += DinoListItem.Header(header)
            }
            items += DinoListItem.Item(result)
        }
        return items
    }
}
