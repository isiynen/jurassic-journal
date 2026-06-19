package com.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jurassicjournal.data.model.CatalystType
import com.jurassicjournal.data.model.Rarity

@Entity(tableName = "catalyst_recipes")
data class CatalystRecipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalystType: CatalystType,
    val sourceRarity: Rarity,
    val coinsCost: Int,
    val dnaCostPerCatalyst: Int,
    val minDinoLevel: Int = 25
)
