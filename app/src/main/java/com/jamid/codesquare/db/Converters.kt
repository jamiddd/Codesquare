package com.jamid.codesquare.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
// something simple
class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null){
            return null
        }
        val listType: Type = object : TypeToken<List<String>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromBooleanToInt(state: Boolean): Int {
        return if (state) {
            1
        } else {
            0
        }
    }

    @TypeConverter
    fun fromIntToBoolean(state: Int): Boolean {
        return state == 1
    }
}