package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ChatChannelWrapper

interface ChatChannelClickListener {
    fun onChannelClick(chatChannelWrapper: ChatChannelWrapper, pos: Int)
    fun onChannelUnread(chatChannelWrapper: ChatChannelWrapper)
    fun onChannelOptionClick(chatChannelWrapper: ChatChannelWrapper)
}