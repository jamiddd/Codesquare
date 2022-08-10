package com.jamid.codesquare.data

import androidx.annotation.Keep
import com.jamid.codesquare.randomId

@Keep
data class ChatChannelWrapper(
    val chatChannel: ChatChannel,
    val id: String = randomId(),
    var isSelected: Boolean = false,
    var selectCount: Int = -1
)// something simple