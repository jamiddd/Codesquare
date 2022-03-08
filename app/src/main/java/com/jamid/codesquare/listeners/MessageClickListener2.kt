package com.jamid.codesquare.listeners

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Message

abstract class MessageClickListener2: GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    var currentMessage: Message = Message()

    // action based listeners
    abstract fun onStartDownload(onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit)
    abstract fun onDocumentClick()
    abstract fun onImageClick(view: View, image: Image)
    abstract fun onMessageRead()
    abstract fun onUserClick()
    abstract fun onForwardClick(messages: ArrayList<Message>)
    abstract fun onMessageUpdate(newMessage: Message)

    // click based listeners
    abstract fun onMessageLongClick()
    abstract fun onMessageClick(): Boolean
    abstract fun onMessageDoubleClick(): Boolean

    override fun onDown(p0: MotionEvent?) = true
    override fun onShowPress(p0: MotionEvent?) {}
    override fun onSingleTapUp(p0: MotionEvent?) = true
    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float) = true
    override fun onLongPress(p0: MotionEvent?) { onMessageLongClick() }
    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float) = true
    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean { return onMessageClick() }
    override fun onDoubleTap(p0: MotionEvent?): Boolean { return onMessageDoubleClick() }
    override fun onDoubleTapEvent(p0: MotionEvent?) = true
}