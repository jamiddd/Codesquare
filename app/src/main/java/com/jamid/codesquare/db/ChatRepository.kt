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
import kotlinx.coroutines.*
import java.io.File

class ChatRepository(val db: CodesquareDatabase, private val scope: CoroutineScope, context: Context) {

    val messageDao = db.messageDao()
    private val chatChannelDao = db.chatChannelDao()
    val errors = MutableLiveData<Exception>()
    private var currentUserId: String = ""

    private val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    private val documentsDir =  context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

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

    private fun insertChatChannels(chatChannels: List<ChatChannel>) = scope.launch (Dispatchers.IO) {
        chatChannelDao.insert(chatChannels)
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

    fun selectedMessages(channelId: String): LiveData<List<Message>> {
        return messageDao.selectedMessages(channelId)
    }

    fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return chatChannelDao.getForwardChannels(userId)
    }

    private fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
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

            message.isCurrentUserMessage = currentUserId == message.senderId

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

    fun clearChatChannels() = scope.launch (Dispatchers.IO) {
        chatChannelDao.clearTable()
    }


    companion object {
        private const val TAG = "ChatRepository"
    }

}