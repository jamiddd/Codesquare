package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

/**
 * @param id An unique id of the notification
 * @param title Title of the notification
 * @param content Body of the notification
 * @param createdAt Time of creation of this notification
 * @param senderId The user id of the user who sent this notification
 * @param receiverId The user id of the user who received this notification
 * @param image Any associated image along with the notification, if there's any #url
 * @param postId If the notification is based on a post, there must be a post Id
 * @param commentChannelId If the notification is based on a thread, there must be commentChannel Id
 * @param commentId If the notification is based on a comment, there must be comment Id
 * @param userId If the notification is based on a user, there must be a user Id
 * @param type [0: For general notifications, -1: For post invite notification, 1: For post request notification]
 * @param read Status of the notification, if the receiver has read the notification or not
 * @param isReceived [Deprecated] Must remove in future migrations
* */
@Parcelize
@IgnoreExtraProperties
@Entity(tableName = "notifications")
@Keep
data class Notification(
    @PrimaryKey
    var id: String,
    var title: String,
    var content: String,
    var senderId: String,
    var receiverId: String,
    @Embedded(prefix = "notification_sender_")
    var sender: UserMinimal,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var image: String? = null,
    var postId: String? = null,
    var commentChannelId: String? = null,
    var commentId: String? = null,
    var userId: String? = null,
    var type: Int = 0,
    var read: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isReceived: Boolean = true
): Parcelable {
    constructor(): this("", "", "", "", "", UserManager.currentUser.minify())

    companion object {

        fun createNotification(
            content: String,
            receiverId: String,
            type: Int = 0,
            userId: String? = null,
            postId: String? = null,
            commentId: String? = null,
            commentChannelId: String? = null,
            id: String? = null,
            title: String? = null,
            image: String? = null
        ): Notification {
            val mId = id ?: randomId()
            val currentUser = UserManager.currentUser
            val mTitle = title ?: "Codesquare"
            return Notification(mId, mTitle, content, currentUser.id, receiverId, currentUser.minify(), System.currentTimeMillis(), System.currentTimeMillis(), image, postId, commentChannelId, commentId, userId, type)
        }
    }

}