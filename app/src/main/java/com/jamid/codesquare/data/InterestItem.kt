package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "interest_item")
@Parcelize
@Serializable
@IgnoreExtraProperties
@Keep
data class InterestItem(
    @PrimaryKey
    @SerialName("itemId")
    var itemId: String,
    @SerialName("objectID")
    var objectID: String,
    @SerialName("content")
    var content: String,
    @SerialName("createdAt")
    var createdAt: Long,
    @SerialName("associations")
    var associations: List<String>,
    @SerialName("updatedAt")
    var updatedAt: Long,
    @SerialName("weight")
    var weight: Double,
    @Transient
    @Exclude @set: Exclude @get: Exclude
    var isChecked: Boolean = false
): Parcelable {
    constructor(): this("", "", "", 0, emptyList(), 0, 0.0)
}