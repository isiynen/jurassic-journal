package com.sufficienteffort.jurassicjournal.data.user

import androidx.room.TypeConverter
import com.sufficienteffort.jurassicjournal.data.game.enumOrDefault
import com.sufficienteffort.jurassicjournal.data.model.CatalystType

class UserConverters {
    @TypeConverter fun fromCatalystType(v: CatalystType): String = v.name
    @TypeConverter fun toCatalystType(v: String): CatalystType = enumOrDefault(v, CatalystType.BRONZE)
}
