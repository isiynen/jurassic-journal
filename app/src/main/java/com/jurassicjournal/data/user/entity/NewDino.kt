package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "new_dinos",
    primaryKeys = ["profileId", "dinoSlug"],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId")],
)
data class NewDino(
    val profileId: Long,
    val dinoSlug: String,
)
