package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.CatalystType

@Entity(
    tableName = "user_catalysts",
    primaryKeys = ["profileId", "catalystType"],
)
data class UserCatalyst(
    val profileId: Long = 1,
    val catalystType: CatalystType,
    val quantityOwned: Int = 0,
)
