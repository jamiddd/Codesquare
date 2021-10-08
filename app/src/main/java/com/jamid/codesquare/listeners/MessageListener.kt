package com.jamid.codesquare.listeners

import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.data.Message

interface MessageListener {
    fun onStartDownload(message: Message, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit)
    fun onDocumentClick(message: Message)
    fun onImageClick(message: Message)
}