package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.DinoHybridIngredient

@Dao
interface DinoHybridIngredientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<DinoHybridIngredient>)

    @Query("SELECT * FROM dino_hybrid_ingredients WHERE hybridDinoId = :hybridDinoId")
    suspend fun getForHybrid(hybridDinoId: Long): List<DinoHybridIngredient>

    @Query("SELECT ingredientDinoId FROM dino_hybrid_ingredients WHERE hybridDinoId = :hybridDinoId")
    suspend fun getIngredientIds(hybridDinoId: Long): List<Long>

    @Query("SELECT hybridDinoId FROM dino_hybrid_ingredients WHERE ingredientDinoId = :ingredientId")
    suspend fun getHybridIdsForIngredient(ingredientId: Long): List<Long>
}
