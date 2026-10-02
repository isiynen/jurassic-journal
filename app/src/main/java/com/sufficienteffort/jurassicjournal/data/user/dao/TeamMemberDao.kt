package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.user.entity.TeamMember
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY slotOrder ASC")
    fun observeForTeam(teamId: Long): Flow<List<TeamMember>>

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY slotOrder ASC")
    suspend fun getForTeam(teamId: Long): List<TeamMember>

    @Query("SELECT * FROM team_members WHERE teamId IN (SELECT id FROM teams WHERE profileId = :profileId) ORDER BY teamId ASC, slotOrder ASC")
    suspend fun getForProfile(profileId: Long): List<TeamMember>

    @Query("SELECT teamId FROM team_members WHERE dinoId = :dinoId")
    fun observeTeamIdsForDino(dinoId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: TeamMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TeamMember>)

    @Query("DELETE FROM team_members WHERE teamId = :teamId AND dinoId = :dinoId")
    suspend fun remove(teamId: Long, dinoId: Long)

    @Query("DELETE FROM team_members WHERE teamId = :teamId")
    suspend fun clearTeam(teamId: Long)
}
