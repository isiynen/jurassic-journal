package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jurassicjournal.data.model.CatalystType

@Entity(tableName = "user_catalysts")
data class UserCatalyst(
    @PrimaryKey val catalystType: CatalystType,
    val quantityOwned: Int = 0
)
