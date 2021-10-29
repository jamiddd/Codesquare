package com.jamid.codesquare.listeners

import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.data.DocumentHolder
import com.jamid.codesquare.data.Message

interface DocumentListener {
    fun onDocumentClick(documentHolder: DocumentHolder)
    fun onDocumentDownload(documentHolder: DocumentHolder, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit)
}