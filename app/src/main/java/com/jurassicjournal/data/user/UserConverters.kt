package com.jurassicjournal.data.user

import androidx.room.TypeConverter
import com.jurassicjournal.data.model.CatalystType

class UserConverters {
    @TypeConverter fun fromCatalystType(v: CatalystType): String = v.name
    @TypeConverter fun toCatalystType(v: String): CatalystType = CatalystType.valueOf(v)
}
