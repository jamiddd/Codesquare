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
    var type: String = "group",
    var rules: String,
    var administrators: List<String>,
    var contributors: List<String>,
    var tokens: List<String>,
    var blockedUsers: List<String> = emptyList(),
    var contributorsCount: Long,
    var createdAt: Long,
    var updatedAt: Long,
    @Exclude @set: Exclude @get: Exclude
    var isNewLastMessage: Boolean = false,
    var mute: Boolean = false,
    var archived: Boolean = false,
    var authorized: Boolean = false,
    @Embedded(prefix = "message_")
    var lastMessage: Message? = null,
    @Embedded(prefix = "message_data1_")
    var data1: UserMinimal? = null,
    @Embedded(prefix = "message_data2_")
    var data2: UserMinimal? = null,
    @Exclude @set: Exclude @get: Exclude
    var thumbnail: String? = null
): Parcelable {
    constructor(): this(
        randomId(),
        "",
        "",
        "",
        rules = "Pssst \uD83E\uDD2D .. No rules written yet ... update it soon before other contributors join. \uD83D\uDE0E \uD83E\uDD73",
        administrators = emptyList(),
        contributors = emptyList(),
        tokens = emptyList(),
        blockedUsers = emptyList(),
        contributorsCount = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastMessage = null
    )


    companion object {
        fun newInstance(post: Post): ChatChannel {
            val chatChannel = ChatChannel()
            chatChannel.postId = post.id
            chatChannel.postTitle = post.name
            chatChannel.postImage = post.thumbnail.ifBlank {
                post.mediaList.firstOrNull {
                    !it.contains(".mp4")
                } ?: ""
            }
            chatChannel.contributorsCount = post.contributors.size.toLong()
            chatChannel.contributors = post.contributors
            chatChannel.administrators = listOf(post.creator.userId)
            chatChannel.contributors = listOf(post.creator.userId)
            chatChannel.createdAt = post.createdAt
            chatChannel.updatedAt = post.updatedAt
            chatChannel.tokens = listOf(UserManager.currentUserId)
            chatChannel.authorized = true
            chatChannel.type = "group"
            return chatChannel
        }

        fun newInstance(user: User): ChatChannel {
            return ChatChannel().apply {
                chatChannelId = randomId()
                postId = user.id + "," + UserManager.currentUserId
                postTitle = ""
                postImage = ""
                type = "private"
                rules = ""
                administrators = emptyList()
                contributors = listOf(user.id, UserManager.currentUserId)
                tokens = listOf(user.token, UserManager.currentUser.token)
                blockedUsers = emptyList()
                contributorsCount = 2
                val now = System.currentTimeMillis()
                createdAt = now
                updatedAt = now
                archived = false
                authorized = false
                lastMessage = null
                data1 = UserManager.currentUser.minify()
                data2 = user.minify()
            }
        }

    }

}