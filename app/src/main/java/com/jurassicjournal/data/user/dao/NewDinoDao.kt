package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
