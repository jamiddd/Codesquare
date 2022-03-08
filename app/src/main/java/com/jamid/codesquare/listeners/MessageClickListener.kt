package com.jamid.codesquare.listeners

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.FrescoImageControllerListener
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.User

interface MessageClickListener: GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    fun onStartDownload(message: Message, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit)
    fun onDocumentClick(message: Message)
    fun onImageClick(view: View, message: Message, controllerListener: FrescoImageControllerListener)
    fun onMessageRead(message: Message)
    fun onUserClick(message: Message)
    fun onForwardClick(view: View, messages: ArrayList<Message>)
    fun onMessageLongClick(message: Message)
    fun onGetReplyMessage(parentMessage: Message, onResult: (newMessage: Message) -> Unit)
    fun onGetMessageReplyUser(senderId: String, onComplete: (user: User?) -> Unit)
    fun onMessageStateChanged(message: Message)

    fun onMessageFocused(message: Message, parent: View)

    fun onMessageLongPress(p0: MotionEvent?)
    fun onMessageClick(p0: MotionEvent?): Boolean
    fun onMessageDoubleTapped(p0: MotionEvent?): Boolean

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean { return true }
    override fun onShowPress(p0: MotionEvent?) {}
    override fun onSingleTapUp(p0: MotionEvent?): Boolean { return true }
    override fun onDown(p0: MotionEvent?): Boolean { return true }
    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean { return true }
    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean { return true }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {
        return onMessageDoubleTapped(p0)
    }

    override fun onLongPress(p0: MotionEvent?) {
        onMessageLongPress(p0)
    }

    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
        return onMessageClick(p0)
    }



}