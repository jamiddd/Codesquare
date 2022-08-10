package com.jamid.codesquare.data

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "post_invites")
@Keep// something simple
data class PostInvite(
    @PrimaryKey
    var id: String,
    var postId: String,
    var receiverId: String,
    var senderId: String,
    @Embedded(prefix = "post_invite_sender_")
    var sender: UserMinimal,
    @Embedded(prefix = "post_invite_post_")
    var post: PostMinimal,
    var createdAt: Long,
    var updatedAt: Long,
    var notificationId: String
) {
    constructor(): this("", "", "", "", UserMinimal(), PostMinimal(), System.currentTimeMillis(), System.currentTimeMillis(),"")
}