package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jurassicjournal.data.user.entity.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeForProfile(profileId: Long): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getForProfile(profileId: Long): List<Team>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getById(id: Long): Team?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: Team): Long

    @Update
    suspend fun update(team: Team)

    @Delete
    suspend fun delete(team: Team)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteById(id: Long)
}
