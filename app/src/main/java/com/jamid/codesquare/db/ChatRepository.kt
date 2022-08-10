package com.jamid.codesquare.db

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// something simple
class ChatRepository(
    val db: CollabDatabase,
    private val scope: CoroutineScope,
    private val context: Context
) {

    val messageDao = db.messageDao()
    private val chatChannelDao = db.chatChannelDao()
    val errors = MutableLiveData<Exception>()
    private var currentUserId: String = ""

    /* private val root = context.filesDir
     private val imagesDir = context.getDir("Images", Context.MODE_PRIVATE)
     private val documentsDir = context.getDir("Documents", Context.MODE_PRIVATE)
     private val videosDir = context.getDir("Videos", Context.MODE_PRIVATE)*/


    private var chatChannelsListenerRegistration: ListenerRegistration? = null

    init {
        Firebase.auth.addAuthStateListener {
            val currentFirebaseUser = it.currentUser
            if (currentFirebaseUser == null) {
                // is not signed in
            } else {
                // is signed in
                currentUserId = currentFirebaseUser.uid
                setChannelListener()

            }
        }

    }


    private fun setChannelListener() {
        chatChannelsListenerRegistration?.remove()
        chatChannelsListenerRegistration = Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(
                        TAG,
                        "initializeListeners: Something went wrong - ${error.localizedMessage}"
                    )
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {

                    clearChatChannels()

                    val chatChannels = value.toObjects(ChatChannel::class.java)
                    insertChatChannels(chatChannels)
                }
            }
    }

    private fun insertChatChannels(chatChannels: List<ChatChannel>) = scope.launch(Dispatchers.IO) {
        val currentUserId = UserManager.currentUserId
        val newListOfChatChannels = mutableListOf<ChatChannel>()
        for (chatChannel in chatChannels) {
            chatChannel.isNewLastMessage =
                chatChannel.lastMessage != null &&
                        chatChannel.lastMessage!!.senderId != currentUserId &&
                        !chatChannel.lastMessage!!.readList.contains(currentUserId)


            newListOfChatChannels.add(chatChannel)
        }
        chatChannelDao.insert(newListOfChatChannels)
    }

    /**
     * To insert messages in the local database
     *
     * @param messages list of messages to be inserted in the local database
     *
     * */
    fun insertChannelMessages(messages: List<Message>) = scope.launch(Dispatchers.IO) {
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

    var count = 0

    /**
     * To insert messages in local database
     *
     * @param messages list of messages to be inserted to local database
     * */
    fun insertMessages(messages: List<Message>, preProcessed: Boolean = false) =
        scope.launch(Dispatchers.IO) {
            if (!preProcessed) {
                messageDao.insertMessages(processMessages(messages))
            } else {
                messageDao.insertMessages(messages)
            }

            for (m in messages) {
                count++
            }

            Log.d(TAG, "insertMessages: $count")

        }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    suspend fun updateMessages(chatChannelId: String, state: Int) {
//        messageDao.updateMessages(chatChannelId, state)
    }

    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return chatChannelDao.getReactiveChatChannel(chatChannelId)
    }

    private fun processMessages(messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message

        for (message in messages) {
            if (message.type == text) {
                message.isDownloaded = true
            } else {
                val name = message.content + message.metadata?.ext
                val fullPath = message.type.toPlural() + "/" + message.chatChannelId
                getNestedDir(context.filesDir, fullPath)?.let {
                    message.isDownloaded = checkFileExists(it, name)
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
    private fun updateDeliveryListOfMessages(
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.updateDeliveryListOfMessages(currentUserId, messages, onComplete)
    }

    suspend fun getChatChannel(chatChannelId: String): ChatChannel? {
        return chatChannelDao.getChatChannel(chatChannelId)
    }

    fun clearChatChannels() = scope.launch(Dispatchers.IO) {
        chatChannelDao.clearTable()
    }


    companion object {
        private const val TAG = "ChatRepository"
    }

}