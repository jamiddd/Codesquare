package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.CHANNEL_GROUP
import com.jamid.codesquare.CHANNEL_PRIVATE
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
    var mute: Boolean = false,
    var archived: Boolean = false,
    var authorized: Boolean = false,
    @Embedded(prefix = "message_")
    var lastMessage: Message? = null,
    @Embedded(prefix = "message_data1_")
    var data1: UserMinimal? = null,
    @Embedded(prefix = "message_data2_")
    var data2: UserMinimal? = null
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

    fun toChatChannelWrapper(): ChatChannelWrapper {
        val isRead = if (this.lastMessage == null) {
            true
        } else {
            this.lastMessage!!.readList.contains(UserManager.currentUserId)
        }
        val (name, thumbnail) = if (this.type != CHANNEL_PRIVATE) {this.postTitle to this.postImage} else {
            if (this.data1?.userId == UserManager.currentUserId) {
                this.data2?.name to this.data2?.photo
            } else {
                this.data1?.name to this.data1?.photo
            }
        }
        return ChatChannelWrapper(this, this.chatChannelId, name.orEmpty(), false, isRead, thumbnail)
    }

    companion object {
        fun newInstance(post: Post) =
            ChatChannel().apply {
                postId = post.id
                postTitle = post.name
                postImage = post.thumbnail.ifBlank {
                    post.mediaList.firstOrNull {
                        !it.contains(".mp4")
                    } ?: ""
                }
                contributorsCount = post.contributors.size.toLong()
                contributors = post.contributors
                administrators = listOf(post.creator.userId)
                contributors = listOf(post.creator.userId)
                createdAt = post.createdAt
                updatedAt = post.updatedAt
                tokens = listOf(UserManager.currentUserId)
                authorized = true
                type = CHANNEL_GROUP
            }

        fun newInstance(user: User) =
            ChatChannel().apply {
                chatChannelId = randomId()
                postId = user.id + "," + UserManager.currentUserId
                postTitle = ""
                postImage = ""
                type = CHANNEL_PRIVATE
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