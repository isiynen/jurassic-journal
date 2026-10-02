package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sufficienteffort.jurassicjournal.data.user.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    // The profile-scoped tables below have no FK to profiles (only teams,
    // team_members and new_dinos cascade), so deleting a profile must clear
    // them explicitly or the rows are orphaned forever.

    @Query("DELETE FROM user_wallet WHERE profileId = :id")
    suspend fun deleteWalletFor(id: Long)

    @Query("DELETE FROM user_dna_inventory WHERE profileId = :id")
    suspend fun deleteDnaInventoryFor(id: Long)

    @Query("DELETE FROM user_catalysts WHERE profileId = :id")
    suspend fun deleteCatalystsFor(id: Long)

    @Query("DELETE FROM user_dinos WHERE profileId = :id")
    suspend fun deleteDinosFor(id: Long)

    @Query("DELETE FROM user_dino_enhancements WHERE profileId = :id")
    suspend fun deleteDinoEnhancementsFor(id: Long)

    @Query("DELETE FROM user_boosts WHERE profileId = :id")
    suspend fun deleteBoostsFor(id: Long)

    @Query("DELETE FROM omega_training_allocations WHERE profileId = :id")
    suspend fun deleteOmegaAllocationsFor(id: Long)

    /** Deletes a profile and every row it owns in one transaction. */
    @Transaction
    suspend fun deleteProfileWithData(id: Long) {
        deleteWalletFor(id)
        deleteDnaInventoryFor(id)
        deleteCatalystsFor(id)
        deleteDinosFor(id)
        deleteDinoEnhancementsFor(id)
        deleteBoostsFor(id)
        deleteOmegaAllocationsFor(id)
        deleteById(id)   // cascades teams, team_members, new_dinos via FK
    }

    @Query("DELETE FROM user_wallet WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedWallets()

    @Query("DELETE FROM user_dna_inventory WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedDnaInventory()

    @Query("DELETE FROM user_catalysts WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedCatalysts()

    @Query("DELETE FROM user_dinos WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedDinos()

    @Query("DELETE FROM user_dino_enhancements WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedDinoEnhancements()

    @Query("DELETE FROM user_boosts WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedBoosts()

    @Query("DELETE FROM omega_training_allocations WHERE profileId NOT IN (SELECT id FROM profiles)")
    suspend fun pruneOrphanedOmegaAllocations()

    /** Sweeps rows orphaned by profile deletions that predate deleteProfileWithData. */
    @Transaction
    suspend fun pruneOrphanedData() {
        pruneOrphanedWallets()
        pruneOrphanedDnaInventory()
        pruneOrphanedCatalysts()
        pruneOrphanedDinos()
        pruneOrphanedDinoEnhancements()
        pruneOrphanedBoosts()
        pruneOrphanedOmegaAllocations()
    }
}
