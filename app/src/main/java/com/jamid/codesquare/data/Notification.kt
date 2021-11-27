package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey
    var id: String,
    var title: String,
    var content: String,
    var createdAt: Long,
    var senderId: String,
    var receiverId: String,
    var contextId: String,
    var type: Int,
    var clazz: String,
    @Exclude @set: Exclude @get: Exclude
    var read: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isReceived: Boolean = true
): Parcelable {
    constructor(): this("", "", "", 0, "", "", "", 0, "")
}