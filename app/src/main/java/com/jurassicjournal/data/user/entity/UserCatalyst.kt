package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jurassicjournal.data.model.CatalystType

@Entity(
    tableName = "user_catalysts",
    primaryKeys = ["profileId", "catalystType"],
)
data class UserCatalyst(
    val profileId: Long = 1,
    val catalystType: CatalystType,
    val quantityOwned: Int = 0,
)
