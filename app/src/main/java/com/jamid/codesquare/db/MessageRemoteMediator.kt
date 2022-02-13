package com.jamid.codesquare.db

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class MessageRemoteMediator(
    private val imagesDir: File,
    private val documentsDir: File,
    private val scope: CoroutineScope,
    query: Query,
    repo: MainRepository
): FirebaseRemoteMediator<Int, Message>(query, repo) {

    private val tag = "MessageRemoteMediator"

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)

        val currentUser = UserManager.currentUser

        val nonUpdatedMessages = messages.filter {
            !it.deliveryList.contains(currentUser.id)
        }

        repository.updateDeliveryListOfMessages(currentUser.id, nonUpdatedMessages) {
            if (it.isSuccessful) {
                scope.launch (Dispatchers.IO) {
                    repository.insertMessages(imagesDir, documentsDir, nonUpdatedMessages)
                }
            } else {
                Log.e(tag, "Error while updating delivery list of messages")
            }
        }

        repository.insertMessages(imagesDir, documentsDir, messages)
    }

    override suspend fun onRefresh() {

    }

}