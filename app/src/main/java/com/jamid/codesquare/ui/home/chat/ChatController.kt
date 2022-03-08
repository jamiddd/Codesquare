package com.jamid.codesquare.ui.home.chat

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.paging.ExperimentalPagingApi
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.ChatViewModel

@ExperimentalPagingApi
class ChatController(private val viewModel: ViewModel, private val mContext: Context) {

    private var isInitialized = false

    private val onChannelsReceived = OnCompleteListener<QuerySnapshot> {
        if (it.isSuccessful) {
            val querySnapshot = it.result
            if (querySnapshot.isEmpty) {
                return@OnCompleteListener
            } else {
                val chatChannels = querySnapshot.toObjects(ChatChannel::class.java)
                if (viewModel is ChatViewModel) {
                    viewModel.insertChatChannels(chatChannels)

                }
                if (viewModel is MainViewModel) {
                    viewModel.insertChatChannels(chatChannels)
                }

                for (chatChannel in chatChannels) {
                    getChannelContributors(chatChannel)
                    setChannelMessagesListener(chatChannel)
                }
            }
        } else {
            it.exception?.localizedMessage?.let { it1 -> Log.e(TAG, it1) }
        }
    }

    init {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            isInitialized = true
            initialize(currentUser.uid)
        }
    }

    private fun initialize(currentUserId: String) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .get()
            .addOnCompleteListener(onChannelsReceived)

        addChannelsListener(currentUserId)
    }

    fun lateInitialize(currentUserId: String) {
        if (!isInitialized) {
            initialize(currentUserId)
        }
    }

    fun getLatestMessages(chatChannel: ChatChannel, lastMessage: Message, onComplete: () -> Unit) {

        val ref = Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .document(lastMessage.messageId)

        FireUtility.getDocument(ref) {
            if (it.isSuccessful) {
                Firebase.firestore.collection(CHAT_CHANNELS)
                    .document(chatChannel.chatChannelId)
                    .collection(MESSAGES)
                    .startAfter(it.result)
                    .get()
                    .addOnCompleteListener { it1 ->
                        if (it1.isSuccessful) {
                            val querySnapshot = it1.result
                            val imagesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            val documentsDir =  mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                            val messages = querySnapshot.toObjects(Message::class.java)
                            if (imagesDir != null && documentsDir != null) {
                                if (viewModel is ChatViewModel) {
                                    viewModel.insertChannelMessages(imagesDir, documentsDir, messages)
                                }

                                if (viewModel is MainViewModel) {
                                    viewModel.insertChannelMessages(imagesDir, documentsDir, messages)
                                }

                                onComplete()
                            }
                        } else {
                            it1.exception?.let { it2 -> Log.e(TAG, it2.localizedMessage!!) }
                        }
                    }
            } else {
                if (viewModel is ChatViewModel) {
                    viewModel.setCurrentError(it.exception)
                }

                if (viewModel is MainViewModel) {
                    viewModel.setCurrentError(it.exception)
                }

            }
        }
    }

    private fun addChannelsListener(currentUserId: String) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    val chatChannels = value.toObjects(ChatChannel::class.java)
                    if (viewModel is ChatViewModel) {
                        viewModel.insertChatChannels(chatChannels)
                    }

                    if (viewModel is MainViewModel) {
                        viewModel.insertChatChannels(chatChannels)
                    }
                }
            }
    }



    private fun setChannelMessagesListener(chatChannel: ChatChannel) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(chatChannel.contributorsCount)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage!!)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val messages = querySnapshot.toObjects(Message::class.java)
                    val imagesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val documentsDir =  mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

                    if (imagesDir != null && documentsDir != null) {
                        if (viewModel is ChatViewModel) {
                            viewModel.insertChannelMessages(imagesDir, documentsDir, messages)
                        }

                        if (viewModel is MainViewModel) {
                            viewModel.insertChannelMessages(imagesDir, documentsDir, messages)
                        }

                    }
                }
            }
    }

    private fun getChannelContributors(channel: ChatChannel) {
        Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNELS, channel.chatChannelId)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (!querySnapshot.isEmpty) {
                        val contributors = querySnapshot.toObjects(User::class.java)
                        if (viewModel is ChatViewModel) {
                            viewModel.insertUsers(*contributors.toTypedArray())
                        }

                        if (viewModel is MainViewModel) {
                            viewModel.insertUsers(*contributors.toTypedArray())
                        }

                    }
                } else {
                    it.exception?.let { it1 -> Log.e(TAG, it1.localizedMessage!!) }
                }
            }
    }


    companion object {
        private const val TAG = "ChatController"
    }


}