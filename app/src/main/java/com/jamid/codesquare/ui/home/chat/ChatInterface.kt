package com.jamid.codesquare.ui.home.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import com.jamid.codesquare.removeAtIf
import kotlinx.coroutines.tasks.await

object ChatInterface {


    data class Zza(var channel: ChatChannel, var contributors: List<User>)
    data class Zzb(var channel: ChatChannel, var messages: List<Message>)

    private val _currentData = MutableLiveData<Result<List<Zza>>>()
    val currentData: LiveData<Result<List<Zza>>> = _currentData

    private lateinit var currentUserId: String
    val channelMessagesMap = MutableLiveData<List<Zzb>>().apply { value = emptyList() }
    private var isChatChannelsListenerAdded = false

    /*
    * Add a listener to listen to all the chat channels respective to the current user
    * */
    private fun addChannelMessagesListener(channels: List<ChatChannel>) {
        if (!isChatChannelsListenerAdded) {
            isChatChannelsListenerAdded = true
            for (channel in channels) {
                addChannelMessagesListener(channel)
            }
        }
    }

    /*
    * Add a listener for messages in every chat chat channel
    * */
    fun addChannelMessagesListener(chatChannel: ChatChannel) {
        Firebase.firestore.collection("chatChannels")
            .document(chatChannel.chatChannelId)
            .collection("messages")
            .limit(10)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    val messages = value.toObjects(Message::class.java)
                    val newList = channelMessagesMap.value!!.toMutableList()

                    val existing = newList.find {
                        it.channel.chatChannelId == chatChannel.chatChannelId
                    }

                    if (existing == null) {
                        newList.add(Zzb(chatChannel, messages))
                    } else {
                        existing.messages = messages
                        newList.removeAtIf {
                            it.channel.chatChannelId == chatChannel.chatChannelId
                        }

                        newList.add(existing)
                    }
                    channelMessagesMap.postValue(newList)
                }
            }

    }


    /**
    * Downloading all chat channels related to this id and all contributors related to all
    * chat channels in background
    * @param userId The id of the current user
    * */
    suspend fun initialize(userId: String) {
        currentUserId = userId
        try {
            val list = mutableListOf<Zza>()

            val task = Firebase.firestore.collection(CHAT_CHANNELS)
                .whereArrayContains(CONTRIBUTORS, currentUserId)
                .get()

            val channelsSnapshot = task.await()
            if (channelsSnapshot.isEmpty)
                return

            val chatChannels = channelsSnapshot.toObjects(ChatChannel::class.java)

            addChannelMessagesListener(chatChannels)

            for (channel in chatChannels) {
                try {
                    val task1 = Firebase.firestore.collection(USERS)
                        .whereArrayContains(CHAT_CHANNELS, channel.chatChannelId)
                        .get()

                    val usersSnapshot = task1.await()
                    if (usersSnapshot.isEmpty)
                        continue

                    val users = usersSnapshot.toObjects(User::class.java)

                    val item = Zza(channel, users)
                    list.add(item)

                } catch (e: Exception) {
                    Log.e(TAG, e.localizedMessage.orEmpty())
                }
            }

            if (list.size == chatChannels.size) {
                _currentData.postValue(Result.Success(list))
            } else {
                throw Exception("Couldn't finish download of chat channels and contributors")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage.orEmpty())
            _currentData.postValue(Result.Error(e))
        }
    }

    suspend fun getLatestMessages(chatChannelId: String, lastMessage: Message): Result<QuerySnapshot> {
        val ref = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection("messages").document(lastMessage.messageId)

        return when (val result = FireUtility.getDocument(ref)) {
            is Result.Error -> Result.Error(result.exception)
            is Result.Success -> {
                val doc = result.data

                if (doc.exists()) {
                    try {
                        val task = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
                            .collection("messages")
                            .startAfter(doc)
                            .get()

                        val newMessagesCollection = task.await()

                        Result.Success(newMessagesCollection)
                    } catch (e: Exception) {
                        Result.Error(e)
                    }

                } else {
                    Result.Error(Exception("Document with id $chatChannelId doesn't exist."))
                }

            }
        }
    }

    fun addChannelsListener(onNewData: (List<ChatChannel>) -> Unit) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    val chatChannels = value.toObjects(ChatChannel::class.java)
                    onNewData(chatChannels)
                }

            }
    }

    const val TAG = "ChatInterface"
    private const val CHAT_CHANNELS = "chatChannels"
    private const val USERS = "users"
    private const val CONTRIBUTORS = "contributors"

}