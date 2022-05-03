package com.jamid.codesquare.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.paging.ExperimentalPagingApi
import androidx.viewbinding.ViewBinding
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.data.Message

@OptIn(ExperimentalPagingApi::class)
abstract class MessageListenerFragment<T: ViewBinding, U: ViewModel>: BaseFragment<T, U>(), MessageListener {

    override fun onMessageClick(message: Message) {}
    override fun onMessageContextClick(message: Message) {}
    override fun onMessageImageClick(imageView: View, message: Message) {}
    override fun onMessageDocumentClick(message: Message) {}
    override fun onMessageRead(message: Message) {}
    override fun onMessageUpdated(message: Message) {}
    override fun onMessageSenderClick(message: Message) {}
    override fun onMessageNotDownloaded(message: Message, onComplete: (newMessage: Message) -> Unit) {}

    abstract fun onCheckForStaleData(message: Message, onUpdate: (newMessage: Message) -> Unit)
    open fun onReplyMessageClick(message: Message) {}
}

interface MessageListener {
    fun onMessageClick(message: Message) {}
    fun onMessageContextClick(message: Message) {}
    fun onMessageImageClick(imageView: View, message: Message) {}
    fun onMessageDocumentClick(message: Message) {}
    fun onMessageRead(message: Message) {}
    fun onMessageUpdated(message: Message) {}
    fun onMessageSenderClick(message: Message) {}
    fun onMessageNotDownloaded(message: Message, onComplete: (newMessage: Message) -> Unit) {}
}