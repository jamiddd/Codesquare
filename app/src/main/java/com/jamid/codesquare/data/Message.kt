package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName="messages")
@Parcelize
@Keep
data class Message(
    @PrimaryKey
    var messageId: String,
    var chatChannelId: String,
    var type: String,
    var content: String,
    var senderId: String,
    @Embedded(prefix = "message_")
    var sender: UserMinimal,
    @Embedded(prefix = "meta_")
    var metadata: Metadata?,
    var deliveryList: List<String>,
    var readList: List<String>,
    val createdAt: Long,
    var updatedAt: Long,
    var replyTo: String? = null,
    @Embedded(prefix = "reply_")
    var replyMessage: MessageMinimal?,
    @Exclude @set: Exclude @get: Exclude
    var isDownloaded: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isCurrentUserMessage: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var state: Int = -1
): Parcelable {

    fun toReplyMessage(): MessageMinimal {
        return MessageMinimal(senderId, sender.name, content, type, messageId, chatChannelId, isDownloaded, metadata)
    }

    constructor(): this("", "", "", "", "", UserMinimal(), Metadata(), emptyList(), emptyList(), System.currentTimeMillis(), System.currentTimeMillis(), null, MessageMinimal(),false, false)
}
