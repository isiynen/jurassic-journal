package com.sufficienteffort.jurassicjournal.util

import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.game.repository.IngredientNode
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Test

class HybridCostCalculatorTest {

    private fun dino(id: Long, rarity: Rarity, isHybrid: Boolean = false) = Dino(
        id = id, slug = "d$id", name = "Dino $id", description = "",
        rarity = rarity, dinoClass = DinoClass.FIERCE,
        hybridType = if (isHybrid) HybridType.HYBRID else HybridType.NON_HYBRID,
        imagePath = "", isHybrid = isHybrid, sanctuaryEligible = false,
    )

    // Legendary hybrid (id 1) made from two epics (ids 2, 3).
    private val legendary = dino(1, Rarity.LEGENDARY, isHybrid = true)
    private val tree = listOf(
        IngredientNode(dino(2, Rarity.EPIC)),
        IngredientNode(dino(3, Rarity.EPIC)),
    )

    /** Legendary level-up costs 16→35: flat 100 DNA and 1000 coins per level for easy arithmetic. */
    private val costs = (16 until 35).map { LevelUpCost(Rarity.LEGENDARY, it, it + 1, coinsCost = 1000, dnaCost = 100) }

    @Test
    fun `fuse dna cost depends on rarity gap`() {
        assertEquals(50, HybridCostCalculator.fuseDnaCost(Rarity.EPIC, Rarity.LEGENDARY))
        assertEquals(200, HybridCostCalculator.fuseDnaCost(Rarity.RARE, Rarity.LEGENDARY))
        assertEquals(500, HybridCostCalculator.fuseDnaCost(Rarity.COMMON, Rarity.LEGENDARY))
        assertEquals(0, HybridCostCalculator.fuseDnaCost(Rarity.LEGENDARY, Rarity.LEGENDARY))
    }

    @Test
    fun `fuses round up at twenty dna each`() {
        assertEquals(0, HybridCostCalculator.fusesFor(0))
        assertEquals(1, HybridCostCalculator.fusesFor(1))
        assertEquals(10, HybridCostCalculator.fusesFor(200))
        assertEquals(11, HybridCostCalculator.fusesFor(201))
    }

    @Test
    fun `rangeCost sums the levels in between`() {
        val range = costs.rangeCost(16, 20)
        assertEquals(400L, range.dna)
        assertEquals(4000L, range.coins)
        assertEquals(LevelRangeCost(0, 0), costs.rangeCost(20, 20))
    }

    @Test
    fun `creating a legendary from nothing needs ten fuses and ingredient dna`() {
        val result = HybridCostCalculator.calculateCosts(
            isCreate = true, rarity = Rarity.LEGENDARY,
            currentLevel = 16, targetLevel = 16,
            currentHybridDna = 0,
            ingredients = HybridCostCalculator.flatten(tree, Rarity.LEGENDARY),
            ingredientTree = tree, costs = costs, coinsOnHand = 0,
        )
        assertEquals(200L, result.hybridDnaStillNeeded)      // creation cost
        assertEquals(10, result.fusesNeeded)                 // 200 / 20
        assertEquals(10L * 200, result.coinsNeeded)          // 10 fuses × 200 coins
        assertEquals(2, result.ingredientCosts.size)
        result.ingredientCosts.forEach {
            assertEquals(500L, it.totalDnaNeeded)            // 10 fuses × 50 DNA
            assertEquals(500L, it.dnaDeficit)
        }
    }

    @Test
    fun `dna on hand reduces fuses and coin deficit`() {
        val result = HybridCostCalculator.calculateCosts(
            isCreate = false, rarity = Rarity.LEGENDARY,
            currentLevel = 16, targetLevel = 18,               // 200 DNA, 2000 coins
            currentHybridDna = 150,
            ingredients = HybridCostCalculator.flatten(tree, Rarity.LEGENDARY),
            ingredientTree = tree, costs = costs, coinsOnHand = 1000,
        )
        assertEquals(50L, result.hybridDnaStillNeeded)
        assertEquals(3, result.fusesNeeded)
        assertEquals(2000L + 3 * 200, result.coinsNeeded)
        assertEquals(1600L, result.coinDeficit)
    }

    @Test
    fun `max reachable level stops when coins run out`() {
        val ingredients = HybridCostCalculator.flatten(tree, Rarity.LEGENDARY)
            .map { it.copy(dnaOnHand = 100_000) }
        val maxLevel = HybridCostCalculator.calculateMaxReachableLevel(
            isCreate = false, rarity = Rarity.LEGENDARY,
            currentLevel = 16, currentHybridDna = 0,
            ingredients = ingredients, ingredientTree = tree,
            coinsOnHand = 2 * (1000 + 5 * 200) + 500,       // exactly two levels plus change
            costs = costs,
        )
        assertEquals(18, maxLevel)
    }

    @Test
    fun `max reachable level reaches 35 with unlimited resources`() {
        val ingredients = HybridCostCalculator.flatten(tree, Rarity.LEGENDARY)
            .map { it.copy(dnaOnHand = 1_000_000) }
        val maxLevel = HybridCostCalculator.calculateMaxReachableLevel(
            isCreate = true, rarity = Rarity.LEGENDARY,
            currentLevel = 16, currentHybridDna = 0,
            ingredients = ingredients, ingredientTree = tree,
            coinsOnHand = Long.MAX_VALUE / 2, costs = costs,
        )
        assertEquals(35, maxLevel)
    }

    @Test
    fun `create mode reports one below current when ingredients are missing`() {
        val maxLevel = HybridCostCalculator.calculateMaxReachableLevel(
            isCreate = true, rarity = Rarity.LEGENDARY,
            currentLevel = 16, currentHybridDna = 0,
            ingredients = HybridCostCalculator.flatten(tree, Rarity.LEGENDARY),
            ingredientTree = tree, coinsOnHand = Long.MAX_VALUE / 2, costs = costs,
        )
        assertEquals(15, maxLevel)
    }

    @Test
    fun `flatten lists direct ingredients before sub-ingredients`() {
        val sub = dino(4, Rarity.RARE)
        val epicHybrid = dino(2, Rarity.EPIC, isHybrid = true)
        val nested = listOf(
            IngredientNode(epicHybrid, listOf(IngredientNode(sub))),
            IngredientNode(dino(3, Rarity.EPIC)),
        )
        val flat = HybridCostCalculator.flatten(nested, Rarity.LEGENDARY)
        assertEquals(listOf(2L, 3L, 4L), flat.map { it.dino.id })
        assertEquals(listOf(0, 0, 1), flat.map { it.depth })
        assertEquals(epicHybrid.name, flat[2].parentDinoName)
        assertEquals(Rarity.EPIC, flat[2].parentRarity)
    }
}
