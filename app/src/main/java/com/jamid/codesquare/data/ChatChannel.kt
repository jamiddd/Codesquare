package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "chat_channels")
@Keep
data class ChatChannel(
    @PrimaryKey(autoGenerate = false)
    var chatChannelId: String,
    var projectId: String,
    var projectTitle: String,
    var projectImage: String,
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
    var archived: Boolean = false
): Parcelable {
    constructor(): this(randomId(), "", "", "", 0, emptyList(), emptyList(), "Pssst \uD83E\uDD2D .. No rules written yet ... update it soon before other contributors join. \uD83D\uDE0E \uD83E\uDD73", System.currentTimeMillis(), System.currentTimeMillis(), Message(), emptyList(), emptyList())


    companion object {
        fun newInstance(project: Project): ChatChannel {
            val chatChannel = ChatChannel()
            chatChannel.projectId = project.id
            chatChannel.projectTitle = project.name
            chatChannel.projectImage = project.images.first()
            return chatChannel
        }
    }

}