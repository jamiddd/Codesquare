package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.data.User

/**
 * The objective of this class is only to attach and detach firestore listeners according to
 * the activity. To properly guarantee that the listener is active after the activity
 * was paused in the middle.
 *
 * This class is dependent on the fact that the singleton objects such as
 * [UserManager] are
 * already initialized and ready for action.
 *
 * */// something simple
@OptIn(ExperimentalPagingApi::class)
class SnapshotListenerContainer(private val viewModel: MainViewModel): LifecycleEventObserver {

    private var currentUserListenerRegistration: ListenerRegistration? = null
    private var notificationListenerRegistration: ListenerRegistration? = null
    private var projectRequestsListenerRegistration: ListenerRegistration? = null

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {

            }
            Lifecycle.Event.ON_START -> {

            }
            Lifecycle.Event.ON_RESUME -> {
                initializeListeners()
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

        notificationListenerRegistration?.remove()
        notificationListenerRegistration = null

        projectRequestsListenerRegistration?.remove()
        projectRequestsListenerRegistration = null

        for (item in listenerMap) {
            item.value.remove()
        }

        listenerMap.clear()

    }

    private val listenerMap = mutableMapOf<String, ListenerRegistration>()

    private fun setContributorsListener(chatChannelId: String) {
        if (!listenerMap.containsKey(chatChannelId)) {
            Log.d(TAG, "setContributorsListener: Setting listener for $chatChannelId")
            val lis = Firebase.firestore.collection(USERS)
                .whereArrayContains(CHAT_CHANNEL, chatChannelId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, "onViewCreated: ${error.localizedMessage}")
                    }

                    if (value != null && !value.isEmpty) {

                        Log.d(TAG, "setContributorsListener: Got new contributors ${value.size()}")

                        val contributors = value.toObjects(User::class.java)
                        viewModel.insertUsers(contributors)
                    }
                }

            listenerMap[chatChannelId] = lis
        }
    }

    private fun initializeListeners() {
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
                                "initializeListeners $currentUserId: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && value.exists()) {
                            val currentUser = value.toObject(User::class.java)!!
                            UserManager.updateUser(currentUser)
                            viewModel.insertCurrentUser(currentUser)

                            for (channel in currentUser.chatChannels) {
                                setContributorsListener(channel)
                            }
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

                            Log.d(TAG, "initializeListeners: $notifications")

                            viewModel.insertNotifications(notifications)
                        }

                    }


                // 4. project requests listener
                projectRequestsListenerRegistration = Firebase.firestore.collection(POST_REQUESTS)
                    .whereEqualTo(SENDER_ID, currentUserId)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Log.e(
                                TAG,
                                "initializeListeners: Something went wrong - ${error.localizedMessage}")
                            return@addSnapshotListener
                        }

                        if (value != null && !value.isEmpty) {
                            val projectRequests = value.toObjects(PostRequest::class.java)

                            viewModel.insertPostRequests(projectRequests)
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