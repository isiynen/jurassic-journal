package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.model.Rarity

@Dao
interface LevelUpCostDao {
    @Query("SELECT * FROM level_up_costs WHERE rarity = :rarity ORDER BY fromLevel ASC")
    suspend fun getForRarity(rarity: Rarity): List<LevelUpCost>
}
