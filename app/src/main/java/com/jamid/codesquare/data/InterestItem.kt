package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Entity(tableName = "interest_item")
@Parcelize
@IgnoreExtraProperties
data class InterestItem(
    @PrimaryKey
    var itemId: String,
    var objectID: String,
    var content: String,
    var createdAt: Long,
    var associations: List<String>,
    var updatedAt: Long,
    var weight: Double,
    @Exclude @set: Exclude @get: Exclude
    var isChecked: Boolean = false
): Parcelable {
    constructor(): this("", "", "", 0, emptyList(), 0, 0.0)
}