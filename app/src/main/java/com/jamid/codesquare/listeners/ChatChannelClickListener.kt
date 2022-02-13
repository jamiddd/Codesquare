package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ChatChannel

interface ChatChannelClickListener {
    fun onChannelClick(chatChannel: ChatChannel)
    fun onChatChannelSelected(chatChannel: ChatChannel)
    fun onChatChannelDeSelected(chatChannel: ChatChannel)
}