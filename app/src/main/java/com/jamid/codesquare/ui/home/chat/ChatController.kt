package com.jamid.codesquare.ui.home.chat

import androidx.paging.ExperimentalPagingApi

@ExperimentalPagingApi
class ChatController() {

    /*private var isInitialized = false
    private val database: CodesquareDatabase
    private val scope: CoroutineScope = activity.lifecycleScope
    private val errors = MutableLiveData<Exception>()

    init {
        val currentUser = Firebase.auth.currentUser
        database = CodesquareDatabase.getInstance(activity.applicationContext)
        if (currentUser != null) {
            isInitialized = true
            initialize(currentUser.uid)
        }
    }

    private fun processChatChannel(chatChannel: ChatChannel): ChatChannel {
        val lastMessage = chatChannel.lastMessage

        if (lastMessage != null && lastMessage.sender.isEmpty()) {

            scope.launch (Dispatchers.IO) {
                val sender = database.userDao().getUser(lastMessage.senderId)
                if (sender != null) {
                    lastMessage.sender = sender
                    chatChannel.lastMessage = lastMessage
                } else {
                    when (val result = FireUtility.getUser(lastMessage.senderId)) {
                        is Result.Error -> {
                            errors.postValue(result.exception)
                        }
                        is Result.Success -> {
                            val unknownContributor = result.data
                            database.userDao().insert(processUsers(unknownContributor).toList())
                            lastMessage.sender = unknownContributor
                            chatChannel.lastMessage = lastMessage
                        }
                        null -> {
                            Log.d(TAG, "Probably the document doesn't exist")
                        }
                    }
                }
            }

        }
        return chatChannel
    }

    private fun initialize(currentUserId: String) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .get()
            .addOnCompleteListener(onChannelsReceived)

        addChannelsListener(currentUserId)
    }

    private val onChannelsReceived = OnCompleteListener<QuerySnapshot> {
        if (it.isSuccessful) {
            val querySnapshot = it.result
            if (querySnapshot.isEmpty) {
                return@OnCompleteListener
            } else {

                val chatChannels = querySnapshot.toObjects(ChatChannel::class.java)
                val newListOfChatChannels = mutableListOf<ChatChannel>()
                for (chatChannel in chatChannels) {
                    newListOfChatChannels.add(processChatChannel(chatChannel))
                }

                scope.launch (Dispatchers.IO) {
                    database.chatChannelDao().insert(newListOfChatChannels)

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
                                    viewModel.insertChannelMessages(messages)
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
            .addSnapshotListener(mContext as MainActivity) { value, error ->

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

                    Log.d(TAG, "setChannelMessagesListener: NEW messages received")

                    val messages = querySnapshot.toObjects(Message::class.java)
                    val imagesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val documentsDir =  mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

                    if (imagesDir != null && documentsDir != null) {
                        if (viewModel is ChatViewModel) {
                            viewModel.insertChannelMessages(messages)
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

                        scope.launch (Dispatchers.IO) {
                            database.userDao().insert(processUsers(*contributors.toTypedArray()).toList())
                        }

                    }
                } else {
                    it.exception?.let { it1 -> Log.e(TAG, it1.localizedMessage!!) }
                }
            }
    }


    companion object {
        private const val TAG = "ChatController"
    }*/


}