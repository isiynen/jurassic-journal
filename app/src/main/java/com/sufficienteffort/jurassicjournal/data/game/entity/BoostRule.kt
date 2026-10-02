package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boost_rules")
data class BoostRule(
    @PrimaryKey val id: Int = 1,
    val unlockLevel: Int = 10,
    val maxBoostsTotal: Int = 30,
    val maxPerStatCap: Int = 20
)
