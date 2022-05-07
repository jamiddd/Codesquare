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

@Parcelize
@Entity(tableName = "chat_channels")
@Keep
@IgnoreExtraProperties
data class ChatChannel(
    @PrimaryKey(autoGenerate = false)
    var chatChannelId: String,
    var postId: String,
    var postTitle: String,
    var postImage: String,
    var contributorsCount: Long,
    var administrators: List<String>,
    var contributors: List<String>,
    var rules: String,
    var createdAt: Long,
    var updatedAt: Long,
    @Embedded(prefix = "message_")
    var lastMessage: Message?,
    var tokens: List<String>,
    var blockedUsers: List<String> = emptyList(),
    @Exclude @set: Exclude @get: Exclude
    var isNewLastMessage: Boolean = false,
    var archived: Boolean = false
): Parcelable {
    constructor(): this(randomId(), "", "", "", 0, emptyList(), emptyList(), "Pssst \uD83E\uDD2D .. No rules written yet ... update it soon before other contributors join. \uD83D\uDE0E \uD83E\uDD73", System.currentTimeMillis(), System.currentTimeMillis(), null, emptyList(), emptyList())


    companion object {
        fun newInstance(post: Post): ChatChannel {
            val chatChannel = ChatChannel()
            chatChannel.postId = post.id
            chatChannel.postTitle = post.name
            chatChannel.postImage = post.images.first()
            chatChannel.contributorsCount = post.contributors.size.toLong()
            chatChannel.contributors = post.contributors
            chatChannel.administrators = listOf(post.creator.userId)
            chatChannel.contributors = listOf(post.creator.userId)
            chatChannel.createdAt = post.createdAt
            chatChannel.updatedAt = post.updatedAt
            chatChannel.tokens = listOf(UserManager.currentUserId)
            return chatChannel
        }
    }

}