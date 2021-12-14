package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User

const val NOTIFICATION_LIKE_PROJECT = 0
const val NOTIFICATION_LIKE_COMMENT = 1
const val NOTIFICATION_LIKE_USER = 2
const val NOTIFICATION_DISLIKE_PROJECT = 3
const val NOTIFICATION_DISLIKE_COMMENT = 4
const val NOTIFICATION_DISLIKE_USER = 5
const val NOTIFICATION_ACCEPT = 6
const val NOTIFICATION_REJECT = 7
const val NOTIFICATION_PROJECT_CREATION = 8
const val NOTIFICATION_COMMENT_CREATION = 9
const val NOTIFICATION_THREAD_CREATION = 10
const val NOTIFICATION_JOIN_PROJECT = 11
const val NOTIFICATION_REMOVE_CONTRIBUTOR = 12
const val NOTIFICATION_INVITE_PROJECT = 13


object NotificationProvider {

    data class Za(
        val title: String,
        val contextId: String,
        val clazz: String
    )

    val newNotifications = MutableLiveData<List<Notification>>().apply { value = emptyList() }

    private lateinit var sender: User
    private var isInitialized = false

    fun init(u: User) {
        if (!isInitialized) {
            sender = u
            isInitialized = true
        }
    }

    fun setNotificationsListener(currentUserId: String) {
        Firebase.firestore.collection("users").document(currentUserId)
            .collection("notifications")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    val notifications = value.toObjects(Notification::class.java)
                    newNotifications.postValue(notifications)
                }
            }
    }

    fun createNotification(context: Any, receiver: String, type: Int): Notification {

        val obj = when (context) {
            is Project -> Za(context.name, context.id, "project")
            is Comment -> Za(if (context.postTitle.isBlank()) {"Thread"} else {context.postTitle}, context.commentId, "comment")
            is User -> Za(context.name, context.id, "user")
            else -> throw IllegalArgumentException("context object can only be of type project, comment and user")
        }

        return Notification(
            randomId(),
            obj.title,
            sender.name + getContentSuffixBasedOnType(type),
            System.currentTimeMillis(),
            sender.id,
            receiver,
            obj.contextId,
            type,
            obj.clazz,
            true,
            isReceived = false
        )
    }

    private fun getContentSuffixBasedOnType(type: Int): String {
        return " " + when (type) {
            NOTIFICATION_LIKE_PROJECT -> "liked your project"
            NOTIFICATION_LIKE_COMMENT -> "liked your comment"
            NOTIFICATION_LIKE_USER -> "liked your profile"
            NOTIFICATION_ACCEPT -> "has accepted your project request"
            NOTIFICATION_COMMENT_CREATION -> "has posted a comment on your project"
            NOTIFICATION_THREAD_CREATION -> "has replied to your comment"
            NOTIFICATION_JOIN_PROJECT -> "has requested to join your project."
            NOTIFICATION_REMOVE_CONTRIBUTOR -> "have been removed from project."
            NOTIFICATION_INVITE_PROJECT -> "has requested you to join their project."
            else -> ""
        }
    }

}