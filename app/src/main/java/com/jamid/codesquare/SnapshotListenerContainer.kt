package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The objective of this class is only to attach and detach firestore listeners according to
 * the activity. To properly guarantee that the listener is active after the activity
 * was paused in the middle.
 *
 * This class is dependent on the fact that the singleton objects such as
 * [NotificationManager], [UserManager], [ProjectManager] and [ChatManager] are
 * already initialized and ready for action.
 *
 * */
class SnapshotListenerContainer : LifecycleEventObserver {

    private var currentUserListenerRegistration: ListenerRegistration? = null
    private var chatChannelsListenerRegistration: ListenerRegistration? = null
    private var notificationListenerRegistration: ListenerRegistration? = null
    private var projectRequestsListenerRegistration: ListenerRegistration? = null

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {

            }
            Lifecycle.Event.ON_START -> {

            }
            Lifecycle.Event.ON_RESUME -> {
                initializeListeners(source)
            }
            Lifecycle.Event.ON_PAUSE -> {
                pauseListeners()
            }
            Lifecycle.Event.ON_STOP -> {
                pauseListeners()
            }
            Lifecycle.Event.ON_DESTROY -> {
                pauseListeners()
            }
            Lifecycle.Event.ON_ANY -> {

            }
        }
    }

    private fun pauseListeners() {
        currentUserListenerRegistration?.remove()
        currentUserListenerRegistration = null

        chatChannelsListenerRegistration?.remove()
        chatChannelsListenerRegistration = null

        notificationListenerRegistration?.remove()
        notificationListenerRegistration = null

        projectRequestsListenerRegistration?.remove()
        projectRequestsListenerRegistration = null
    }

    private fun initializeListeners(source: LifecycleOwner) {
        Firebase.auth.addAuthStateListener {
            val firebaseUser = it.currentUser
            if (firebaseUser != null) {

                val currentUserId = firebaseUser.uid

                // 1. current user document listener
                currentUserListenerRegistration = Firebase.firestore.collection(USERS)
                    .document(currentUserId)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Log.e(
                                TAG,
                                "initializeListeners: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && value.exists()) {
                            val currentUser = value.toObject(User::class.java)!!
                            ChatManager.lateInitialize(currentUser)
                            UserManager.updateUser(currentUser)
                        }

                    }

                // 2. chat channel collection listener
                chatChannelsListenerRegistration = Firebase.firestore.collection(CHAT_CHANNELS)
                    .whereArrayContains(CONTRIBUTORS, currentUserId)
                    .addSnapshotListener { value, error ->

                        if (error != null) {
                            Log.e(
                                TAG,
                                "initializeListeners: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && !value.isEmpty) {

                            val immediatelyChangedChatChannels = mutableListOf<ChatChannel>()
                            val changes = value.documentChanges
                            for (change in changes) {
                                val n = change.document.toObject(ChatChannel::class.java)
                                immediatelyChangedChatChannels.add(n)
                            }

                            val chatChannels = value.toObjects(ChatChannel::class.java)
                            ChatManager.setChatChannels(chatChannels)

                            ChatManager.setImmediateChannels(immediatelyChangedChatChannels)
                        }
                    }

                // 3. notification listener
                notificationListenerRegistration = Firebase.firestore.collection(USERS)
                    .document(currentUserId)
                    .collection(NOTIFICATIONS)
                    .limit(NOTIFICATION_LIMIT)
                    .addSnapshotListener { value, error ->

                        if (error != null) {
                            Log.e(
                                TAG,
                                "initializeListeners: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && !value.isEmpty) {
                            val notifications = value.toObjects(Notification::class.java)

                            NotificationManager.setNotifications(notifications)
                        }

                    }



                // 4. project requests listener
                projectRequestsListenerRegistration = Firebase.firestore.collection(PROJECT_REQUESTS)
                    .whereEqualTo(SENDER_ID, currentUserId)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Log.e(
                                TAG,
                                "initializeListeners: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && !value.isEmpty) {
                            val projectRequests = value.toObjects(ProjectRequest::class.java)

                            source.lifecycleScope.launch (Dispatchers.IO) {
                                ProjectManager.setMyProjectRequests(projectRequests)
                            }
                        }
                    }
            }
        }
    }

    companion object {
        private const val TAG = "SnapshotListener"
        private const val NOTIFICATION_LIMIT: Long = 10
    }

}