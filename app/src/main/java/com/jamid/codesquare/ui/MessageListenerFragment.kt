package com.jamid.codesquare.ui

import com.jamid.codesquare.data.Message
// something simple
interface MessageListener3 {

    fun onMessageThumbnailNotDownload(message: Message) {}
    fun onMessageClick(message: Message) {}
    fun onMessageContextClick(message: Message) {}
    fun onMessageMediaItemClick(message: Message) {}
    fun onMessageSaveClick(message: Message, f: (Message) -> Unit) {}
    fun onMessageRead(message: Message) {}
    fun onMessageSenderClick(message: Message) {}
    fun onMessageNotDownloaded(message: Message, f: (Message) -> Unit) {}
    fun onMessageReplyMsgClick(message: Message) {}

}