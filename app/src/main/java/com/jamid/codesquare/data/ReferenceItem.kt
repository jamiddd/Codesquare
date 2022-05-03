package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "liked_item")
@Parcelize
data class ReferenceItem(
    @PrimaryKey
    val id: String,
    val createdAt: Long
): Parcelable {
    constructor(): this("", 0)
}