package com.jamid.codesquare.db

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ChatRepository(private val scope: CoroutineScope, context: Context, a: CodesquareDatabase): BaseRepository(a) {

    val messageDao = database.messageDao()
    private val chatChannelDao = database.chatChannelDao()

    val errors = MutableLiveData<Exception>()

    private val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    private val documentsDir =  context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

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

}