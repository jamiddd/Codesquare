package com.jamid.codesquare.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.data.*
import com.jamid.codesquare.db.CodesquareDatabase
import com.jamid.codesquare.db.MainRepository
import com.jamid.codesquare.db.MessageRemoteMediator
import com.jamid.codesquare.ui.home.chat.ChatController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Responsible for handling network and database requests for chats only
 *
 * */
@OptIn(ExperimentalPagingApi::class)
class ChatViewModel(context: Context): ViewModel() {

    private val repo: MainRepository
    private val chatController: ChatController

    init {
        val db = CodesquareDatabase.getInstance(context)
        repo = MainRepository(db)
        chatController = ChatController(this, context)
    }

    @ExperimentalPagingApi
    fun getPagedMessages(imagesDir: File, documentsDir: File, chatChannelId: String, query: Query): Flow<PagingData<Message>> {
        return Pager(config =
        PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            maxSize = 150,
            prefetchDistance = 25,
            initialLoadSize= 40),
            remoteMediator = MessageRemoteMediator(
                imagesDir,
                documentsDir,
                viewModelScope,
                query,
                repo
            )
        ) {
            repo.messageDao.getChannelPagedMessages(chatChannelId)
        }.flow.cachedIn(viewModelScope)
    }

    private val _forwardList = MutableLiveData<List<ChatChannel>>()
    val forwardList: LiveData<List<ChatChannel>> = _forwardList

    fun addChannelToForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null) {
            val newList = oldList.toMutableList()
            newList.add(chatChannel)
            _forwardList.postValue(newList)
        } else {
            _forwardList.postValue(listOf(chatChannel))
        }
    }

    fun removeChannelFromForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null && oldList.isNotEmpty()) {
            val newList = oldList.toMutableList()
            newList.remove(chatChannel)
            _forwardList.postValue(newList)
        }
    }

    fun clearForwardList() {
        _forwardList.postValue(emptyList())
    }

    fun sendTextMessage(externalImagesDir: File, externalDocumentsDir: File, chatChannelId: String, content: String, replyTo: String? = null, replyMessage: MessageMinimal? = null) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendTextMessage(chatChannelId, content, replyTo, replyMessage)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                repo.insertMessages(externalImagesDir, externalDocumentsDir, listOf(result.data))
                val chatChannel = repo.getLocalChatChannel(chatChannelId)
                if (chatChannel != null) {
                    chatChannel.lastMessage = result.data
                    chatChannel.updatedAt = result.data.createdAt

                    Log.d(TAG, chatChannel.toString())

                    repo.insertChatChannels(listOf(chatChannel))
                }
            }
        }
    }


    fun sendMessagesSimultaneously(imagesDir: File, documentsDir: File, chatChannelId: String, listOfMessages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendMessagesSimultaneously(chatChannelId, listOfMessages)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {


                val messages = result.data
                repo.insertMessages(imagesDir, documentsDir, messages)

                setChatUploadImages(emptyList())
                setChatUploadDocuments(emptyList())

                val chatChannel = repo.getLocalChatChannel(chatChannelId)

                if (chatChannel != null) {
                    chatChannel.lastMessage = messages.last()
                    chatChannel.updatedAt = messages.last().createdAt
                    repo.insertChatChannels(listOf(chatChannel))
                }
            }
        }
    }

    fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return repo.getForwardChannels("%$userId%")
    }

    fun sendForwardsToChatChannels(imagesDir: File, documentsDir: File, messages: List<Message>, channels: List<ChatChannel>, onComplete: (result: Result<List<Message>>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val result = FireUtility.sendMultipleMessageToMultipleChannels(messages, channels)
        when (result) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                val newMessages = result.data
                insertMessages(imagesDir, documentsDir, newMessages)

                for (i in channels.indices) {
                    val channelMessages = newMessages.filter {
                        it.chatChannelId == channels[i].chatChannelId
                    }
                    if (channelMessages.isNotEmpty()) {
                        val lastMessage = channelMessages.last()
                        lastMessage.chatChannelId = channels[i].chatChannelId
                        channels[i].lastMessage = lastMessage
                        channels[i].updatedAt = lastMessage.createdAt
                    }
                }

                insertChatChannels(channels)
            }
        }
        onComplete(result)
    }

    fun insertUsers(vararg users: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }

    fun setCurrentError(error: Exception?) {

    }

    // insert channel messages and also update the channel along with it
    fun insertChannelMessages(imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        val uid = Firebase.auth.currentUser?.uid
        if (messages.isNotEmpty() && uid != null) {

            val firstTimeMessages = messages.filter { message ->
                !message.deliveryList.contains(uid)
            }

            val alreadyDeliveredMessages = messages.filter { message ->
                message.deliveryList.contains(uid)
            }

            insertMessages(imagesDir, documentsDir, alreadyDeliveredMessages)

            // update the delivery list
            updateDeliveryListOfMessages(uid, firstTimeMessages) { it1 ->
                if (!it1.isSuccessful) {
                    setCurrentError(it1.exception)
                } else {
                    insertMessages(imagesDir, documentsDir, firstTimeMessages)
                }
            }
        }
    }

    fun insertChatChannels(chatChannels: List<ChatChannel>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertChatChannels(chatChannels)
    }

    private fun updateDeliveryListOfMessages(currentUserId: String, messages: List<Message>, onComplete: (task: Task<Void>) -> Unit) {
        repo.updateDeliveryListOfMessages(currentUserId, messages, onComplete)
    }

    private fun insertMessages(imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertMessages(imagesDir, documentsDir, messages)
    }

    var isSelectModeOn = false


    private val _replyMessage = MutableLiveData<Message>().apply { value = null }
    val replyMessage: LiveData<Message> = _replyMessage

    private val _chatImagesUpload = MutableLiveData<List<Uri>>()
    val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload

    private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
    val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload

    fun selectedMessages(chatChannelId: String): LiveData<List<Message>> {
        return repo.selectedMessages(chatChannelId)
    }

    fun setChatUploadImages(images: List<Uri>) {
        _chatImagesUpload.postValue(images)
    }

    fun setChatUploadDocuments(documents: List<Uri>) {
        _chatDocumentsUpload.postValue(documents)
    }

    fun getCurrentlySelectedMessages(chatChannelId: String, onComplete: (List<Message>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getCurrentlySelectedMessages(chatChannelId))
    }

    fun setReplyMessage(message: Message?) {
        _replyMessage.postValue(message)
        if (message != null) {
            disableSelectMode(message.chatChannelId)
        }
    }

    fun getLatestMessages(chatChannel: ChatChannel, lastMessage: Message, onComplete: () -> Unit) {
        chatController.getLatestMessages(chatChannel, lastMessage, onComplete)
    }

    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return repo.getReactiveChatChannel(chatChannelId)
    }

    fun disableSelectMode(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = false
        repo.updateMessages(chatChannelId, MESSAGE_IDLE)
    }

    fun addUploadingImages(images: List<Uri>) {
        val existingImages = chatImagesUpload.value
        if (existingImages != null) {
            val currentList = existingImages.toMutableList()
            currentList.addAll(images)
            _chatImagesUpload.postValue(currentList)
        } else {
            _chatImagesUpload.postValue(images)
        }
    }

    fun addUploadingDocuments(documents: List<Uri>) {
        val existingDocuments = chatDocumentsUpload.value
        if (existingDocuments != null) {
            val currentList = existingDocuments.toMutableList()
            currentList.addAll(documents)
            _chatDocumentsUpload.postValue(currentList)
        } else {
            _chatDocumentsUpload.postValue(documents)
        }
    }

    fun enableSelectMode(firstSelectedMessage: Message) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = true
        repo.updateMessages(firstSelectedMessage.chatChannelId, MESSAGE_READY)

        delay(300)

        firstSelectedMessage.state = MESSAGE_SELECTED
        updateMessage(firstSelectedMessage)
    }

    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        Log.d(TAG, "Updating message: ${message.state}")
        repo.updateMessage(message)
    }

    fun removeImageAtPosition(position: Int) {
        val existingList = chatImagesUpload.value
        if (existingList != null) {
            val currentList = existingList.toMutableList()
            currentList.removeAt(position)
            _chatImagesUpload.postValue(currentList)
        }
    }

    fun removeDocumentAtPosition(position: Int) {
        val existingList = chatDocumentsUpload.value
        if (existingList != null) {
            val currentList = existingList.toMutableList()
            currentList.removeAt(position)
            _chatDocumentsUpload.postValue(currentList)
        }
    }

    companion object {
        const val MESSAGE_IDLE = -1
        const val MESSAGE_SELECTED = 1
        const val MESSAGE_READY = 0
        private const val TAG = "ChatViewModel"
    }


}

@Suppress("UNCHECKED_CAST")
class ChatViewModelFactory(val context: Context): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(context) as T
    }
}