package com.jamid.codesquare.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import com.google.firebase.firestore.Query
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.*
import com.jamid.codesquare.db.ChatRepository
import com.jamid.codesquare.db.CodesquareDatabase
import com.jamid.codesquare.db.MessageRemoteMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Responsible for handling network and database requests for chats only
 *
 * */
@OptIn(ExperimentalPagingApi::class)
class ChatViewModel(context: Context): ViewModel() {

    private val chatRepository: ChatRepository

    init {
        val db = CodesquareDatabase.getInstance(context)
        chatRepository = ChatRepository(viewModelScope, context, db)
    }

    /**
     * List of chat channels for ForwardFragment
     * */
    private val _forwardList = MutableLiveData<List<ChatChannel>>()
    val forwardList: LiveData<List<ChatChannel>> = _forwardList

    /**
     * Flag for holding the current state of the chat, whether messages are being selected or not
     * */
    var isSelectModeOn = false


    /**
     * Placeholder for the current message that has been selected to be replied to
     * */
    private val _replyMessage = MutableLiveData<Message>().apply { value = null }
    val replyMessage: LiveData<Message> = _replyMessage

    /**
     * List of images to be uploaded in the current chat
     * */
    private val _chatImagesUpload = MutableLiveData<List<Uri>>()
    val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload


    /**
     * List of documents to be uploaded in the current chat
     * */
    private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
    val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload


    /**
     * To get messages for ChatFragment in paged order
     *
     * @param chatChannelId Channel to get messages from local database
     * @param query FireBase query to get messages on boundary callbacks
     *
     * @return A flow of messages in paged order by position in local database by recent order
     * */
    fun getPagedMessages(chatChannelId: String, query: Query): Flow<PagingData<Message>> {
        return Pager(config =
        PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            maxSize = 150,
            prefetchDistance = 25,
            initialLoadSize= 40),
            remoteMediator = MessageRemoteMediator(
                query,
                chatRepository
            )
        ) {
            chatRepository.messageDao.getChannelPagedMessages(chatChannelId)
        }.flow.cachedIn(viewModelScope)
    }


    /**
     * To add channel to forward list that is accessed by ForwardFragment
     * @param chatChannel The chat channel to add to the list
     * */
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


    /**
     * To remove channel from the forward list which is accessed by ForwardFragment
     * @param chatChannel The chat channel to remove from the list
     * */
    fun removeChannelFromForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null && oldList.isNotEmpty()) {
            val newList = oldList.toMutableList()
            newList.remove(chatChannel)
            _forwardList.postValue(newList)
        }
    }

    /**
     * To clear all data present to list accessed by ForwardFragment
     * */
    fun clearForwardList() {
        _forwardList.postValue(emptyList())
    }

    /**
     * To upload message to firestore
     *
     * @param chatChannelId The chat channel id to where this project belongs
     * @param content Content of the text message
     * @param replyTo An optional message id attached to the current message to be send which is connected respectively
     * @param replyMessage An optional message attached to the current message in minified form
     * */
    fun sendTextMessage(chatChannelId: String, content: String, replyTo: String? = null, replyMessage: MessageMinimal? = null) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendTextMessage(chatChannelId, content, replyTo, replyMessage)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                chatRepository.insertMessage(result.data, true)
            }
        }
    }


    /**
     * To send multiple messages together, may include message of all type [image, document, text]
     *
     * @param chatChannelId The chat channel id to where this project belongs
     * @param listOfMessages The messages to be sent
     *
     * */
    fun sendMessagesSimultaneously(chatChannelId: String, listOfMessages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendMessagesSimultaneously(chatChannelId, listOfMessages)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {

                val messages = result.data
                chatRepository.insertMessages(messages)

                setChatUploadImages(emptyList())
                setChatUploadDocuments(emptyList())
            }
        }
    }

    /**
     * Get all channels for forward fragment based on the user id provided, from local database wrapped in livedata
     *
     * @param userId The userId to filter channels for ForwardFragment
     * @return A list of chat channel for ForwardFragment wrapped in livedata
     *
     * */
    fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return chatRepository.getForwardChannels("%$userId%")
    }

    /**
     * To send messages to selected chat channels as forward messages
     *
     * @param messages messages selected to be forwarded
     * @param channels list of chat channels to which the selected messages needs to be forwarded
     * @param onComplete A callback function that is invoked on completion of sending all the messages to their respective chat channels
     *
     * */
    fun sendForwardsToChatChannels(messages: List<Message>, channels: List<ChatChannel>, onComplete: (result: Result<List<Message>>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val result = FireUtility.sendMultipleMessageToMultipleChannels(messages, channels)
        when (result) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                val newMessages = result.data
                chatRepository.insertMessages(newMessages)
            }
        }
        onComplete(result)
    }

    /**
     * To report error related to chat
     *
     * @param error The error to be reported that is related to chat
     * */
    fun setCurrentError(error: Exception?) {
        Log.e(TAG, "setCurrentError: ${error?.localizedMessage}")
    }


    /**
     * To get the currently selected messages in a chat channel
     *
     * @param chatChannelId The channel id for which messages are being selected
     * @return A list of messages wrapped in livedata
     * */
    fun selectedMessages(chatChannelId: String): LiveData<List<Message>> {
        return chatRepository.selectedMessages(chatChannelId)
    }


    /**
     * To fill [chatImagesUpload] with images retrieved by the user
     * */
    fun setChatUploadImages(images: List<Uri>) {
        _chatImagesUpload.postValue(images)
    }

    /**
     * To fill [chatDocumentsUpload] with documents retrieved by the user
     * */
    fun setChatUploadDocuments(documents: List<Uri>) {
        _chatDocumentsUpload.postValue(documents)
    }


    /**
     * To get currently selected messages in a given chat channel
     *
     * @param chatChannelId Channel id for which messages are being selected
     * @param onComplete A callback function for completion of process
     *
     * */
    fun getCurrentlySelectedMessages(chatChannelId: String, onComplete: (List<Message>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(chatRepository.getCurrentlySelectedMessages(chatChannelId))
    }

    /**
     * To set message as attached reply for another message to be sent currently
     *
     * @param message The message to be attached
     * */
    fun setReplyMessage(message: Message?) {
        _replyMessage.postValue(message)
        if (message != null) {
            disableSelectMode(message.chatChannelId)
        }
    }


    /**
     * To get a live version of the current chat Channel wrapped in livedata from local database
     *
     * @param chatChannelId Id of the chat channel to retrieve
     * @return A reactive chat channel that reacts to changes in the local database
     *
     * */
    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return chatRepository.getReactiveChatChannel(chatChannelId)
    }

    /**
     * To disable select mode and update all messages that are in select mode in local database
     * @see isSelectModeOn
     * */
    fun disableSelectMode(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = false
        chatRepository.updateMessages(chatChannelId, MESSAGE_IDLE)
    }

    /**
     * To add images to already existing list [chatImagesUpload]
     *
     * @param images new images to be appended to the existing list
     * */
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

    /**
     * To add documents to already existing list [chatDocumentsUpload]
     *
     * @param documents new documents to be appended to the existing list
     * */
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

    /**
     * To enable select mode
     * @see isSelectModeOn
     *
     * @param firstSelectedMessage The message which invoked select mode
     * */
    fun enableSelectMode(firstSelectedMessage: Message) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = true
        chatRepository.updateMessages(firstSelectedMessage.chatChannelId, MESSAGE_READY)

        delay(300)

        firstSelectedMessage.state = MESSAGE_SELECTED
        updateMessage(firstSelectedMessage)
    }


    /**
     * To update a message in local database
     *
     * @param message The message to be updated
     * */
    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        Log.d(TAG, "Updating message: ${message.state}")
        chatRepository.updateMessage(message)
    }

    /**
     * To remove an image from the [chatImagesUpload] list
     *
     * @param position The position where the image is to be removed
     * */
    fun removeImageAtPosition(position: Int) {
        val existingList = chatImagesUpload.value
        if (existingList != null) {
            val currentList = existingList.toMutableList()
            currentList.removeAt(position)
            _chatImagesUpload.postValue(currentList)
        }
    }

    /**
     * To remove a document from the [chatDocumentsUpload] list
     *
     * @param position The position where the document is to be removed
     * */
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
