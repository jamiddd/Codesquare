package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "post_requests")
@Parcelize
@Keep// something simple
data class PostRequest(
    @PrimaryKey(autoGenerate = false)
    var requestId: String,
    var postId: String,
    var senderId: String,
    var receiverId: String,
    @Embedded(prefix = "request_post_")
    var post: PostMinimal,
    @Embedded(prefix = "request_user_")
    var sender: UserMinimal,
    var createdAt: Long,
    var updatedAt: Long,
    var notificationId: String
): Parcelable {
    constructor(): this("", "", "", "", PostMinimal(), UserMinimal(), System.currentTimeMillis(), System.currentTimeMillis(), "")
}