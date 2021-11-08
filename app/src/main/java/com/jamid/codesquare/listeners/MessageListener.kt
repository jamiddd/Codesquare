package com.jamid.codesquare.listeners

import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.User

interface MessageListener {
    fun onStartDownload(message: Message, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit)
    fun onDocumentClick(message: Message)
    fun onImageClick(view: View, message: Message, pos: Int, id: String)
    fun onMessageRead(message: Message)
    fun onUserClick(message: Message)
    fun onForwardClick(view: View, message: Message)
    fun onMessageLongClick(message: Message)
    fun onMessageStateChanged(message: Message)
    suspend fun onGetMessageReply(replyTo: String): Message?
    suspend fun onGetMessageReplyUser(senderId: String): User?
    fun onMessageDoubleClick(message: Message)
}