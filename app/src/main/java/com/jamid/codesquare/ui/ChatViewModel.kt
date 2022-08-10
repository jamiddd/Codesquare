package com.jamid.codesquare.ui

import android.content.Context
import androidx.lifecycle.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.db.CollabDatabase
import com.jamid.codesquare.db.MainRepository
import com.jamid.codesquare.document
import com.jamid.codesquare.getMediaItemsFromMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// something simple
@Suppress("UNCHECKED_CAST")
class ChatViewModelFactory(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(context) as T
    }
}

class ChatViewModel(context: Context): ViewModel() {

    /* private val _currentSendMode = MutableLiveData<ChatSendMode>().apply { value = ChatSendMode.Normal }
     val currentSendMode: LiveData<ChatSendMode> = _currentSendMode*/

    private val _replyMessage = MutableLiveData<Message>().apply { value = null }
    val replyMessage: LiveData<Message> = _replyMessage

    /*fun setChatMode(mode: ChatSendMode) {
        _currentSendMode.postValue(mode)
    }*/

    private val db: CollabDatabase = CollabDatabase.getInstance(context)
    private val repo: MainRepository = MainRepository.getInstance(db)

    fun setReplyMessage(message: Message?) {
        _replyMessage.postValue(message)
    }

    suspend fun getMultimediaMessagesSync(
        chatChannelId: String,
        after: Long? = null
    ): List<Message> {
        return if (after != null) {
            db.messageDao().getMultiMediaMessagesSyncAfter(chatChannelId, after)
        } else {
            db.messageDao().getMultiMediaMessagesSync(chatChannelId)
        }
    }

    private val _chatMediaList = MutableLiveData<List<MediaItemWrapper>>()
    val chatMediaList: LiveData<List<MediaItemWrapper>> = _chatMediaList

    val chatPhotosList: LiveData<List<MediaItemWrapper>> = Transformations.map(chatMediaList) {
        it.filter { it1 ->
            it1.mediaItem.type != document
        }
    }

    val chatDocumentsList: LiveData<List<MediaItemWrapper>> = Transformations.map(chatMediaList) {
        it.filter { it1 ->
           it1.mediaItem.type == document
        }
    }

    private val _currentChannel = MutableLiveData<ChatChannel?>()
    val currentChannel: LiveData<ChatChannel?> = _currentChannel

    fun setCurrentChannel(channel: ChatChannel?) = viewModelScope.launch (Dispatchers.IO) {
        _currentChannel.postValue(channel)
    }

    fun addMediaItemsToList(items: List<MediaItemWrapper>) {
        val existingList = mutableListOf<MediaItemWrapper>()
        if (_chatMediaList.value != null) {
            existingList.addAll(items)
            existingList.addAll(_chatMediaList.value!!)
            _chatMediaList.postValue(existingList.distinctBy {
                it.mediaItem.url
            })
        } else {
            _chatMediaList.postValue(items)
        }
    }

    private val _initialMediaMessages = MutableLiveData<List<Message>>()
    val initialMediaMessages: LiveData<List<Message>> = _initialMediaMessages

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return repo.getDocumentMessages(chatChannelId)
    }

    fun prefetchChatMediaItems(chatChannel: ChatChannel) = viewModelScope.launch (Dispatchers.IO) {
        setCurrentChannel(chatChannel)
        var initialMessages = getMultimediaMessagesSync(chatChannel.chatChannelId)
        if (initialMessages.isNotEmpty()) {
            _initialMediaMessages.postValue(initialMessages)
        } else {
            initialMessages = getDocumentMessages(chatChannel.chatChannelId)
            if (initialMessages.isNotEmpty()) {
                _initialMediaMessages.postValue(initialMessages)
            }
        }
    }

    fun convertInitialMessagesToMediaItems(context: Context, it: List<Message>) = viewModelScope.launch(Dispatchers.IO) {
        val mediaMessages = context.getMediaItemsFromMessages(it)
        addMediaItemsToList(mediaMessages)
        _initialMediaMessages.postValue(emptyList())
    }

    var hasReachedEnd = false

    fun fetchMoreMediaItems(context: Context, anchor: Long) = viewModelScope.launch (Dispatchers.IO) {
        if (_currentChannel.value != null) {
            val nextMessages = getMultimediaMessagesSync(_currentChannel.value!!.chatChannelId, anchor)

            hasReachedEnd = nextMessages.size < 40

            val mediaMessages = context.getMediaItemsFromMessages(nextMessages)
            addMediaItemsToList(mediaMessages)
        }
    }

    fun clearData() {
        _chatMediaList.postValue(emptyList())
        _currentChannel.postValue(null)
        _initialMediaMessages.postValue(emptyList())
        hasReachedEnd = false
    }


}