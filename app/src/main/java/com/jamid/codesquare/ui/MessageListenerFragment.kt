package com.jamid.codesquare.ui

import android.view.View
import androidx.fragment.app.Fragment
import com.jamid.codesquare.data.Message

abstract class MessageListenerFragment: Fragment() {

    open fun onMessageClick(message: Message) {}
    open fun onMessageContextClick(message: Message) {}
    open fun onMessageImageClick(imageView: View, message: Message) {}
    open fun onMessageDocumentClick(message: Message) {}
    open fun onMessageRead(message: Message) {}
    open fun onMessageUpdated(message: Message) {}
    open fun onMessageSenderClick(message: Message) {}
    open fun onMessageNotDownloaded(message: Message, onComplete: (newMessage: Message) -> Unit) {}

}