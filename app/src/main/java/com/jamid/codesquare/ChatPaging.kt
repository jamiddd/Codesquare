package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.db.MainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ChatPaging(
    private val imagesDir: File,
    private val documentsDir: File,
    private val chatChannelId: String,
    private val repo: MainRepository,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "MyPaging"
    }

    init {
        setMessagesListener()
    }

    private val pageSize = 10
    private val zero: Long = 0
    private var anchorMessageTimeEnd = zero
    private var nextKey: String = ""

    private var isAppendOnProgress = false

    val snapshot = MutableLiveData<List<Message>>().apply { value = emptyList() }
    val hasReachedEnd = MutableLiveData<Boolean>().apply { value = false }
    val networkErrors = MutableLiveData<Exception>()

    private fun setMessagesListener() = scope.launch (Dispatchers.Default) {
        delay(3000)
        Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(pageSize.toLong())
            .addSnapshotListener { value, error ->
                if (error != null) {
                    networkErrors.postValue(error)
                    return@addSnapshotListener
                }

                if (value != null) {
                    scope.launch (Dispatchers.IO) {
                        val newMessages = repo.processMessages(imagesDir, documentsDir, value.toObjects(Message::class.java))
                        if (newMessages.isNotEmpty()) {
                            onNewMessages(newMessages)
                        }
                        repo.insertMessages(imagesDir, documentsDir, newMessages, true)
                    }
                }

            }

    }

    private suspend fun getLatestSnapshot(callback: (messages: List<Message>) -> Unit) {
        val messagesQuery = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        when (val result = FireUtility.getQuerySnapshot(messagesQuery)) {
            is Result.Error -> {
                networkErrors.postValue(result.exception)
            }
            is Result.Success -> {
                val messagesSnapshot = result.data
                val newMessages = repo.processMessages(imagesDir, documentsDir, messagesSnapshot.toObjects(Message::class.java))
                callback(newMessages)
                repo.insertMessages(imagesDir, documentsDir, newMessages, true)
            }
        }
    }

    private fun onNewMessages(messages: List<Message>) {
        val newSnapshot = snapshot.value.orEmpty().toMutableList()

        val anotherNewList = mutableListOf<Message>()
        anotherNewList.addAll(messages)
        anotherNewList.addAll(newSnapshot)

        anotherNewList.sortByDescending {
            it.createdAt
        }

        snapshot.postValue(anotherNewList.distinctBy { it.messageId })
    }

    fun refresh() = scope.launch (Dispatchers.IO) {
        // get latest snapshot on every refresh
        getLatestSnapshot {
            val messages = it
            if (messages.isNotEmpty()) {
                val lastMessage = messages.last()
                anchorMessageTimeEnd = lastMessage.createdAt
                nextKey = lastMessage.messageId

                if (messages.size < pageSize) {
                    hasReachedEnd.postValue(true)
                }
            }
            Log.d(TAG, messages.toString())
            snapshot.postValue(messages.distinctBy { it1-> it1.messageId })
        }
    }

    private suspend fun getAppendSnapshot(nextSnapshot: DocumentSnapshot, callback: (messages: List<Message>?) -> Unit) {
        val messagesQuery = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .startAfter(nextSnapshot)
            .limit(pageSize.toLong())

        when (val result = FireUtility.getQuerySnapshot(messagesQuery)) {
            is Result.Error -> {
                networkErrors.postValue(result.exception)
                callback(null)
            }
            is Result.Success -> {
                val messagesSnapshot = result.data
                val newMessages = repo.processMessages(imagesDir, documentsDir, messagesSnapshot.toObjects(Message::class.java))
                callback(newMessages)
                repo.insertMessages(imagesDir, documentsDir, newMessages, true)
            }
        }
    }

    fun append() = scope.launch (Dispatchers.IO) {
        if (!isAppendOnProgress) {
            Log.d("Messaging", "No append on progress, so proceeding on append.")
            isAppendOnProgress = true

            if (anchorMessageTimeEnd == zero) {
                Log.d("Messaging", "Stopping appending because anchor message time end is zero")
                hasReachedEnd.postValue(true)
                return@launch
            }

            val messages = repo.getMessagesOnAppend(chatChannelId, pageSize, anchorMessageTimeEnd)
            if (messages.isNotEmpty()) {

                Log.d("Messaging", "Messages received. count = ${messages.size}")

                val lastMessage = messages.last()
                nextKey = lastMessage.messageId

                Log.d("Messaging", "Next key - $nextKey")

                if (messages.size < pageSize) {
                    // either it's the last of them, or new items need to fetched

                    Log.d("Messaging", "Messages count is less than page size, it's probably the last batch")

                    val docRef = Firebase.firestore.collection("chatChannels")
                        .document(chatChannelId)
                        .collection("messages")
                        .document(nextKey)

                    when (val result = FireUtility.getDocument(docRef)) {
                        is Result.Error -> {
                            Log.d("Messaging", "Something went wrong while getting last snapshot.")
                            networkErrors.postValue(result.exception)
                        }
                        is Result.Success -> {

                            Log.d("Messaging", "Got the last Document. Getting messages after that.")

                            val nextSnapshot = result.data
                            if (nextSnapshot.exists()) {

                                getAppendSnapshot(nextSnapshot) {

                                    Log.d("Messaging", "Got the next batch result. Might be error and might be success.")

                                    scope.launch {
                                        delay(3000)
                                        isAppendOnProgress = false

                                        Log.d("Messaging", "Appending stopped. Ready for next one.")
                                    }

                                    if (it != null) {

                                        Log.d("Messaging", "Got the next batch of messages.")

                                        val newMessages = it
                                        val newSnapshot = snapshot.value.orEmpty().toMutableList()
                                        newSnapshot.addAll(newMessages)
                                        snapshot.postValue(newSnapshot.distinctBy { it.messageId })

                                        if (newMessages.size < pageSize) {

                                            Log.d("Messaging", "The next batch of messages is less than page size.")

                                            val lastMessage1 = newMessages.last()
                                            nextKey = lastMessage1.messageId
                                            hasReachedEnd.postValue(true)
                                        }
                                    } else {
                                        networkErrors.postValue(Exception("Something went wrong while getting snapshot."))
                                    }
                                }
                            }
                        }
                    }
                } else {

                    Log.d("Messaging", "Page size is not less, probably messages already there in cache.")

                    scope.launch {
                        delay(3000)
                        isAppendOnProgress = false

                        Log.d("Messaging", "Appending stopped. Ready for next one.")
                    }

                    val newSnapshot = snapshot.value.orEmpty().toMutableList()
                    newSnapshot.addAll(messages)
                    snapshot.postValue(newSnapshot)
                }

            } else {
                hasReachedEnd.postValue(true)
            }
        } else {
            Log.d("Messaging", "Append already in progress")
        }
    }

}