package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "interests")
@Serializable
@Parcelize
@Keep
data class Interest(
    @PrimaryKey
    @SerialName("objectID")
    val id: String,
    val interest: String
): Parcelable {
    constructor(): this("", "")
}