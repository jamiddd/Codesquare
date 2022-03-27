package com.jamid.codesquare.db

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import kotlinx.coroutines.*
import java.io.File

class ChatRepository(db: CodesquareDatabase, private val scope: CoroutineScope, context: Context): BaseRepository(
    db
) {

    val messageDao = database.messageDao()
    private val chatChannelDao = database.chatChannelDao()

    private lateinit var currentUser: User

    private val userDao = database.userDao()

    val errors = MutableLiveData<Exception>()

    private val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    private val documentsDir =  context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

    private var chatChannelsListenerRegistration: ListenerRegistration? = null
    private val allContributors = mutableMapOf<String, User>()

    init {
        Firebase.auth.addAuthStateListener {
            val currentFirebaseUser = it.currentUser
            if (currentFirebaseUser == null) {
                // is not signed in
            } else {
                // is signed in
                FireUtility.getCurrentUser(currentFirebaseUser.uid) { currentUserDocumentSnapshotTask ->
                    if (currentUserDocumentSnapshotTask.isSuccessful) {
                        val currentUserDocumentSnapshot = currentUserDocumentSnapshotTask.result
                        if (currentUserDocumentSnapshot != null && currentUserDocumentSnapshot.exists()) {
                            val currentUser = currentUserDocumentSnapshot.toObject(User::class.java)
                            if (currentUser != null) {
                                this.currentUser = currentUser
                                setupContributors(currentUser.chatChannels)
                            }
                        } else {
                            Log.d(TAG, "OnInit: Either the current user document snapshot is null or doesn't exist.")
                        }
                    } else {
                        Log.e(
                            TAG,
                            "OnInit: ${currentUserDocumentSnapshotTask.exception?.localizedMessage}"
                        )
                    }
                }

            }
        }
    }

    private fun setupContributors(chatChannels: List<String>) = scope.launch {
        val contributors = withContext(scope.coroutineContext) { prefetchRelatedUsers(chatChannels) }
        for (contributor in contributors) {
            allContributors[contributor.id] = contributor
        }
        withContext(scope.coroutineContext) { setChannelListener() }
        insertUsers(contributors)
    }

    private suspend fun prefetchRelatedUsers(channels: List<String>): List<User> {

        val users = mutableListOf<User>()

        for (channel in channels) {
            when (val result = FireUtility.getContributors(channel)) {
                is Result.Error -> {
                    Log.e(TAG, "prefetchRelatedUsers: ${result.exception.localizedMessage}")
                }
                is Result.Success -> {
                    users.addAll(result.data)
                }
            }
        }

        return users
    }

    private fun processUsers(users: List<User>): List<User> {
        val newList = mutableListOf<User>()
        for (user in users) {
            newList.add(processUser(user))
        }
        return newList
    }

    private fun processUser(user: User): User {
        user.isCurrentUser = currentUser.id == user.id
        user.isLiked = currentUser.likedUsers.contains(user.id)
        return user
    }

    private fun insertUser(user: User) = scope.launch (Dispatchers.IO) {
        userDao.insert(processUser(user))
    }

    private fun insertUsers(users: List<User>) = scope.launch (Dispatchers.IO) {
        userDao.insert(processUsers(users))
    }

    private fun setChannelListener() {
        chatChannelsListenerRegistration?.remove()
        chatChannelsListenerRegistration = Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUser.id)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(
                        TAG,
                        "initializeListeners: Something went wrong - ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {

                    clearChatChannels()

                    val chatChannels = value.toObjects(ChatChannel::class.java)
                    insertChatChannels(chatChannels)
                }
            }
    }

    private suspend fun processChatChannels(chatChannels: List<ChatChannel>): List<ChatChannel> {
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
                    when (val senderResult = FireUtility.getUser(lastMessage.senderId)) {
                        is Result.Error -> Log.e(
                            TAG,
                            "processChatChannels: ${senderResult.exception}"
                            )
                        is Result.Success -> {
                            allContributors[senderResult.data.id] = senderResult.data
                            lastMessage.sender = senderResult.data
                            chatChannel.lastMessage = lastMessage
                            insertUser(senderResult.data)
                        }
                        null -> Log.e(TAG, "processChatChannels: No user found whatsoever", )
                    }
                }
            }

            newList.add(chatChannel)
        }

        return newList
    }

    private fun insertChatChannels(chatChannels: List<ChatChannel>) = scope.launch (Dispatchers.IO) {
        chatChannelDao.insert(processChatChannels(chatChannels))
    }

    fun getLatestMessages(chatChannel: ChatChannel, onComplete: () -> Unit) {

        val ref = Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .document(chatChannel.lastMessage!!.messageId)

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
                            val messages = querySnapshot.toObjects(Message::class.java)
                            insertChannelMessages(messages)
                        } else {
                            errors.postValue(it1.exception)
                        }
                        onComplete()
                    }
            } else {
                errors.postValue(it.exception)
            }
        }
    }

    /**
     * To insert messages in the local database
     *
     * @param messages list of messages to be inserted in the local database
     *
     * */
    fun insertChannelMessages(messages: List<Message>) = scope.launch (Dispatchers.IO) {
        val uid = Firebase.auth.currentUser?.uid
        if (messages.isNotEmpty() && uid != null) {

            val firstTimeMessages = messages.filter { message ->
                !message.deliveryList.contains(uid)
            }

            val alreadyDeliveredMessages = messages.filter { message ->
                message.deliveryList.contains(uid)
            }

            insertMessages(alreadyDeliveredMessages)

            // update the delivery list
            updateDeliveryListOfMessages(uid, firstTimeMessages) { it1 ->
                if (!it1.isSuccessful) {
                    errors.postValue(it1.exception)
                } else {
                    insertMessages(firstTimeMessages)
                }
            }
        }
    }

    fun insertMessage(message: Message, preProcessed: Boolean = false) {
        insertMessages(listOf(message), preProcessed)
    }

    /**
     * To insert messages in local database
     *
     * @param messages list of messages to be inserted to local database
     * */
    fun insertMessages(messages: List<Message>, preProcessed: Boolean = false) = scope.launch (Dispatchers.IO) {
        if (!preProcessed) {
            messageDao.insertMessages(processMessages(imagesDir, documentsDir, messages))
        } else {
            messageDao.insertMessages(messages)
        }
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    suspend fun updateMessages(chatChannelId: String, state: Int) {
        messageDao.updateMessages(chatChannelId, state)
    }

    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return chatChannelDao.getReactiveChatChannel(chatChannelId)
    }

    suspend fun getCurrentlySelectedMessages(chatChannelId: String): List<Message> {
        return messageDao.getCurrentlySelectedMessages(chatChannelId)
    }

    fun selectedMessages(channelId: String): LiveData<List<Message>> {
        return messageDao.selectedMessages(channelId)
    }

    fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return chatChannelDao.getForwardChannels(userId)
    }

    private suspend fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
            // check if the message has user attached to it
            val user = ChatManager.getContributor(message.senderId)
            if (user != null) {
                message.sender = user
            } else {
                when (val result = FireUtility.getUser(message.senderId)) {
                    is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                    is Result.Success -> {
                        message.sender = processUsers(result.data).first()
                    }
                    else -> {
                        //
                    }
                }
            }

            // check if the media is already downloaded in the local folder
            if (message.type == image) {
                val name = message.content + message.metadata?.ext.orEmpty()
                val f = File(imagesDir, name)
                message.isDownloaded = f.exists()
            }

            if (message.type == document) {
                val name = message.content + message.metadata?.ext.orEmpty()
                val f = File(documentsDir, name)
                message.isDownloaded = f.exists()
            }

            if (message.type == text) {
                message.isDownloaded = true
            }

            if (message.replyTo != null) {
                val localMessage = messageDao.getMessage(message.replyTo!!)
                if (localMessage != null) {
                    message.replyMessage = localMessage.toReplyMessage()
                } else {
                    val docRef = Firebase.firestore.collection("chatChannels")
                        .document(message.chatChannelId)
                        .collection("messages")
                        .document(message.replyTo!!)

                    when (val result = FireUtility.getDocument(docRef)) {
                        is Result.Error -> {
                            Log.e(TAG, result.exception.localizedMessage.orEmpty())
                        }
                        is Result.Success -> {
                            if (result.data.exists()) {
                                val msg = result.data.toObject(Message::class.java)!!
                                val sender = ChatManager.getContributor(msg.senderId)
                                if (sender != null) {
                                    msg.sender = sender
                                    message.replyMessage = msg.toReplyMessage()
                                }
                            }
                        }
                    }
                }
            }

            message.isCurrentUserMessage = UserManager.currentUserId == message.senderId

        }

        return messages
    }

    /**
     * To update the users of each message's delivery list, to let the message know that a particular user has received the message
     *
     * @param currentUserId To update all messages that the messages have been delivered to the current user
     * @param messages list of messages to be updated
     * @param onComplete A callback function to know the state of completion of this particular process
     * */
    private fun updateDeliveryListOfMessages(currentUserId: String, messages: List<Message>, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.updateDeliveryListOfMessages(currentUserId, messages, onComplete)
    }

    suspend fun getChatChannel(chatChannelId: String): ChatChannel? {
        return chatChannelDao.getChatChannel(chatChannelId)
    }

    fun getMediaMessages(chatChannelId: String, limit: Int = 6): LiveData<List<Message>> {
        return messageDao.getMediaMessages(chatChannelId, limit)
    }

    fun clearChatChannels() = scope.launch (Dispatchers.IO) {
        chatChannelDao.clearTable()
    }


    companion object {
        private const val TAG = "ChatRepository"
    }

}