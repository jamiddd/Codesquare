package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class MessageMinimal(
    val senderId: String,
    var name: String,
    var content: String,
    var type: String,
    var messageId: String,
    var chatChannelId: String,
    @Exclude @set: Exclude @get: Exclude
    var isDownloaded: Boolean,
    @Embedded(prefix = "metadata_")
    var metadata: Metadata?
): Parcelable {
    constructor(): this("", "", "", "", "", "", false, null)

    fun toMessage(): Message {
        val message = Message()
        message.messageId = this.messageId
        message.senderId = this.senderId
        message.content = this.content
        message.type = this.type
        message.chatChannelId = this.chatChannelId
        message.metadata = this.metadata
        return message
    }

}