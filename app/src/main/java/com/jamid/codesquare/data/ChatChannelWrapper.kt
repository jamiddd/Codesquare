package com.jamid.codesquare.data

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamid.codesquare.randomId

@Keep
@Entity(tableName = "chat_channel_wrapper")
data class ChatChannelWrapper(
    @Embedded(prefix = "chatChannel_")
    val chatChannel: ChatChannel,
    @PrimaryKey
    val id: String = randomId(),
    var channelName: String = "",
    var isSelected: Boolean = false,
    var isRead: Boolean = false,
    var thumbnail: String? = null,
    var selectCount: Int = -1
)