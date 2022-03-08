package com.jamid.codesquare.listeners

import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Message

class SampleAc: MessageClickListener2() {

    override fun onStartDownload(onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit) {

    }

    override fun onDocumentClick() {
        TODO("Not yet implemented")
    }

    override fun onImageClick(view: View, image: Image) {
        TODO("Not yet implemented")
    }

    override fun onMessageRead() {

    }

    override fun onUserClick() {
        TODO("Not yet implemented")
    }

    override fun onForwardClick(messages: ArrayList<Message>) {
        TODO("Not yet implemented")
    }

    override fun onMessageUpdate(newMessage: Message) {
        TODO("Not yet implemented")
    }

    override fun onMessageLongClick() {
        TODO("Not yet implemented")
    }

    override fun onMessageClick(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onMessageDoubleClick(): Boolean {
        TODO("Not yet implemented")
    }

}