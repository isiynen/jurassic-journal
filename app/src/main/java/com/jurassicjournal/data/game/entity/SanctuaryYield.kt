package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.ResearchStatus

@Entity(tableName = "sanctuary_yields")
data class SanctuaryYield(
    @PrimaryKey val dinoId: Long,
    val baseYieldAt26: Float = 0f,
    val yieldPerLevelDelta: Float = 0f,
    val yieldPerSpeedBoostDelta: Float = 0f,
    val formulaOverrideJson: String? = null,
    val researchStatus: ResearchStatus = ResearchStatus.UNRESEARCHED,
    val lastVerified: Long? = null,
    val notes: String? = null
)
