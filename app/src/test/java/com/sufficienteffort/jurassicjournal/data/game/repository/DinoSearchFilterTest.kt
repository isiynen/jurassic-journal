package com.sufficienteffort.jurassicjournal.data.game.repository

import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DinoSearchFilterTest {

    private fun result(id: Long, name: String, rarity: Rarity, moves: List<String>, isNew: Boolean = false) =
        DinoSearchResult(
            dino = Dino(
                id = id, slug = name.lowercase(), name = name, description = "",
                rarity = rarity, dinoClass = DinoClass.CUNNING, hybridType = HybridType.NON_HYBRID,
                imagePath = "", isHybrid = false, sanctuaryEligible = true,
            ),
            matchedMoves = moves,
            isNew = isNew,
        )

    private val all = listOf(
        result(1, "Velociraptor", Rarity.COMMON, listOf("Pounce", "Strike")),
        result(2, "Indoraptor", Rarity.UNIQUE, listOf("Cautious Strike", "Definite Rampage")),
        result(3, "Thoradolosaur", Rarity.UNIQUE, listOf("Fierce Impact"), isNew = true),
    )

    private fun search(
        query: String = "",
        rarities: Set<Rarity> = emptySet(),
        locations: Set<SpawnLocation> = emptySet(),
        spawnMap: Map<Long, Set<SpawnLocation>> = emptyMap(),
    ) = DinoSearchFilter.apply(all, query, rarities, emptySet(), locations, spawnMap)

    @Test
    fun `blank query keeps everything and clears matched moves`() {
        val out = search()
        assertEquals(3, out.size)
        assertTrue(out.all { it.matchedMoves.isEmpty() })
    }

    @Test
    fun `new dinos float to the top`() {
        assertEquals(3L, search().first().dino.id)
    }

    @Test
    fun `multi-word query requires every word in name or moves`() {
        val out = search("raptor strike")
        assertEquals(listOf(1L, 2L), out.map { it.dino.id })
        // only the moves that explain the non-name word are shown
        assertEquals(listOf("Strike"), out[0].matchedMoves)
        assertEquals(listOf("Cautious Strike"), out[1].matchedMoves)
    }

    @Test
    fun `name-only match shows no move chips`() {
        val out = search("indo")
        assertEquals(listOf(2L), out.map { it.dino.id })
        assertTrue(out[0].matchedMoves.isEmpty())
    }

    @Test
    fun `quoted query is an exact phrase`() {
        assertEquals(listOf(2L), search("\"cautious strike\"").map { it.dino.id })
        assertTrue(search("\"strike cautious\"").isEmpty())
        assertTrue(DinoSearchFilter.isStrictQuery("'x'"))
        assertFalse(DinoSearchFilter.isStrictQuery("\""))
    }

    @Test
    fun `rarity filter applies before the query`() {
        assertEquals(listOf(2L), search("strike", rarities = setOf(Rarity.UNIQUE)).map { it.dino.id })
    }

    @Test
    fun `location filter requires every selected location`() {
        val spawn = mapOf(
            1L to setOf(SpawnLocation.LOCAL_AREA_1, SpawnLocation.PARK),
            2L to setOf(SpawnLocation.LOCAL_AREA_1),
        )
        val both = setOf(SpawnLocation.LOCAL_AREA_1, SpawnLocation.PARK)
        assertEquals(listOf(1L), search(locations = both, spawnMap = spawn).map { it.dino.id })
        assertEquals(listOf(1L, 2L), search(locations = setOf(SpawnLocation.LOCAL_AREA_1), spawnMap = spawn).map { it.dino.id })
    }
}
