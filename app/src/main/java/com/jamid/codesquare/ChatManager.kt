package com.jamid.codesquare

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.User

@Suppress("ObjectPropertyName")
object ChatManager {

    // storing all the users that we will be needing in the process in a map
    private val allContributors = mutableMapOf<String, User>()
    val chatErrors = MutableLiveData<Exception>()

    private val _channelContributors = MutableLiveData<List<User>>()
    val channelContributors: LiveData<List<User>> = _channelContributors

    private val _currentUserChatChannels = MutableLiveData<List<ChatChannel>>()
    val currentUserChatChannels: LiveData<List<ChatChannel>> = _currentUserChatChannels

    fun getContributor(id: String): User? {
        return allContributors[id]
    }

    /**
     * The channels that has changed immediately. Need to observe these channels to
     * fetch new messages instantly.
     * */
    private val _immediateChange = MutableLiveData<List<ChatChannel>>()
    val immediateChange: LiveData<List<ChatChannel>> = _immediateChange

    fun initialize() {
        if (UserManager.isInitialized) {
            val currentUser = UserManager.currentUser
            setupContributors(currentUser)
        } else {
            Firebase.auth.currentUser?.uid?.let { currentUserId ->
                Firebase.firestore.collection(USERS).document(currentUserId)
                    .get()
                    .addOnSuccessListener {
                        if (it.exists()) {
                            val currentUser = it.toObject(User::class.java)!!
                            setupContributors(currentUser)
                        }
                    }.addOnFailureListener {
                        chatErrors.postValue(it)
                    }
            }
        }
    }

    fun lateInitialize(currentUser: User) {
        if (allContributors.isEmpty()) {
            setupContributors(currentUser)
        }
    }

    private fun setupContributors(currentUser: User) {
        // getting all the users that we will be needing for chats, we will not be relying on local database
        // this file should only contain channel and message related stuff, since it is our limitation that we cannot
        // use queries to get channel, message and user in the same query
        for (channel in currentUser.chatChannels) {
            Firebase.firestore.collection(USERS)
                .whereArrayContains(CHAT_CHANNELS, channel)
                .get()
                .addOnCompleteListener { userCollections ->
                    if (userCollections.isSuccessful) {
                        val contributors = userCollections.result.toObjects(User::class.java)
                        for (contributor in contributors) {
                            allContributors[contributor.id] = contributor
                        }

                        setChannelContributors(contributors)

                    } else {
                        chatErrors.postValue(userCollections.exception)
                    }
                }
        }
    }

    private fun processChatChannels(chatChannels: List<ChatChannel>): List<ChatChannel> {
        val newList = mutableListOf<ChatChannel>()
        for (chatChannel in chatChannels) {
            val lastMessage = chatChannel.lastMessage

            if (lastMessage != null && lastMessage.sender.isEmpty()) {

                // will only try once, in case it is null, it is very unfortunate because it should
                // not be possible.
                val lastMessageSender = allContributors[lastMessage.senderId]
                if (lastMessageSender != null) {
                    lastMessage.sender = lastMessageSender
                    chatChannel.lastMessage = lastMessage
                } else {
                    chatErrors.postValue(Exception("User was not found in ChatRepository."))
                }
            }

            newList.add(chatChannel)
        }

        return newList
    }


    fun setChatChannels(chatChannels: List<ChatChannel> = emptyList()) {
        val newList = processChatChannels(chatChannels)
        _currentUserChatChannels.postValue(newList)
    }

    fun setImmediateChannels(chatChannels: List<ChatChannel>) {
        val newList = processChatChannels(chatChannels)
        _immediateChange.postValue(newList)
    }

    fun setChannelContributors(contributors: List<User>) {
        _channelContributors.postValue(contributors)
    }

}