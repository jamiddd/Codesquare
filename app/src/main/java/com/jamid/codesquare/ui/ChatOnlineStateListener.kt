package com.jamid.codesquare.ui

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.codesquare.CHAT_CHANNELS
import com.jamid.codesquare.TOKENS
import com.jamid.codesquare.UserManager

class ChatOnlineStateListener: LifecycleEventObserver {

    private lateinit var token: String

    init {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener {
                token = it
            }.addOnFailureListener {
                Log.e(TAG, "addTokenToChatChannels: ${it.localizedMessage}")
            }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                removeTokenFromChatChannels()
            }
            Lifecycle.Event.ON_PAUSE -> {
                addTokenToChatChannels()
            }
            else -> {

            }
        }
    }

    private fun toggleOnlineState(state: Boolean, token: String, channels: List<String>, shouldUpdateUser: Boolean) {
        val batch = Firebase.firestore.batch()

        val change = if (!state) {
            mapOf(TOKENS to FieldValue.arrayUnion(token))
        } else {
            mapOf(TOKENS to FieldValue.arrayRemove(token))
        }

        for (channel in channels) {
            val ref = Firebase.firestore.collection(CHAT_CHANNELS).document(channel)
            batch.update(ref, change)
        }

        batch.commit().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(
                    TAG,
                    "Successfully changed offline status to online in chat channel document."
                )

                if (shouldUpdateUser) {
                    val currentUser = UserManager.currentUser
                    currentUser.token = token
                    UserManager.updateUser(currentUser)
                }

            } else {
                Log.e(
                    TAG,
                    "removeTokenFromChatChannels: ${it.exception?.localizedMessage}")
            }
        }

    }

    private fun addTokenToChatChannels() {
        // the user is initialized and ready for action
        val currentUser = UserManager.currentUser

        if (currentUser.chatChannels.isNotEmpty()) {
            toggleOnlineState(false, token, currentUser.chatChannels, currentUser.token != token)
        }
    }

    private fun removeTokenFromChatChannels() {
        // the user is initialized and ready for action
        val currentUser = UserManager.currentUser

        if (currentUser.chatChannels.isNotEmpty()) {
            toggleOnlineState(true, token, currentUser.chatChannels, currentUser.token != token)
        }
    }

    companion object {
        private const val TAG = "ChatOnlineStateListener"
    }

}