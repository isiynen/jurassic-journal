package com.sufficienteffort.jurassicjournal.util

import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.game.repository.IngredientNode
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import kotlin.math.ceil

// ── Level-up cost helpers (shared by the hybrid and level-up calculators) ─────

data class LevelRangeCost(val dna: Long, val coins: Long)

/** Summed DNA and coin cost to go from [fromLevel] up to (and reaching) [toLevel]. */
fun List<LevelUpCost>.rangeCost(fromLevel: Int, toLevel: Int): LevelRangeCost {
    val byFrom = associateBy { it.fromLevel }
    var dna = 0L
    var coins = 0L
    for (level in fromLevel until toLevel) {
        val c = byFrom[level] ?: continue
        dna += c.dnaCost
        coins += c.coinsCost
    }
    return LevelRangeCost(dna, coins)
}

// ── Hybrid fusion model ───────────────────────────────────────────────────────

data class IngredientInput(
    val dino: Dino,
    val depth: Int = 0,
    val parentDinoId: Long? = null,
    val parentDinoName: String? = null,
    val dnaOnHand: Int = 0,
    val parentRarity: Rarity = Rarity.COMMON,
)

data class IngredientCost(
    val dino: Dino,
    val depth: Int = 0,
    val parentDinoId: Long? = null,
    val parentDinoName: String? = null,
    val dnaCostPerFuse: Int,
    val fusesOfParent: Int,
    val totalDnaNeeded: Long,
    val dnaOnHand: Int,
    val dnaDeficit: Long,
    val fusesNeededToProduce: Int = 0,
    val fuseCoinCost: Long = 0L,
)

data class CalcResult(
    val hybridDnaStillNeeded: Long,
    val fusesNeeded: Int,
    val coinsNeeded: Long,
    val coinDeficit: Long,
    val ingredientCosts: List<IngredientCost>,
)

/**
 * Pure fusion/level-up arithmetic for hybrids. No Android or DB dependencies so
 * it can be unit tested; the ViewModel only wires inputs and persistence.
 */
object HybridCostCalculator {

    /** Average DNA produced per fuse. */
    const val DNA_PER_FUSE = 20

    private fun fusionTier(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON    -> 0
        Rarity.RARE      -> 1
        Rarity.EPIC      -> 2
        Rarity.LEGENDARY -> 3
        Rarity.UNIQUE    -> 4
        Rarity.APEX      -> 5
        Rarity.OMEGA     -> -1
    }

    /** Ingredient DNA consumed per fuse, by how many rarity tiers the ingredient sits below the target. */
    fun fuseDnaCost(ingredientRarity: Rarity, targetRarity: Rarity): Int =
        when (fusionTier(targetRarity) - fusionTier(ingredientRarity)) {
            1    -> 50
            2    -> 200
            3    -> 500
            else -> 0
        }

    fun creationDnaCostForRarity(rarity: Rarity): Int = when (rarity) {
        Rarity.RARE      -> 100
        Rarity.EPIC      -> 150
        Rarity.LEGENDARY -> 200
        Rarity.UNIQUE    -> 250
        Rarity.APEX      -> 300
        Rarity.COMMON, Rarity.OMEGA -> 0
    }

    // Coin cost charged per press of the "Fuse" button, by the hybrid's own rarity.
    // Rare/Epic/Legendary/Unique/Apex confirmed. Common is unused in practice —
    // hybrids only exist at Rare rarity and above.
    fun fuseCoinCostForRarity(rarity: Rarity): Long = when (rarity) {
        Rarity.COMMON    -> 20L
        Rarity.RARE      -> 20L
        Rarity.EPIC      -> 100L
        Rarity.LEGENDARY -> 200L
        Rarity.UNIQUE    -> 1_000L
        Rarity.APEX      -> 2_000L
        Rarity.OMEGA     -> 0L
    }

    fun fusesFor(dnaDeficit: Long): Int =
        if (dnaDeficit <= 0L) 0 else ceil(dnaDeficit / DNA_PER_FUSE.toDouble()).toInt()

    // ── Tree helpers ──────────────────────────────────────────────────────────

    /**
     * Flattens the ingredient tree for display: DFS preorder, then stable-sorted
     * by depth so all direct ingredients precede sub-ingredients.
     */
    fun flatten(tree: List<IngredientNode>, rootRarity: Rarity): List<IngredientInput> {
        val out = mutableListOf<IngredientInput>()
        fun walk(nodes: List<IngredientNode>, depth: Int, parent: Dino?, parentRarity: Rarity) {
            for (node in nodes) {
                out += IngredientInput(
                    dino           = node.dino,
                    depth          = depth,
                    parentDinoId   = parent?.id,
                    parentDinoName = parent?.name,
                    parentRarity   = parentRarity,
                )
                walk(node.children, depth + 1, node.dino, node.dino.rarity)
            }
        }
        walk(tree, 0, null, rootRarity)
        return out.sortedBy { it.depth }
    }

    // Recursively build the flat ingredient cost list (DFS preorder) and return total
    // coin cost for all sub-hybrid fuses required to produce the needed DNA.
    private fun buildIngredientCosts(
        tree: List<IngredientNode>,
        fusesOfParent: Int,
        dnaMap: Map<Long, Int>,
        depth: Int,
        parent: Dino?,
        parentRarity: Rarity,
        result: MutableList<IngredientCost>,
    ): Long {
        var totalSubFuseCoins = 0L
        for (node in tree) {
            val costPerFuse    = fuseDnaCost(node.dino.rarity, parentRarity)
            val totalDnaNeeded = fusesOfParent.toLong() * costPerFuse
            val dnaOnHand      = dnaMap[node.dino.id] ?: 0
            val dnaDeficit     = maxOf(0L, totalDnaNeeded - dnaOnHand)
            val fusesNeededToProduce = if (node.dino.isHybrid) fusesFor(dnaDeficit) else 0
            val fuseCoinCost = fusesNeededToProduce.toLong() * fuseCoinCostForRarity(node.dino.rarity)
            totalSubFuseCoins += fuseCoinCost
            result += IngredientCost(
                dino                 = node.dino,
                depth                = depth,
                parentDinoId         = parent?.id,
                parentDinoName       = parent?.name,
                dnaCostPerFuse       = costPerFuse,
                fusesOfParent        = fusesOfParent,
                totalDnaNeeded       = totalDnaNeeded,
                dnaOnHand            = dnaOnHand,
                dnaDeficit           = dnaDeficit,
                fusesNeededToProduce = fusesNeededToProduce,
                fuseCoinCost         = fuseCoinCost,
            )
            if (node.children.isNotEmpty()) {
                totalSubFuseCoins += buildIngredientCosts(
                    tree          = node.children,
                    fusesOfParent = fusesNeededToProduce,
                    dnaMap        = dnaMap,
                    depth         = depth + 1,
                    parent        = node.dino,
                    parentRarity  = node.dino.rarity,
                    result        = result,
                )
            }
        }
        return totalSubFuseCoins
    }

    // Returns total coin cost for all sub-hybrid fuses, or null if a non-hybrid ingredient
    // has insufficient DNA (impossible to produce the needed amount).
    private fun calcSubFuseCoins(
        tree: List<IngredientNode>,
        fusesNeeded: Int,
        dnaAvail: Map<Long, Long>,
        parentRarity: Rarity,
    ): Long? {
        if (fusesNeeded == 0) return 0L
        var totalCoins = 0L
        for (node in tree) {
            val needed  = fusesNeeded.toLong() * fuseDnaCost(node.dino.rarity, parentRarity)
            val have    = dnaAvail.getOrDefault(node.dino.id, 0L)
            val deficit = maxOf(0L, needed - have)
            if (deficit == 0L) continue
            if (!node.dino.isHybrid) return null
            val subFuses = fusesFor(deficit)
            totalCoins += subFuses.toLong() * fuseCoinCostForRarity(node.dino.rarity)
            totalCoins += calcSubFuseCoins(node.children, subFuses, dnaAvail, node.dino.rarity) ?: return null
        }
        return totalCoins
    }

    // Mutates dnaAvail to simulate spending ingredient DNA (and producing sub-hybrid DNA
    // as needed). Only call after calcSubFuseCoins confirms affordability.
    private fun spendIngredientDna(
        tree: List<IngredientNode>,
        fusesNeeded: Int,
        dnaAvail: MutableMap<Long, Long>,
        parentRarity: Rarity,
    ) {
        if (fusesNeeded == 0) return
        for (node in tree) {
            val needed  = fusesNeeded.toLong() * fuseDnaCost(node.dino.rarity, parentRarity)
            val have    = dnaAvail.getOrDefault(node.dino.id, 0L)
            val deficit = maxOf(0L, needed - have)
            if (deficit > 0L && node.dino.isHybrid) {
                val subFuses = fusesFor(deficit)
                spendIngredientDna(node.children, subFuses, dnaAvail, node.dino.rarity)
                dnaAvail[node.dino.id] = have + subFuses * DNA_PER_FUSE.toLong()
            }
            dnaAvail[node.dino.id] = dnaAvail.getOrDefault(node.dino.id, 0L) - needed
        }
    }

    // ── Calculations ──────────────────────────────────────────────────────────

    fun calculateCosts(
        isCreate: Boolean,
        rarity: Rarity,
        currentLevel: Int,
        targetLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        ingredientTree: List<IngredientNode>,
        costs: List<LevelUpCost>,
        coinsOnHand: Long,
    ): CalcResult {
        val levelRange = costs.rangeCost(currentLevel, targetLevel)
        val creationDna = if (isCreate) creationDnaCostForRarity(rarity).toLong() else 0L
        val remainingHybridDna = maxOf(0L, creationDna + levelRange.dna - currentHybridDna)
        val fusesNeeded = fusesFor(remainingHybridDna)
        val fuseCoins = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)

        val dnaMap = ingredients.associate { it.dino.id to it.dnaOnHand }
        val ingredientCostsDfs = mutableListOf<IngredientCost>()
        val subFuseCoins = buildIngredientCosts(
            tree          = ingredientTree,
            fusesOfParent = fusesNeeded,
            dnaMap        = dnaMap,
            depth         = 0,
            parent        = null,
            parentRarity  = rarity,
            result        = ingredientCostsDfs,
        )
        val totalCoins = levelRange.coins + fuseCoins + subFuseCoins

        return CalcResult(
            hybridDnaStillNeeded = remainingHybridDna,
            fusesNeeded          = fusesNeeded,
            coinsNeeded          = totalCoins,
            coinDeficit          = maxOf(0L, totalCoins - coinsOnHand),
            // Stable-sort by depth so depth-0 costs precede depth-1 costs, etc.
            ingredientCosts      = ingredientCostsDfs.sortedBy { it.depth },
        )
    }

    /**
     * Highest level reachable with the current inventory. Returns `currentLevel - 1`
     * when in create mode and the hybrid cannot even be created.
     */
    fun calculateMaxReachableLevel(
        isCreate: Boolean,
        rarity: Rarity,
        currentLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        ingredientTree: List<IngredientNode>,
        coinsOnHand: Long,
        costs: List<LevelUpCost>,
    ): Int {
        val costMap = costs.associateBy { it.fromLevel }
        var hybridDnaAvail = currentHybridDna.toLong()
        val dnaAvail       = ingredients.associate { it.dino.id to it.dnaOnHand.toLong() }.toMutableMap()
        var coinsAvail     = coinsOnHand
        var maxLevel       = currentLevel

        if (isCreate) {
            val creationDnaNeeded = creationDnaCostForRarity(rarity).toLong()
            val fusesNeeded       = fusesFor(creationDnaNeeded - hybridDnaAvail)
            val fuseCoinsNeeded   = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
            val subCoinCost       = calcSubFuseCoins(ingredientTree, fusesNeeded, dnaAvail, rarity) ?: return currentLevel - 1
            if (coinsAvail < fuseCoinsNeeded + subCoinCost) return currentLevel - 1
            coinsAvail -= fuseCoinsNeeded + subCoinCost
            spendIngredientDna(ingredientTree, fusesNeeded, dnaAvail, rarity)
            hybridDnaAvail = hybridDnaAvail + fusesNeeded * DNA_PER_FUSE.toLong() - creationDnaNeeded
        }

        for (fromLevel in currentLevel until StatCalculator.MAX_LEVEL) {
            val cost = costMap[fromLevel] ?: break

            val hybridDnaNeeded  = cost.dnaCost.toLong()
            val fusesNeeded      = fusesFor(hybridDnaNeeded - hybridDnaAvail)
            val fuseCoinsNeeded  = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
            val subCoinCost      = calcSubFuseCoins(ingredientTree, fusesNeeded, dnaAvail, rarity) ?: break
            val totalCoinsNeeded = cost.coinsCost + fuseCoinsNeeded + subCoinCost
            if (coinsAvail < totalCoinsNeeded) break

            coinsAvail -= totalCoinsNeeded
            spendIngredientDna(ingredientTree, fusesNeeded, dnaAvail, rarity)
            // Leftover hybrid DNA carries forward.
            hybridDnaAvail = hybridDnaAvail + fusesNeeded * DNA_PER_FUSE.toLong() - hybridDnaNeeded
            maxLevel = fromLevel + 1
        }

        return maxLevel
    }
}
