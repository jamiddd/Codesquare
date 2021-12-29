package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
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
    var image: String? = null,
    var projectId: String? = null,
    var commentChannelId: String? = null,
    var commentId: String? = null,
    var userId: String? = null,
    var type: Int = 0,
    var read: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isReceived: Boolean = true
): Parcelable {
    constructor(): this("", "", "", 0, "", "")

    companion object {

        fun createNotification(content: String, senderId: String, receiverId: String, type: Int = 0, userId: String? = null, projectId: String? = null, commentId: String? = null, commentChannelId: String? = null, id: String? = null, title: String? = null, image: String? = null): Notification {
            val mId = id ?: randomId()
            val mTitle = title ?: "Codesquare"
            return Notification(mId, mTitle, content, System.currentTimeMillis(), senderId, receiverId, image, projectId, commentChannelId, commentId, userId, type)
        }
    }

}