package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
// something simple
@Entity(tableName = "search_query")
@Parcelize
@Keep
data class SearchQuery(@PrimaryKey val id: String, val queryString: String, val createdAt: Long, val type: Int): Parcelable {
    constructor(): this("", "", System.currentTimeMillis(), QUERY_TYPE_POST)
}

const val QUERY_TYPE_POST = 0
const val QUERY_TYPE_USER = 1
const val QUERY_TYPE_INTEREST = 2