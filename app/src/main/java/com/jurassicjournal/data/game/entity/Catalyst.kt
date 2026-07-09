package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.CatalystType

@Entity(tableName = "catalysts")
data class Catalyst(
    @PrimaryKey val id: Long = 0,
    val type: CatalystType,
    val name: String,
    val description: String
)
