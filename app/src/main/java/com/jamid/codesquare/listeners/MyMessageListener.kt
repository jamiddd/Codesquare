package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.Message

interface MyMessageListener {

    fun onMessageClick(message: Message)
    fun onMessageContextClick(message: Message)
    fun onMessageImageClick(imageView: View, message: Message)
    fun onMessageDocumentClick(message: Message)
    fun onMessageRead(message: Message)
    fun onMessageUpdated(message: Message)
    fun onMessageSenderClick(message: Message)
    fun onMessageNotDownloaded(message: Message, onComplete: (newMessage: Message) -> Unit)

}