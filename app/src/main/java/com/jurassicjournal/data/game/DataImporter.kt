package com.jurassicjournal.data.game

import android.content.Context
import androidx.room.withTransaction
import com.jurassicjournal.data.game.dao.DinoBaseStatDao
import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.dao.DinoHybridIngredientDao
import com.jurassicjournal.data.game.dao.DinoMoveDao
import com.jurassicjournal.data.game.dao.DinoResistanceDao
import com.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.jurassicjournal.data.game.dao.DinoSpawnLocationDao
import com.jurassicjournal.data.game.dao.LevelUpCostDao
import com.jurassicjournal.data.game.dao.MoveDao
import com.jurassicjournal.data.game.dao.OmegaTrainingConfigDao
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.entity.DinoBaseStat
import com.jurassicjournal.data.game.entity.DinoHybridIngredient
import com.jurassicjournal.data.game.entity.DinoMove
import com.jurassicjournal.data.game.entity.DinoResistance
import com.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.jurassicjournal.data.game.entity.DinoSpawnLocation
import com.jurassicjournal.data.game.entity.LevelUpCost
import com.jurassicjournal.data.game.entity.Move
import com.jurassicjournal.data.game.entity.OmegaTrainingConfig
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.HybridType
import com.jurassicjournal.data.model.MovePriorityType
import com.jurassicjournal.data.model.MoveTriggerType
import com.jurassicjournal.data.model.MoveUnlockType
import com.jurassicjournal.data.model.ProgressionSystem
import com.jurassicjournal.data.model.Rarity
import com.jurassicjournal.data.model.ResistanceType
import com.jurassicjournal.data.model.SpawnLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: GameDatabase,
    private val dinoDao: DinoDao,
    private val dinoBaseStatDao: DinoBaseStatDao,
    private val dinoResistanceDao: DinoResistanceDao,
    private val dinoSpawnLocationDao: DinoSpawnLocationDao,
    private val moveDao: MoveDao,
    private val dinoMoveDao: DinoMoveDao,
    private val dinoHybridIngredientDao: DinoHybridIngredientDao,
    private val omegaTrainingConfigDao: OmegaTrainingConfigDao,
    private val dinoSanctuaryPointDao: DinoSanctuaryPointDao,
    private val levelUpCostDao: LevelUpCostDao,
) {
    suspend fun importIfEmpty() {
        if (dinoDao.count() > 0) return

        val abilitiesJson = context.assets.open("abilities.json").bufferedReader().readText()
        val dinosJson = context.assets.open("dinos.json").bufferedReader().readText()

        val abilitiesObj = JSONObject(abilitiesJson)
        val dinosArr = JSONArray(dinosJson)

        db.withTransaction {
            importMoves(abilitiesObj)
            importDinos(dinosArr)
        }
        importSanctuaryPoints()
        importLevelUpCosts()
    }

    private suspend fun importMoves(abilitiesObj: JSONObject) {
        val moves = mutableListOf<Move>()
        val keys = abilitiesObj.keys()
        while (keys.hasNext()) {
            val slug = keys.next()
            val a = abilitiesObj.getJSONObject(slug)
            val iconsObj = a.optJSONObject("icons")
            val mainIcon = iconsObj?.optString("main")?.takeIf { it.isNotEmpty() }
            val overlayIcons = buildOverlayIconsJson(iconsObj)

            val variants = a.getJSONObject("variants")
            val normal = variants.getJSONObject("normal")
            val threatened = variants.optJSONObject("threatened")

            moves += Move(
                slug = slug,
                name = a.getString("name"),
                trigger = parseTrigger(a.getString("trigger")),
                mainIconPath = mainIcon,
                overlayIconsJson = overlayIcons,
                priority = parsePriority(normal.optString("priority", "")),
                cooldown = normal.optInt("cooldown", 0),
                delay = normal.optInt("delay", 0),
                normalTargetsJson = normal.getJSONArray("targets").toString(),
                threatenedPriority = threatened?.let { parsePriority(it.optString("priority", "")) },
                threatenedCooldown = threatened?.optInt("cooldown"),
                threatenedDelay = threatened?.optInt("delay"),
                threatenedTargetsJson = threatened?.getJSONArray("targets")?.toString(),
            )
        }
        moveDao.insertAll(moves)
    }

    private suspend fun importDinos(dinosArr: JSONArray) {
        val dinos = mutableListOf<Dino>()
        val stats = mutableListOf<DinoBaseStat>()
        val resistances = mutableListOf<DinoResistance>()
        val spawnLocations = mutableListOf<DinoSpawnLocation>()
        val dinoMoves = mutableListOf<DinoMove>()
        val omegaConfigs = mutableListOf<OmegaTrainingConfig>()
        val ingredientSlugs = mutableMapOf<String, List<String>>()

        for (i in 0 until dinosArr.length()) {
            val d = dinosArr.getJSONObject(i)
            val id = (i + 1).toLong()
            val slug = d.getString("slug")
            val rarity = parseRarity(d.getString("rarity"))
            val sources = d.getJSONArray("dna_sources")

            val sanctuaryEligible = (0 until sources.length()).any {
                sources.getJSONObject(it).getString("loc") == "sanctuary"
            }

            val hybridType = parseHybridType(d.optString("hybrid_type", "non_hybrid"))
            val isHybrid = hybridType != HybridType.NON_HYBRID

            dinos += Dino(
                id = id,
                slug = slug,
                name = d.getString("name"),
                description = d.optString("description", ""),
                rarity = rarity,
                dinoClass = parseDinoClass(d.getString("class")),
                hybridType = hybridType,
                imagePath = d.getString("image").removePrefix("images/"),
                isHybrid = isHybrid,
                sanctuaryEligible = sanctuaryEligible,
                progressionSystem = if (rarity == Rarity.OMEGA) ProgressionSystem.TRAINING_POINT
                                    else ProgressionSystem.BOOST,
            )

            val s = d.getJSONObject("stats")
            stats += DinoBaseStat(
                dinoId = id,
                baseHealth = s.getInt("health"),
                baseAttack = s.getInt("attack"),
                speed = s.getInt("speed"),
                armor = s.getDouble("armor").toFloat(),
                critChance = s.getDouble("crit_chance").toFloat(),
                critMultiplier = s.optDouble("crit_multiplier", 125.0).toFloat(),
            )

            val r = d.optJSONObject("resistances")
            if (r != null) {
                for (key in RESISTANCE_KEY_MAP.keys) {
                    val pct = r.optInt(key, 0)
                    if (pct > 0) {
                        resistances += DinoResistance(
                            dinoId = id,
                            resistType = RESISTANCE_KEY_MAP[key]!!,
                            percentage = pct,
                        )
                    }
                }
            }

            for (j in 0 until sources.length()) {
                val src = sources.getJSONObject(j)
                val loc = parseSpawnLocation(src.getString("loc")) ?: continue
                val timeArr = src.optJSONArray("time")
                val timeOfDay = if (timeArr != null) {
                    (0 until timeArr.length()).joinToString(",") { timeArr.getString(it) }
                } else null
                spawnLocations += DinoSpawnLocation(
                    dinoId = id,
                    location = loc,
                    timeOfDay = timeOfDay,
                )
            }

            val abilities = d.getJSONObject("abilities")
            var slot = 0
            for ((jsonKey, triggerType) in MOVE_ARRAY_KEYS) {
                val arr = abilities.optJSONArray(jsonKey) ?: continue
                for (k in 0 until arr.length()) {
                    dinoMoves += DinoMove(
                        dinoId = id,
                        moveSlug = arr.getString(k),
                        slotOrder = slot++,
                        triggerType = triggerType,
                        unlockType = MoveUnlockType.DEFAULT,
                    )
                }
            }

            // Omega training configs — only present for Omega rarity dinos
            val omegaTraining = d.optJSONObject("omega_training")
            if (omegaTraining != null) {
                val gainPerPoint = omegaTraining.optJSONObject("gain_per_point")
                val pointCap    = omegaTraining.optJSONObject("point_cap")
                val maxCap      = omegaTraining.optJSONObject("max_cap")
                if (gainPerPoint != null && pointCap != null && maxCap != null) {
                    for (stat in OMEGA_STAT_KEYS) {
                        val cap = pointCap.optInt(stat, 0)
                        if (cap > 0) {
                            omegaConfigs += OmegaTrainingConfig(
                                dinoId = id,
                                stat = stat,
                                gainPerPoint = gainPerPoint.optInt(stat, 0),
                                pointCap = cap,
                                maxCap = maxCap.optInt(stat, 0),
                            )
                        }
                    }
                }
            }

            val ingrArr = d.optJSONArray("hybrid_ingredients")
            if (ingrArr != null && ingrArr.length() > 0) {
                ingredientSlugs[slug] = (0 until ingrArr.length()).map { ingrArr.getString(it) }
            }
        }

        dinoDao.insertAll(dinos)
        dinoBaseStatDao.insertAll(stats)
        dinoResistanceDao.insertAll(resistances)
        dinoSpawnLocationDao.insertAll(spawnLocations)
        dinoMoveDao.insertAll(dinoMoves)
        omegaTrainingConfigDao.insertAll(omegaConfigs)

        if (ingredientSlugs.isNotEmpty()) {
            val slugToId = dinoDao.getAllSlugIds().associate { it.slug to it.id }
            val ingredients = mutableListOf<DinoHybridIngredient>()
            for ((hybridSlug, ingrs) in ingredientSlugs) {
                val hybridId = slugToId[hybridSlug] ?: continue
                for (ingrSlug in ingrs) {
                    val ingrId = slugToId[ingrSlug] ?: continue
                    ingredients += DinoHybridIngredient(
                        hybridDinoId = hybridId,
                        ingredientDinoId = ingrId,
                        dnaCostPerFuse = 0,
                    )
                }
            }
            dinoHybridIngredientDao.insertAll(ingredients)
        }
    }

    private suspend fun importSanctuaryPoints() {
        val json = context.assets.open("sanctuary_points.json").bufferedReader().readText()
        val arr = JSONArray(json)
        val nameToId = dinoDao.getAllNameIds()
            .associate { normalizeName(it.name) to it.id }
        val points = mutableListOf<DinoSanctuaryPoint>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rawName = obj.getString("name")
            val dinoId = nameToId[normalizeName(rawName)] ?: continue
            points += DinoSanctuaryPoint(
                dinoId = dinoId,
                spMaxBoost = obj.getInt("sp_max"),
                spBaseline = obj.getInt("sp_baseline"),
                spSad = obj.getDouble("sad"),
            )
        }
        dinoSanctuaryPointDao.insertAll(points)
    }

    private fun normalizeName(name: String): String =
        name.lowercase().replace("gen2", "gen 2").replace(Regex("\\s+"), " ").trim()

    private suspend fun importLevelUpCosts() {
        val json = context.assets.open("level_up_costs.json").bufferedReader().readText()
        val root = JSONObject(json)

        // Coin costs are rarity-agnostic (except Omega has separate omega_coins track).
        // Build a fromLevel → regular coins map and fromLevel → omega coins map.
        val coinsArr = root.getJSONArray("coins_by_level")
        val regularCoins = mutableMapOf<Int, Int>()
        for (i in 0 until coinsArr.length()) {
            val obj = coinsArr.getJSONObject(i)
            regularCoins[obj.getInt("from")] = obj.getInt("coins")
        }

        val omegaCoinsArr = root.getJSONArray("omega_coins_by_level")
        val omegaCoins = mutableMapOf<Int, Int>()
        for (i in 0 until omegaCoinsArr.length()) {
            val obj = omegaCoinsArr.getJSONObject(i)
            omegaCoins[obj.getInt("from")] = obj.getInt("coins")
        }

        val rarityKeyMap = mapOf(
            "COMMON"    to Rarity.COMMON,
            "RARE"      to Rarity.RARE,
            "EPIC"      to Rarity.EPIC,
            "LEGENDARY" to Rarity.LEGENDARY,
            "UNIQUE"    to Rarity.UNIQUE,
            "APEX"      to Rarity.APEX,
            "OMEGA"     to Rarity.OMEGA,
        )

        val costs = mutableListOf<LevelUpCost>()
        val dnaByRarity = root.getJSONObject("dna_by_rarity")
        for ((key, rarity) in rarityKeyMap) {
            val dnaArr = dnaByRarity.getJSONArray(key)
            for (i in 0 until dnaArr.length()) {
                val obj = dnaArr.getJSONObject(i)
                val from = obj.getInt("from")
                val coinsMap = if (rarity == Rarity.OMEGA) omegaCoins else regularCoins
                costs += LevelUpCost(
                    rarity    = rarity,
                    fromLevel = from,
                    toLevel   = from + 1,
                    coinsCost = coinsMap[from] ?: 0,
                    dnaCost   = obj.getInt("dna"),
                )
            }
        }
        levelUpCostDao.insertAll(costs)
    }

    private fun buildOverlayIconsJson(iconsObj: JSONObject?): String? {
        if (iconsObj == null) return null
        val overlays = JSONArray()
        val keys = iconsObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "main") continue
            val entry = iconsObj.optJSONObject(key) ?: continue
            val overlay = JSONObject()
            overlay.put("key", key)
            overlay.put("path", entry.optString("path"))
            overlay.put("position", entry.optString("position"))
            overlays.put(overlay)
        }
        return if (overlays.length() > 0) overlays.toString() else null
    }

    private fun parseTrigger(s: String): MoveTriggerType = when (s) {
        "Regular"   -> MoveTriggerType.SELECTABLE
        "Swap In"   -> MoveTriggerType.ON_SWAP_IN
        "Counter"   -> MoveTriggerType.ON_COUNTER
        "On Escape" -> MoveTriggerType.ON_ESCAPE
        "Reactive"  -> MoveTriggerType.REACTIVE
        else        -> MoveTriggerType.SELECTABLE
    }

    private fun parsePriority(s: String): MovePriorityType = when (s) {
        "priority" -> MovePriorityType.PRIORITY
        "act last" -> MovePriorityType.LAST
        else       -> MovePriorityType.NORMAL
    }

    private fun parseRarity(s: String): Rarity = when (s.lowercase()) {
        "common"    -> Rarity.COMMON
        "rare"      -> Rarity.RARE
        "epic"      -> Rarity.EPIC
        "legendary" -> Rarity.LEGENDARY
        "unique"    -> Rarity.UNIQUE
        "omega"     -> Rarity.OMEGA
        "apex"      -> Rarity.APEX
        else        -> Rarity.COMMON
    }

    private fun parseDinoClass(s: String): DinoClass = when (s.lowercase()) {
        "cunning"           -> DinoClass.CUNNING
        "cunning_fierce"    -> DinoClass.CUNNING_FIERCE
        "cunning_resilient" -> DinoClass.CUNNING_RESILIENT
        "fierce"            -> DinoClass.FIERCE
        "fierce_resilient"  -> DinoClass.FIERCE_RESILIENT
        "resilient"         -> DinoClass.RESILIENT
        "wild_card"         -> DinoClass.WILD_CARD
        else                -> DinoClass.WILD_CARD
    }

    private fun parseHybridType(s: String): HybridType = when (s) {
        "hybrid"       -> HybridType.HYBRID
        "super_hybrid" -> HybridType.SUPER_MEGA
        "mega_hybrid"  -> HybridType.GIGA_MEGA
        else           -> HybridType.NON_HYBRID
    }

    private fun parseSpawnLocation(s: String): SpawnLocation? = when (s) {
        "none"                     -> null
        "sanctuary"                -> SpawnLocation.SANCTUARY
        "everywhere"               -> SpawnLocation.EVERYWHERE
        "everywhere_monday"        -> SpawnLocation.EVERYWHERE_MONDAY
        "everywhere_tuesday"       -> SpawnLocation.EVERYWHERE_TUESDAY
        "everywhere_wednesday"     -> SpawnLocation.EVERYWHERE_WEDNESDAY
        "everywhere_thursday"      -> SpawnLocation.EVERYWHERE_THURSDAY
        "everywhere_friday"        -> SpawnLocation.EVERYWHERE_FRIDAY
        "everywhere_saturday"      -> SpawnLocation.EVERYWHERE_SATURDAY
        "everywhere_sunday"        -> SpawnLocation.EVERYWHERE_SUNDAY
        "park"                     -> SpawnLocation.PARK
        "local_area_1"             -> SpawnLocation.LOCAL_AREA_1
        "local_area_2"             -> SpawnLocation.LOCAL_AREA_2
        "local_area_3"             -> SpawnLocation.LOCAL_AREA_3
        "local_area_4"             -> SpawnLocation.LOCAL_AREA_4
        "short_range"              -> SpawnLocation.SHORT_RANGE
        "continent_AF/AN/AS/OC/US" -> SpawnLocation.CONTINENT_ASIA
        "continent_EU/US"          -> SpawnLocation.CONTINENT_EUROPE
        "continent_NA/SA/US"       -> SpawnLocation.CONTINENT_AMERICAS
        "raid"                     -> SpawnLocation.RAID
        "arena"                    -> SpawnLocation.ARENA
        "strike_towers"            -> SpawnLocation.STRIKE_TOWERS
        "isla_events"              -> SpawnLocation.ISLA_EVENTS
        "alliance_missions"        -> SpawnLocation.ALLIANCE_MISSIONS
        "pass"                     -> SpawnLocation.PASS
        else                       -> null
    }

    companion object {
        val OMEGA_STAT_KEYS = listOf("health", "attack", "speed", "armor", "crit_chance", "crit_multiplier")

        private val RESISTANCE_KEY_MAP = mapOf(
            "crit"                to ResistanceType.CRIT_REDUCTION,
            "bleed"               to ResistanceType.DOT,
            "dot"                 to ResistanceType.DOT,
            "damage_decrease"     to ResistanceType.DAMAGE_DECREASE,
            "rend"                to ResistanceType.REND,
            "armor_decrease"      to ResistanceType.REDUCED_ARMOR,
            "speed_decrease"      to ResistanceType.SPEED_DECREASE,
            "stun"                to ResistanceType.STUN,
            "swap_prevention"     to ResistanceType.SWAP_PREVENTION,
            "taunt"               to ResistanceType.TAUNT,
            "vulnerability"       to ResistanceType.VULNERABLE,
            "resistance_decrease" to ResistanceType.RESISTANCE_DECREASE,
            "heal_decrease"       to ResistanceType.HEAL_DECREASE,
            "daze"                to ResistanceType.DAZE,
        )

        private val MOVE_ARRAY_KEYS = listOf(
            "moves"           to MoveTriggerType.SELECTABLE,
            "moves_swap_in"   to MoveTriggerType.ON_SWAP_IN,
            "moves_counter"   to MoveTriggerType.ON_COUNTER,
            "moves_on_escape" to MoveTriggerType.ON_ESCAPE,
            "moves_reactive"  to MoveTriggerType.REACTIVE,
        )
    }
}
