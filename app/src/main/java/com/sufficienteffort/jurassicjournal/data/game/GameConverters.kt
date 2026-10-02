package com.sufficienteffort.jurassicjournal.data.game

import android.util.Log
import androidx.room.TypeConverter
import com.sufficienteffort.jurassicjournal.data.model.CatalystType
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.EffectTarget
import com.sufficienteffort.jurassicjournal.data.model.EnhancementType
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import com.sufficienteffort.jurassicjournal.data.model.MoveUnlockType
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.ResearchStatus
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation

/**
 * Enum parse that survives forward-compat: an OTA data patch (same schema,
 * newer content) may carry enum strings this APK doesn't know yet. valueOf()
 * would crash every read of that row; degrade to a sane default instead.
 */
internal inline fun <reified T : Enum<T>> enumOrDefault(v: String, default: T): T =
    try {
        enumValueOf<T>(v)
    } catch (e: IllegalArgumentException) {
        Log.w("GameConverters", "Unknown ${T::class.simpleName} value '$v' — using $default")
        default
    }

class GameConverters {
    @TypeConverter fun fromRarity(v: Rarity): String = v.name
    @TypeConverter fun toRarity(v: String): Rarity = enumOrDefault(v, Rarity.COMMON)

    @TypeConverter fun fromDinoClass(v: DinoClass): String = v.name
    @TypeConverter fun toDinoClass(v: String): DinoClass = enumOrDefault(v, DinoClass.WILD_CARD)

    @TypeConverter fun fromSpawnLocation(v: SpawnLocation): String = v.name
    @TypeConverter fun toSpawnLocation(v: String): SpawnLocation = enumOrDefault(v, SpawnLocation.NONE)

    @TypeConverter fun fromHybridType(v: HybridType): String = v.name
    // New hybrid tiers historically extend upward, and isHybrid is a separate
    // column, so an unknown tier folds into the highest known one.
    @TypeConverter fun toHybridType(v: String): HybridType = enumOrDefault(v, HybridType.GIGA_MEGA)

    @TypeConverter fun fromProgressionSystem(v: ProgressionSystem): String = v.name
    @TypeConverter fun toProgressionSystem(v: String): ProgressionSystem = enumOrDefault(v, ProgressionSystem.BOOST)

    @TypeConverter fun fromResistanceType(v: ResistanceType): String = v.name
    @TypeConverter fun toResistanceType(v: String): ResistanceType = enumOrDefault(v, ResistanceType.DOT)

    @TypeConverter fun fromResearchStatus(v: ResearchStatus): String = v.name
    @TypeConverter fun toResearchStatus(v: String): ResearchStatus = enumOrDefault(v, ResearchStatus.UNRESEARCHED)

    @TypeConverter fun fromMovePriorityType(v: MovePriorityType): String = v.name
    @TypeConverter fun toMovePriorityType(v: String): MovePriorityType = enumOrDefault(v, MovePriorityType.NORMAL)

    @TypeConverter fun fromMoveTriggerType(v: MoveTriggerType): String = v.name
    @TypeConverter fun toMoveTriggerType(v: String): MoveTriggerType = enumOrDefault(v, MoveTriggerType.SELECTABLE)

    @TypeConverter fun fromMoveUnlockType(v: MoveUnlockType): String = v.name
    @TypeConverter fun toMoveUnlockType(v: String): MoveUnlockType = enumOrDefault(v, MoveUnlockType.DEFAULT)

    @TypeConverter fun fromEffectTarget(v: EffectTarget): String = v.name
    @TypeConverter fun toEffectTarget(v: String): EffectTarget = enumOrDefault(v, EffectTarget.SELF)

    @TypeConverter fun fromEnhancementType(v: EnhancementType): String = v.name
    @TypeConverter fun toEnhancementType(v: String): EnhancementType = enumOrDefault(v, EnhancementType.STAT_BONUS)

    @TypeConverter fun fromCatalystType(v: CatalystType): String = v.name
    @TypeConverter fun toCatalystType(v: String): CatalystType = enumOrDefault(v, CatalystType.BRONZE)
}
