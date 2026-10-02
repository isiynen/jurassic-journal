package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sufficienteffort.jurassicjournal.data.user.entity.NewDino
import kotlinx.coroutines.flow.Flow

@Dao
interface NewDinoDao {

    @Query("SELECT dinoSlug FROM new_dinos WHERE profileId = :profileId")
    fun observeNewSlugs(profileId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM new_dinos WHERE profileId = :profileId")
    fun observeNewCount(profileId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<NewDino>)

    @Query("DELETE FROM new_dinos WHERE profileId = :profileId AND dinoSlug = :slug")
    suspend fun delete(profileId: Long, slug: String)

    @Query("DELETE FROM new_dinos WHERE dinoSlug NOT IN (:validSlugs)")
    suspend fun pruneStale(validSlugs: List<String>)

    /** Insert new badges and prune stale ones atomically so a crash can't leave them half-applied. */
    @Transaction
    suspend fun applyDetection(entries: List<NewDino>, validSlugs: List<String>) {
        if (entries.isNotEmpty()) insertAll(entries)
        pruneStale(validSlugs)
    }
}
