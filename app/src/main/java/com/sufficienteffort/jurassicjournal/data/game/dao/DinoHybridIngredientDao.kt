package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DinoHybridIngredientDao {
    @Query("SELECT ingredientDinoId FROM dino_hybrid_ingredients WHERE hybridDinoId = :hybridDinoId")
    suspend fun getIngredientIds(hybridDinoId: Long): List<Long>

    @Query("SELECT hybridDinoId FROM dino_hybrid_ingredients WHERE ingredientDinoId = :ingredientId")
    suspend fun getHybridIdsForIngredient(ingredientId: Long): List<Long>
}
