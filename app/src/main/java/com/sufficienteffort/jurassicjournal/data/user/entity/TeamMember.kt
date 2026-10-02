package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "team_members",
    primaryKeys = ["teamId", "dinoId"],
    foreignKeys = [ForeignKey(
        entity = Team::class,
        parentColumns = ["id"],
        childColumns = ["teamId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("teamId")],
)
data class TeamMember(
    val teamId: Long,
    val dinoId: Long,
    val slotOrder: Int = 0,
)
