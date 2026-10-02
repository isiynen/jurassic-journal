package com.sufficienteffort.jurassicjournal.ui.dino

import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoBaseStat
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoResistance
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoSearchResult
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import org.junit.Assert.assertEquals
import org.junit.Test

class DinoSorterTest {

    private fun result(id: Long) = DinoSearchResult(
        dino = Dino(
            id = id, slug = "d$id", name = "Dino $id", description = "",
            rarity = Rarity.EPIC, dinoClass = DinoClass.RESILIENT, hybridType = HybridType.NON_HYBRID,
            imagePath = "", isHybrid = false, sanctuaryEligible = true,
        ),
        matchedMoves = emptyList(),
    )

    private val results = listOf(result(1), result(2), result(3))
    private val stats = mapOf(
        1L to DinoBaseStat(1, baseHealth = 3000, baseAttack = 1000, speed = 120, armor = 0f, critChance = 5f),
        2L to DinoBaseStat(2, baseHealth = 4000, baseAttack = 1200, speed = 110, armor = 30f, critChance = 20f),
        3L to DinoBaseStat(3, baseHealth = 4000, baseAttack = 1500, speed = 120, armor = 0f, critChance = 5f),
    )

    private fun sort(
        mode: StatSortMode? = null,
        resistance: ResistanceType? = null,
        user: Map<Long, DinoSorter.UserDinoData> = emptyMap(),
        sanctuary: Map<Long, DinoSanctuaryPoint> = emptyMap(),
        resistances: Map<Long, List<DinoResistance>> = emptyMap(),
    ) = DinoSorter.sort(results, mode, resistance, stats, sanctuary, user, resistances)

    private fun ids(items: List<DinoListItem>) = items.filterIsInstance<DinoListItem.Item>().map { it.result.dino.id }
    private fun headers(items: List<DinoListItem>) = items.filterIsInstance<DinoListItem.Header>().map { it.label }

    @Test
    fun `no sort keeps input order without headers`() {
        val out = sort()
        assertEquals(listOf(1L, 2L, 3L), ids(out))
        assertEquals(emptyList<String>(), headers(out))
    }

    @Test
    fun `speed sort groups by exact value descending`() {
        val out = sort(StatSortMode.SPEED)
        assertEquals(listOf("Speed 120", "Speed 110"), headers(out))
        assertEquals(listOf(1L, 3L, 2L), ids(out))
    }

    @Test
    fun `user boosts change the sorted speed`() {
        val user = mapOf(2L to DinoSorter.UserDinoData(level = 26, boosts = BoostState(speed = 10)))
        val out = sort(StatSortMode.SPEED, user = user)
        assertEquals(listOf("Speed 130", "Speed 120"), headers(out))
        assertEquals(2L, ids(out).first())
    }

    @Test
    fun `damage sort buckets by 200 at level 26`() {
        val out = sort(StatSortMode.DAMAGE)
        assertEquals(listOf("Damage 1400–1599", "Damage 1200–1399", "Damage 1000–1199"), headers(out))
        assertEquals(listOf(3L, 2L, 1L), ids(out))
    }

    @Test
    fun `armor sort buckets by five percent`() {
        val out = sort(StatSortMode.ARMOR)
        assertEquals(listOf("Armor 30%", "Armor 0%"), headers(out))
    }

    @Test
    fun `sanctuary sort drops dinos without sp data`() {
        val sanctuary = mapOf(
            1L to DinoSanctuaryPoint(1, spMaxBoost = 0, spBaseline = 0, spSad = 125.0),
            3L to DinoSanctuaryPoint(3, spMaxBoost = 0, spBaseline = 0, spSad = 250.0),
        )
        val out = sort(StatSortMode.SANCTUARY, sanctuary = sanctuary)
        assertEquals(listOf(3L, 1L), ids(out))
        assertEquals(2, headers(out).size)
    }

    @Test
    fun `resistance sort orders by percentage and buckets by five`() {
        val resistances = mapOf(
            1L to listOf(DinoResistance(1, ResistanceType.STUN, 100)),
            2L to listOf(DinoResistance(2, ResistanceType.STUN, 33)),
        )
        val out = sort(resistance = ResistanceType.STUN, resistances = resistances)
        assertEquals(listOf("Stun 100%", "Stun 30%", "Stun 0%"), headers(out))
        assertEquals(listOf(1L, 2L, 3L), ids(out))
    }
}
