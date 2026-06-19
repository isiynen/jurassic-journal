package com.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dino_hybrid_ingredients")
data class DinoHybridIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hybridDinoId: Long,
    val ingredientDinoId: Long,
    val dnaCostPerFuse: Int
)
