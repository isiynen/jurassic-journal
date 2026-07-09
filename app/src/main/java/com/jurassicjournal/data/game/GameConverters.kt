package com.sufficienteffort.jurassicjournal.data.game

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

class GameConverters {
    @TypeConverter fun fromRarity(v: Rarity): String = v.name
    @TypeConverter fun toRarity(v: String): Rarity = Rarity.valueOf(v)

    @TypeConverter fun fromDinoClass(v: DinoClass): String = v.name
    @TypeConverter fun toDinoClass(v: String): DinoClass = DinoClass.valueOf(v)

    @TypeConverter fun fromSpawnLocation(v: SpawnLocation): String = v.name
    @TypeConverter fun toSpawnLocation(v: String): SpawnLocation = SpawnLocation.valueOf(v)

    @TypeConverter fun fromHybridType(v: HybridType): String = v.name
    @TypeConverter fun toHybridType(v: String): HybridType = HybridType.valueOf(v)

    @TypeConverter fun fromProgressionSystem(v: ProgressionSystem): String = v.name
    @TypeConverter fun toProgressionSystem(v: String): ProgressionSystem = ProgressionSystem.valueOf(v)

    @TypeConverter fun fromResistanceType(v: ResistanceType): String = v.name
    @TypeConverter fun toResistanceType(v: String): ResistanceType = ResistanceType.valueOf(v)

    @TypeConverter fun fromResearchStatus(v: ResearchStatus): String = v.name
    @TypeConverter fun toResearchStatus(v: String): ResearchStatus = ResearchStatus.valueOf(v)

    @TypeConverter fun fromMovePriorityType(v: MovePriorityType): String = v.name
    @TypeConverter fun toMovePriorityType(v: String): MovePriorityType = MovePriorityType.valueOf(v)

    @TypeConverter fun fromMoveTriggerType(v: MoveTriggerType): String = v.name
    @TypeConverter fun toMoveTriggerType(v: String): MoveTriggerType = MoveTriggerType.valueOf(v)

    @TypeConverter fun fromMoveUnlockType(v: MoveUnlockType): String = v.name
    @TypeConverter fun toMoveUnlockType(v: String): MoveUnlockType = MoveUnlockType.valueOf(v)

    @TypeConverter fun fromEffectTarget(v: EffectTarget): String = v.name
    @TypeConverter fun toEffectTarget(v: String): EffectTarget = EffectTarget.valueOf(v)

    @TypeConverter fun fromEnhancementType(v: EnhancementType): String = v.name
    @TypeConverter fun toEnhancementType(v: String): EnhancementType = EnhancementType.valueOf(v)

    @TypeConverter fun fromCatalystType(v: CatalystType): String = v.name
    @TypeConverter fun toCatalystType(v: String): CatalystType = CatalystType.valueOf(v)
}
