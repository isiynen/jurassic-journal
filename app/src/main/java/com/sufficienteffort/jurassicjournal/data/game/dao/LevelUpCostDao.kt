package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.model.Rarity

@Dao
interface LevelUpCostDao {
    @Query("SELECT * FROM level_up_costs WHERE rarity = :rarity ORDER BY fromLevel ASC")
    suspend fun getForRarity(rarity: Rarity): List<LevelUpCost>

    @Query("SELECT * FROM level_up_costs WHERE rarity = :rarity AND fromLevel = :fromLevel LIMIT 1")
    suspend fun get(rarity: Rarity, fromLevel: Int): LevelUpCost?

    @Query("SELECT COUNT(*) FROM level_up_costs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(costs: List<LevelUpCost>)
}
