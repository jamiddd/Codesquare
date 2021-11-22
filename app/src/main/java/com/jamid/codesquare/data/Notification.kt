package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
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
    var type: String,
    var clazz: String,
): Parcelable {
    constructor(): this("", "", "", 0, "", "", "", "", "")
}