package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep
sealed class ChatChannel2 {
    data class Private(val chatChannel: ChatChannel): ChatChannel2()
    data class Group(val chatChannel: ChatChannel): ChatChannel2()
}