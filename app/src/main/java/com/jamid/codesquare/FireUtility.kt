package com.jamid.codesquare

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.jamid.codesquare.data.*
import kotlinx.coroutines.tasks.await
import java.io.File

object FireUtility {

    private const val TAG = "FireUtility"

    private fun getQuerySnapshot(query: Query, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val task = query.get()
        task.addOnCompleteListener(onComplete)
    }

    suspend fun fetchItems(
        query: Query,
        lim: Int = 20,
        lastSnapshot: DocumentSnapshot? = null
    ): Result<QuerySnapshot> {
        return if (lastSnapshot != null) {
            try {
                val task = query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .startAfter(lastSnapshot)
                    .limit(lim.toLong())
                    .get()

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            try {
                val task = query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(lim.toLong())
                    .get()

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    fun signIn(email: String, password: String, onComplete: (task: Task<AuthResult>) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(onComplete)
    }

    fun getDocument(
        documentRef: DocumentReference,
        onComplete: (task: Task<DocumentSnapshot>) -> Unit
    ) {
        documentRef.get()
            .addOnCompleteListener(onComplete)
    }

    suspend fun getDocument(documentRef: DocumentReference): Result<DocumentSnapshot> {
        return try {
            val task = documentRef.get()
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun signInWithGoogle(credential: AuthCredential, onComplete: (task: Task<AuthResult>) -> Unit) {
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(onComplete)
    }

    fun createAccount(
        email: String,
        password: String,
        onComplete: (task: Task<AuthResult>) -> Unit
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(onComplete)
    }

    fun uploadDocument(ref: DocumentReference, data: Any, onComplete: (task: Task<Void>) -> Unit) {
        ref.set(data)
            .addOnCompleteListener(onComplete)
    }

    suspend fun createProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val ref = Firebase.firestore.collection(PROJECTS).document(project.id)

        val listOfNames = mutableListOf<String>()
        for (i in project.images.indices) {
            listOfNames.add(randomId())
        }

        val downloadUris =
            uploadItems("${project.id}/images", listOfNames, project.images.map { it.toUri() })

        val downloadUrls = downloadUris.map { it.toString() }
        project.images = downloadUrls

        val chatChannelRef =
            Firebase.firestore.collection(CHAT_CHANNELS).document(project.chatChannel)

        val tokens = mutableListOf(currentUser.token)

        val chatChannel = ChatChannel(
            project.chatChannel,
            project.id,
            project.name,
            project.images.first(),
            project.contributors.size.toLong(),
            listOf(project.creator.userId),
            listOf(project.creator.userId),
            "",
            project.createdAt,
            project.updatedAt,
            null,
            tokens
        )

        val commentChannelRef = Firebase.firestore.collection(COMMENT_CHANNELS).document()
        val commentChannelId = commentChannelRef.id
        project.commentChannel = commentChannelId

        val commentChannel = CommentChannel(
            commentChannelId,
            project.id,
            project.id,
            project.name,
            project.createdAt,
            null
        )

        val batch = Firebase.firestore.batch()

        batch.set(chatChannelRef, chatChannel)
        batch.set(commentChannelRef, commentChannel)

        batch.set(ref, project)

        val userChanges = mapOf<String, Any?>(
            PROJECTS_COUNT to FieldValue.increment(1),
            PROJECTS to FieldValue.arrayUnion(project.id),
            CHAT_CHANNELS to FieldValue.arrayUnion(project.chatChannel)
        )

        batch.update(Firebase.firestore.collection(USERS).document(currentUser.id), userChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    private suspend fun uploadItems(
        locationPath: String,
        names: List<String>,
        items: List<Uri>
    ): List<Uri> {

        val listOfReferences = mutableListOf<StorageReference>()
        val listOfUploadTask = mutableListOf<UploadTask>()

        for (i in items.indices) {
            val ref = Firebase.storage.reference.child("$locationPath/${names[i]}")
            listOfReferences.add(ref)

            val task = ref.putFile(items[i])
            listOfUploadTask.add(task)
        }

        val listOfDownloadedImages = mutableListOf<Uri>()

        return try {
            for (t in listOfUploadTask.indices) {
                val task = listOfUploadTask[t]
                task.await() // upload complete

                val currentImageRef = listOfReferences[t]
                val newTask = currentImageRef.downloadUrl

                val imageDownloadUri = newTask.await() // got download uri
                listOfDownloadedImages.add(imageDownloadUri)
            }

            listOfDownloadedImages
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage!!)
            emptyList()
        }
    }

    fun uploadImage(locationId: String, image: Uri, onComplete: (image: Uri?) -> Unit) {
        val randomImageName = randomId()
        val ref = Firebase.storage.reference.child("images/$locationId/$randomImageName.jpg")
        ref.putFile(image)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    onComplete(it)
                }.addOnFailureListener {
                    onComplete(null)
                }
            }.addOnFailureListener {
                onComplete(null)
            }
    }

    fun updateUser2(
        changes: Map<String, Any?>,
        shouldUpdateProjects: Boolean = true,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        // updating user
        val mAuth = Firebase.auth

        if (mAuth.currentUser != null) {
            val currentUserRef = db.collection(USERS).document(mAuth.currentUser!!.uid)
            batch.update(currentUserRef, changes)

            // updating projects where the creator is current user
            if (shouldUpdateProjects) {
                val currentUser = UserManager.currentUser
                for (project in currentUser.projects) {
                    val ref = db.collection(PROJECTS).document(project)
                    val miniUser = currentUser.minify()
                    Log.d(TAG, miniUser.toString())
                    batch.update(ref, CREATOR, miniUser)
                }
            }

            batch.commit()
                .addOnCompleteListener(onComplete)
        }
    }

    fun checkIfUserNameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val query = Firebase.firestore.collection(USERS)
            .whereEqualTo(USERNAME, username)

        getQuerySnapshot(query, onComplete)
    }

    fun likeProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val projectRef = Firebase.firestore.collection(PROJECTS)
            .document(project.id)

        val batch = Firebase.firestore.batch()
        val userRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        batch.update(projectRef, LIKES, FieldValue.increment(1))
        batch.update(userRef, LIKED_PROJECTS, FieldValue.arrayUnion(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun dislikeProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val projectRef = Firebase.firestore.collection(PROJECTS)
            .document(project.id)

        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        val batch = Firebase.firestore.batch()

        batch.update(projectRef, LIKES, FieldValue.increment(-1))
        batch.update(currentUserRef, LIKED_PROJECTS, FieldValue.arrayRemove(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun saveProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef =
            Firebase.firestore.collection(USERS).document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()
        batch.update(currentUserRef, SAVED_PROJECTS, FieldValue.arrayUnion(project.id))
        val savedProjectRef = currentUserRef.collection(SAVED_PROJECTS).document(project.id)

        batch.set(savedProjectRef, project)
        batch.commit().addOnCompleteListener(onComplete)
    }

    fun unSaveProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()

        batch.update(currentUserRef, SAVED_PROJECTS, FieldValue.arrayRemove(project.id))

        batch.delete(currentUserRef.collection(SAVED_PROJECTS).document(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }


    /**
     * @param notificationId A notification to send along with the action of sending a
     * request to join the project
     * @param project Project for which the user is sending request to
     * @param onComplete Callback function for completion of sending project request
     *
     * User can send a project request to join a project. The creator of the project can see
     * this request. The request can be removed in the future.
     *
     * */
    fun joinProject(
        notificationId: String,
        project: Project,
        onComplete: (task: Task<Void>, projectRequest: ProjectRequest) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser
        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val projectRequestRef = db.collection(PROJECT_REQUESTS).document()
        val requestId = projectRequestRef.id

        val projectRef = db.collection(PROJECTS).document(project.id)
        val projectRequest = ProjectRequest(
            requestId,
            project.id,
            currentUser.id,
            project.creator.userId,
            project.minify(),
            currentUser.minify(),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            notificationId
        )

        val userChanges = mapOf(PROJECT_REQUESTS to FieldValue.arrayUnion(requestId))

        val projectChanges = mapOf(REQUESTS to FieldValue.arrayUnion(requestId))

        // create new project request
        batch.set(projectRequestRef, projectRequest)
            .update(currentUserRef, userChanges)
            .update(projectRef, projectChanges)
            .commit().addOnCompleteListener {
                onComplete(it, projectRequest)
            }
    }

    /**
     * @param projectRequest The project request to revoke
     * @param onComplete Callback function for undoing project request
     *
     * Undoing a project request which was sent earlier
    * */
    fun undoJoinProject(projectRequest: ProjectRequest, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserRef = db.collection(USERS).document(projectRequest.senderId)
        val projectRequestRef = db.collection(PROJECT_REQUESTS).document(projectRequest.requestId)
        val projectRef = db.collection(PROJECTS).document(projectRequest.projectId)

        val userChanges = mapOf(
            PROJECT_REQUESTS to FieldValue.arrayRemove(projectRequest.requestId)
        )

        val projectChanges = mapOf(
            REQUESTS to FieldValue.arrayRemove(projectRequest.requestId)
        )

        val requestNotificationRef =
            Firebase.firestore.collection(USERS).document(projectRequest.senderId).collection(
                NOTIFICATIONS
            ).document(projectRequest.notificationId)


        batch.update(projectRef, projectChanges)

        batch.update(currentUserRef, userChanges)

        batch.delete(projectRequestRef)

        batch.delete(requestNotificationRef)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * @param project The project associated with the given project request
     * @param projectRequest The project request to accept
     * @param onComplete Callback function for completion of accepting project request
     *
     * Accepting project request. The user who requested to join, will be added to the project.
     *
     * */
    fun acceptProjectRequest(
        project: Project,
        projectRequest: ProjectRequest,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        val senderRef = db.collection(USERS).document(projectRequest.senderId)
        val projectRequestRef = db.collection(PROJECT_REQUESTS).document(projectRequest.requestId)

        val senderChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayUnion(projectRequest.projectId),
            COLLABORATIONS_COUNT to FieldValue.increment(1),
            PROJECT_REQUESTS to FieldValue.arrayRemove(projectRequest.projectId),
            CHAT_CHANNELS to FieldValue.arrayUnion(project.chatChannel)
        )

        val currentUserNotificationRef =
            Firebase.firestore.collection(USERS).document(projectRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(projectRequest.notificationId)

        batch.addNewUserToProject(project.id, project.chatChannel, projectRequest.senderId, UserManager.currentUser.token)
            .updateParticularDocument(senderRef, senderChanges)
            .deleteParticularDocument(projectRequestRef)
            .deleteParticularDocument(currentUserNotificationRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * @param projectRequest The project request to reject
     * @param onComplete Callback function for rejecting project request
     *
     * Rejecting a project request.
     *
    * */
    fun rejectRequest(projectRequest: ProjectRequest, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val projectRef = db.collection(PROJECTS).document(projectRequest.projectId)
        val senderRef = db.collection(USERS).document(projectRequest.senderId)
        val requestRef = db.collection(PROJECT_REQUESTS).document(projectRequest.requestId)

        val projectChanges = mapOf(REQUESTS to FieldValue.arrayRemove(projectRequest.requestId))

        val senderChanges = mapOf(
            PROJECT_REQUESTS to FieldValue.arrayRemove(projectRequest.projectId)
        )
        val batch = db.batch()
        val requestNotificationRef =
            Firebase.firestore.collection(USERS).document(projectRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(projectRequest.notificationId)

        batch.update(projectRef, projectChanges)
            .update(senderRef, senderChanges)
            .delete(requestRef)
            .delete(requestNotificationRef)
            .commit()
            .addOnCompleteListener(onComplete)

    }


    /**
     * @param userId Id of the user to be liked
     * @param onComplete Callback function for liking the user
     *
     * Like the given user with userId
     *
    * */
    fun likeUser(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        val otherUserReference = db.collection(USERS).document(userId)
        val currentUserReference = db.collection(USERS).document(currentUser.id)

        batch.update(otherUserReference, mapOf(LIKES_COUNT to FieldValue.increment(1)))
            .update(currentUserReference, mapOf(LIKED_USERS to FieldValue.arrayUnion(userId)))
            .commit()
            .addOnCompleteListener(onComplete)
    }

    fun dislikeUser(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        val ref1 = db.collection(USERS).document(userId)
        batch.update(ref1, mapOf(LIKES_COUNT to FieldValue.increment(-1)))

        val ref2 = db.collection(USERS).document(currentUser.id)
        batch.update(ref2, mapOf(LIKED_USERS to FieldValue.arrayRemove(userId)))

        val existingList = currentUser.likedUsers.toMutableList()
        existingList.remove(userId)
        currentUser.likedUsers = existingList

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun sendComment(
        comment: Comment,
        parentCommentChannelId: String?,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        val commentCollectionRef = db.collection(COMMENT_CHANNELS)

        val commentRef = commentCollectionRef.document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        val newCommentChannel = CommentChannel(
            randomId(),
            comment.commentId,
            comment.projectId,
            comment.postTitle,
            System.currentTimeMillis(),
            null
        )
        val newCommentChannelRef = commentCollectionRef
            .document(newCommentChannel.commentChannelId)

        batch.set(newCommentChannelRef, newCommentChannel)
        comment.threadChannelId = newCommentChannel.commentChannelId
        batch.set(commentRef, comment)

        val parentCommentChannelRef = commentCollectionRef
            .document(comment.commentChannelId)

        val parentCommentChannelChanges = mapOf(LAST_COMMENT to comment)
        batch.update(parentCommentChannelRef, parentCommentChannelChanges)

        val projectRef = db.collection(PROJECTS).document(comment.projectId)
        val projectChanges = mapOf(COMMENTS to FieldValue.increment(1))
        batch.update(projectRef, projectChanges)

        // update the parent comment replies count
        if (comment.commentLevel.toInt() != 0) {
            val parentRef = commentCollectionRef
                .document(parentCommentChannelId!!).collection(COMMENTS)
                .document(comment.parentId)

            batch.update(parentRef, mapOf(REPLIES_COUNT to FieldValue.increment(1)))
        }

        batch.commit().addOnCompleteListener(onComplete)

    }

    fun dislikeComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore

        val batch = db.batch()

        val commentRef = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        batch.update(
            commentRef,
            mapOf(
                LIKES_COUNT to FieldValue.increment(-1),
                LIKES to FieldValue.arrayRemove(UserManager.currentUserId)
            )
        )

        val currentUserRef = db.collection(USERS).document(UserManager.currentUserId)

        batch.update(
            currentUserRef,
            mapOf(LIKED_COMMENTS to FieldValue.arrayRemove(comment.commentId))
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun likeComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val currentUserId = UserManager.currentUserId
        val batch = db.batch()

        val commentRef = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        batch.update(
            commentRef,
            mapOf(
                LIKES_COUNT to FieldValue.increment(1),
                LIKES to FieldValue.arrayUnion(currentUserId)
            )
        )

        val currentUserRef = db.collection(USERS).document(currentUserId)

        batch.update(
            currentUserRef,
            mapOf(LIKED_COMMENTS to FieldValue.arrayUnion(comment.commentId))
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    suspend fun sendTextMessage(
        chatChannelId: String,
        content: String,
        replyTo: String? = null,
        replyMessage: MessageMinimal? = null
    ): Result<Message> {
        return try {
            val currentUser = UserManager.currentUser
            val db = Firebase.firestore
            val batch = db.batch()

            val chatChannelRef = db.collection(CHAT_CHANNELS).document(chatChannelId)

            val ref = chatChannelRef.collection(MESSAGES).document()
            val messageId = ref.id

            val message = Message(
                messageId,
                chatChannelId,
                text,
                content,
                currentUser.id,
                currentUser.minify(),
                null,
                listOf(currentUser.id),
                listOf(currentUser.id),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                replyTo,
                replyMessage,
                false,
                isCurrentUserMessage = true,
                -1
            )

            batch.set(ref, message)

            val chatChannelChanges = mapOf(
                LAST_MESSAGE to message,
                UPDATED_AT to message.createdAt
            )

            batch.update(chatChannelRef, chatChannelChanges)

            val task = batch.commit()
            task.await()
            Result.Success(message)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    // size of the list must be 2 if text included and 1 if only media
    suspend fun sendMessagesSimultaneously(
        chatChannelId: String,
        listOfMessages: List<Message>
    ): Result<List<Message>> {
        val db = Firebase.firestore

        val lastMessage = listOfMessages.last()
        val isLastMessageTextMsg = lastMessage.type == text

        val chatChannelRef = db.collection(CHAT_CHANNELS).document(chatChannelId)

        val sample = listOfMessages.first()
        val updatedList = if (isLastMessageTextMsg && listOfMessages.size > 1) {
            val mediaMessages = listOfMessages.slice(0..listOfMessages.size - 2)
            val downloadedContents = if (sample.type == image) {
                uploadItems(
                    "$chatChannelId/images",
                    mediaMessages.map { it.content },
                    mediaMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems(
                    "$chatChannelId/documents",
                    mediaMessages.map { it.content },
                    mediaMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            }

            mediaMessages.mapIndexed { index, message ->
                message.metadata?.url = downloadedContents[index]
                message
            }

        } else {
            val downloadedContents = if (sample.type == image) {
                uploadItems(
                    "$chatChannelId/images",
                    listOfMessages.map { it.content },
                    listOfMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems(
                    "$chatChannelId/documents",
                    listOfMessages.map { it.content },
                    listOfMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            }
            listOfMessages.mapIndexed { index, message ->
                message.metadata?.url = downloadedContents[index]
                message
            }

        }.toMutableList()

        if (isLastMessageTextMsg) {
            updatedList.add(lastMessage)
        }

        return try {
            val batch = Firebase.firestore.batch()

            for (message in updatedList) {
                val ref = chatChannelRef.collection(MESSAGES).document()
                message.messageId = ref.id
                batch.set(ref, message)
            }

            val chatChannelChanges = mapOf(
                LAST_MESSAGE to updatedList.last(),
                UPDATED_AT to System.currentTimeMillis()
            )

            batch.update(chatChannelRef, chatChannelChanges)

            val task = batch.commit()
            task.await()

            Result.Success(updatedList)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    // name must be with extension
    fun downloadMedia(
        destinationFile: File,
        name: String,
        message: Message,
        onComplete: (task: Task<FileDownloadTask.TaskSnapshot>) -> Unit
    ) {
        val path = if (message.type == image) {
            "${message.chatChannelId}/images/$name"
        } else {
            "${message.chatChannelId}/documents/$name"
        }
        val objRef = Firebase.storage.reference.child(path)
        objRef.getFile(destinationFile).addOnCompleteListener(onComplete)
    }

    fun deleteComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val commentRef = Firebase.firestore
            .collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)
        commentRef.delete().addOnCompleteListener(onComplete)
    }

    suspend fun sendMultipleMessageToMultipleChannels(
        messages: List<Message>,
        channels: List<ChatChannel>
    ): Result<List<Message>> {
        return try {
            val currentUser = UserManager.currentUser
            val newMessages = mutableListOf<Message>()

            val db = Firebase.firestore
            val now = System.currentTimeMillis()

            val batch = db.batch()

            val imagesMessages = messages.filter { it.type == image }
            val imageUris = imagesMessages.map { it.content.toUri() }

            val documentMessages = messages.filter { it.type == document }
            val documentUris = documentMessages.map { it.content.toUri() }

            for (message in imagesMessages) {
                message.content = randomId()
            }

            for (message in documentMessages) {
                message.content = randomId()
            }

            channels.forEach {

                val downloadedImageUris = if (imagesMessages.isNotEmpty()) {
                    uploadItems(
                        "${it.chatChannelId}/images",
                        imagesMessages.map { it1 -> it1.content },
                        imageUris
                    )
                } else {
                    emptyList()
                }

                val downloadedDocumentUris = if (documentMessages.isNotEmpty()) {
                    uploadItems(
                        "${it.chatChannelId}/documents",
                        documentMessages.map { it1 -> it1.content },
                        documentUris
                    )
                } else {
                    emptyList()
                }

                for (i in downloadedImageUris.indices) {
                    imagesMessages[i].metadata!!.url = downloadedImageUris[i].toString()
                }

                for (i in downloadedDocumentUris.indices) {
                    documentMessages[i].metadata!!.url = downloadedDocumentUris[i].toString()
                }

                for (i in messages.indices) {
                    if (messages[i].type == image) {
                        val newMessage = imagesMessages.find { it1 ->
                            it1.messageId == messages[i].messageId
                        }
                        if (newMessage != null) {
                            messages[i].content = newMessage.content
                            messages[i].metadata!!.url = newMessage.metadata!!.url
                        }
                    }

                    if (messages[i].type == document) {
                        val newMessage = documentMessages.find { it1 ->
                            it1.messageId == messages[i].messageId
                        }
                        if (newMessage != null) {
                            messages[i].content = newMessage.content
                            messages[i].metadata!!.url = newMessage.metadata!!.url
                        }
                    }

                    val ref = db.collection(CHAT_CHANNELS)
                        .document(it.chatChannelId)
                        .collection(MESSAGES)
                        .document()

                    val newMessage = Message(
                        ref.id,
                        it.chatChannelId,
                        messages[i].type,
                        messages[i].content,
                        currentUser.id,
                        currentUser.minify(),
                        messages[i].metadata,
                        emptyList(),
                        emptyList(),
                        now,
                        now,
                        null,
                        null,
                        isDownloaded = false,
                        isCurrentUserMessage = true
                    )

                    newMessages.add(newMessage)

                    Log.d(TAG, newMessages.map { it1 -> it1.content }.toString())

                    batch.set(ref, newMessage)

                    if (i == messages.size - 1) {
                        batch.update(
                            db.collection(CHAT_CHANNELS).document(it.chatChannelId),
                            mapOf(LAST_MESSAGE to newMessage, UPDATED_AT to now)
                        )
                    }

                }
            }

            batch.commit().addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.d(TAG, it.exception?.localizedMessage.orEmpty())
                }
            }

            Result.Success(newMessages)

        } catch (e: Exception) {

            Result.Error(e)
        }
    }


    fun updateDeliveryListOfMessages(
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val batch = Firebase.firestore.batch()

        for (message in messages) {
            if (message.senderId != currentUserId) {
                val messageRef = Firebase.firestore.collection(CHAT_CHANNELS)
                    .document(message.chatChannelId)
                    .collection(MESSAGES)
                    .document(message.messageId)

                val changes = mapOf(DELIVERY_LIST to FieldValue.arrayUnion(currentUserId))
                val newList = message.deliveryList.addItemToList(currentUserId)
                message.deliveryList = newList

                batch.update(messageRef, changes)
            }
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun updateReadList(
        chatChannel: ChatChannel,
        message: Message,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore
        val batch = db.batch()
        val ref = db.collection(CHAT_CHANNELS)
            .document(message.chatChannelId)
            .collection(MESSAGES)
            .document(message.messageId)

        val newList = message.readList.addItemToList(currentUserId)
        message.readList = newList

        if (chatChannel.lastMessage!!.messageId == message.messageId) {
            batch.update(
                Firebase.firestore.collection(CHAT_CHANNELS)
                    .document(chatChannel.chatChannelId),
                mapOf(
                    "$LAST_MESSAGE.$READ_LIST" to FieldValue.arrayUnion(currentUserId)
                )
            )
        }

        batch.update(ref, mapOf(READ_LIST to FieldValue.arrayUnion(currentUserId)))

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun sendRegistrationTokenToServer(token: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            updateUser2(mapOf(TOKEN to token), false, onComplete)
        }
    }

    suspend fun sendReport(report: Report, onComplete: (task: Task<Void>) -> Unit) {
        val names = mutableListOf<String>()
        for (i in report.snapshots) {
            names.add(randomId())
        }

        val toBeUploadedImages = uploadItems("${report.id}/images", names, report.snapshots.map { it.toUri() })
        report.snapshots = toBeUploadedImages.map { it.toString() }

        Firebase.firestore.collection(REPORTS)
            .document(report.id)
            .set(report)
            .addOnCompleteListener(onComplete)

    }

    fun sendFeedback(feedback: Feedback, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(FEEDBACKS)
            .document(feedback.id)
            .set(feedback)
            .addOnCompleteListener(onComplete)
    }

    // the changes to the chat channel will be reflected in the local database because there is listener
    // attached to the channels
    fun setOtherUserAsAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
            .update(ADMINISTRATORS, FieldValue.arrayUnion(userId))
            .addOnCompleteListener(onComplete)
    }

    // the changes to the chat channel will be reflected in the local database because there is listener
    // attached to the channels
    fun removeUserFromAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
            .update(ADMINISTRATORS, FieldValue.arrayRemove(userId))
            .addOnCompleteListener(onComplete)
    }


    fun removeUserFromProject(
        user: User,
        projectId: String,
        chatChannelId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val contributorRef = Firebase.firestore
            .collection(USERS)
            .document(user.id)

        val batch = Firebase.firestore.batch()

        val userChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayRemove(projectId),
            COLLABORATIONS_COUNT to FieldValue.increment(-1),
            CHAT_CHANNELS to FieldValue.arrayRemove(chatChannelId)
        )

        val chatChannelRef = Firebase.firestore
            .collection(CHAT_CHANNELS)
            .document(chatChannelId)

        val channelChanges = mapOf(
            CONTRIBUTORS to FieldValue.arrayRemove(user.id),
            ADMINISTRATORS to FieldValue.arrayRemove(user.id),
            CONTRIBUTORS_COUNT to FieldValue.increment(-1),
            TOKENS to FieldValue.arrayRemove(user.token),
            UPDATED_AT to System.currentTimeMillis()
        )

        val projectRef = Firebase.firestore
            .collection(PROJECTS)
            .document(projectId)

        val projectChanges = mapOf(
            CONTRIBUTORS to FieldValue.arrayRemove(user.id)
        )

        batch.update(projectRef, projectChanges)
        batch.update(chatChannelRef, channelChanges)
        batch.update(contributorRef, userChanges)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * For inviting an user to the project created by current user
     *
     * @param project The project on which the current user wants to invite someone
     * @param receiverId The receiver of this invite
     * @param notificationId A notification associated with this invite
     * @param onComplete Callback function for completion of inviting user to project
    * */
    fun inviteUserToProject(
        project: Project,
        receiverId: String,
        notificationId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()
        val ref = Firebase.firestore.collection(USERS)
            .document(receiverId)
            .collection(INVITES)
            .document()

        val projectInvite = ProjectInvite(
            ref.id,
            project.id,
            receiverId,
            currentUser.id,
            currentUser.minify(),
            project.minify(),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            notificationId = notificationId
        )

        val currentUserChanges = mapOf(
            PROJECT_INVITES to FieldValue.arrayUnion(projectInvite.id)
        )

        batch.set(ref, projectInvite)
            .update(db.collection(USERS).document(project.creator.userId), currentUserChanges)
            .commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * To revoke an invite which was earlier sent by the current user and not yet taken action by the receiver
     *
     * @param invite The invite to revoke
     * @param onComplete Callback function for completion of revoking invite
    * */
    fun revokeInvite(invite: ProjectInvite, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val inviteRef = db.collection(USERS)
            .document(invite.receiverId)
            .collection(INVITES)
            .document(invite.id)


        val inviteNotificationRef =
            Firebase.firestore.collection(USERS).document(invite.receiverId)
                .collection(NOTIFICATIONS)
                .document(invite.notificationId)


        val currentUserRef = db.collection(USERS).document(invite.senderId)

        val currentUserChanges = mapOf(
            PROJECT_INVITES to FieldValue.arrayRemove(invite.id)
        )

        batch.delete(inviteNotificationRef)
            .delete(inviteRef)
            .update(currentUserRef, currentUserChanges)
            .commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * To get an existing invite by the current user
     *
     * @param projectId Project id associated with the invite
     * @param otherUserId The receiver of the invite
     * @param currentUserId The sender of the invite
     * @param onComplete Callback function for completion of getting existing invite
    * */
    fun getExistingInvite(
        projectId: String,
        otherUserId: String,
        currentUserId: String,
        onComplete: (Result<ProjectInvite>?) -> Unit
    ) {
        Firebase.firestore.collection(USERS)
            .document(otherUserId)
            .collection(INVITES)
            .whereEqualTo(SENDER_ID, currentUserId)
            .whereEqualTo(PROJECT_ID, projectId)
            .limit(1)
            .get()
            .addOnCompleteListener {
                val result = if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (!querySnapshot.isEmpty) {
                        // there were results, which means there is an existing invite with this detail
                        val projectInvite =
                            querySnapshot.toObjects(ProjectInvite::class.java).first()
                        Result.Success(projectInvite)
                    } else {
                        null
                    }
                } else {
                    it.exception?.let { it1 -> Result.Error(it1) }
                }

                onComplete(result)
            }
    }

    /**
     * To get an existing project request
     *
     * @param projectId Project id associated with the request
     * @param senderId The user who sent the request to current user
     * @param onComplete Callback function for getting existing project request
    * */
    fun getProjectRequest(
        projectId: String,
        senderId: String,
        onComplete: (Result<ProjectRequest>?) -> Unit
    ) {
        Firebase.firestore.collection(PROJECT_REQUESTS)
            .whereEqualTo(SENDER_ID, senderId)
            .whereEqualTo(PROJECT_ID, projectId)
            .limit(1)
            .get()
            .addOnCompleteListener {
                val result = if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (!querySnapshot.isEmpty) {
                        val projectRequest =
                            querySnapshot.toObjects(ProjectRequest::class.java).first()
                        Result.Success(projectRequest)
                    } else {
                        null
                    }
                } else {
                    it.exception?.let { it1 -> Result.Error(it1) }
                }
                onComplete(result)
            }
    }

    /**
     * To get a project from firestore, not intended for use in main thread
     * @see getProject use this method to get project from firestore in main thread
     * @param projectId The id of the project to fetch
     * @return Result of project data
    * */
    suspend fun getProject(projectId: String): Result<Project>? {
        val ref = Firebase.firestore.collection(PROJECTS).document(projectId)
        return when (val result = getDocument(ref)) {
            is Result.Error -> {
                Result.Error(result.exception)
            }
            is Result.Success -> {
                val snapshot = result.data
                if (snapshot.exists()) {
                    val project = snapshot.toObject(Project::class.java)!!
                    Result.Success(project)
                } else {
                    null
                }
            }
        }
    }

    /**
     * @param documentRef Reference of the document to be deleted
     * @param onComplete Callback for deletion (Optional).
     * */
    private fun deleteDocument(
        documentRef: DocumentReference,
        onComplete: ((task: Task<Void>) -> Unit)? = null
    ) {
        // passive one shot, low risk call
        val task = documentRef.delete()
        if (onComplete != null) {
            task.addOnCompleteListener(onComplete)
        }
    }


    /**
     * @param projectId Id of the project
     * @param onComplete Callback function for getting the project
    * */
    fun getProject(projectId: String, onComplete: (Result<Project>?) -> Unit) {
        val ref = Firebase.firestore.collection(PROJECTS).document(projectId)
        getDocument(ref) {
            if (it.isSuccessful) {
                if (it.result.exists()) {
                    onComplete(Result.Success(it.result.toObject(Project::class.java)!!))
                } else {
                    deleteDocument(it.result.reference)
                    onComplete(null)
                }
            } else {
                onComplete(it.exception?.let { it1 -> Result.Error(it1) })
            }
        }
    }

    /**
     * @param userId The id of the user to fetch
     * @return Result<User> Contains either the user or an exception due to failure.
     * This method is not intended to run on main thread
    * */
    suspend fun getUser(userId: String): Result<User>? {
        val ref = Firebase.firestore.collection(USERS).document(userId)
        return when (val result = getDocument(ref)) {
            is Result.Error -> {
                Result.Error(result.exception)
            }
            is Result.Success -> {
                val data = result.data
                val user = data.toObject(User::class.java)
                if (user != null) {
                    Result.Success(user)
                } else {
                    null
                }
            }
        }
    }

    /**
     * @param userId The id of the user to fetch
     * @param onComplete Callback function containing result of user data
     *
     * To get the user from firebase using userId [For use in main thread]
     * */
    fun getUser(userId: String, onComplete: (Result<User>?) -> Unit) {
        val ref = Firebase.firestore.collection(USERS).document(userId)
        getDocument(ref) {
            if (it.isSuccessful) {
                if (it.result.exists()) {
                    onComplete(Result.Success(it.result.toObject(User::class.java)!!))
                } else {
                    deleteDocument(it.result.reference)
                    onComplete(null)
                }
            } else {
                onComplete(it.exception?.let { it1 -> Result.Error(it1) })
            }
        }
    }

    /**
     * To archive a project
     * 1. The project must be removed from global projects directory and added to user's personal archive gallery
     * 2. The project cannot accept further invites as it will be unreachable, but the users who already sent request needs to be notified that the project has been archived
     * 3. The users who are linked to this project must also be notified that the project has been archived
     * 4. Chat and comment channels of the project should be archived too
     *
     * @param project The project to be archived
     * @param onComplete Callback function for archive function
     *
     * */
    fun archiveProject(
        project: Project,
        duration: Long = 30L * 24L * 60L * 60L * 1000L,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore

        val projectRef = db.collection(PROJECTS).document(project.id)
        val currentUserRef = db.collection(USERS).document(currentUserId)
        val archiveRef = currentUserRef.collection(ARCHIVE).document(project.id)
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(project.commentChannel)
        val chatChannelRef = db.collection(CHAT_CHANNELS).document(project.chatChannel)

        val batch = db.batch()
        // move the project from project directory to archive directory
        batch.delete(projectRef)

        project.expiredAt = System.currentTimeMillis() + duration
        batch.set(archiveRef, project)

        // changing the status of the CommentChannel and ChatChannel to archived
        val changes = mapOf(ARCHIVED to true)
        batch.update(commentChannelRef, changes)
        batch.update(chatChannelRef, changes)

        val currentUserChanges = mapOf(
            ARCHIVED_PROJECTS to FieldValue.arrayUnion(project.id),
            PROJECTS to FieldValue.arrayRemove(project.id)
        )

        batch.update(currentUserRef, currentUserChanges)

        // committing the changes
        batch.commit().addOnCompleteListener(onComplete)
    }


    /**
     * @param project Project ato be un-archived
     * @param onComplete Callback function for un-archiving the project
    * */
    fun unArchiveProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore

        val currentUserRef = db.collection(USERS).document(currentUserId)

        val projectRef = db.collection(PROJECTS).document(project.id)
        val archivedProjectRef = currentUserRef.collection(ARCHIVE).document(project.id)
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(project.commentChannel)
        val chatChannelRef = db.collection(CHAT_CHANNELS).document(project.chatChannel)

        val batch = db.batch()
        project.expiredAt = -1
        batch.set(projectRef, project)

        batch.delete(archivedProjectRef)

        // changing the status of the CommentChannel and ChatChannel to archived
        val changes = mapOf(ARCHIVED to false)
        batch.update(commentChannelRef, changes)
        batch.update(chatChannelRef, changes)

        val currentUserChanges = mapOf(
            ARCHIVED_PROJECTS to FieldValue.arrayRemove(project.id),
            PROJECTS to FieldValue.arrayUnion(project.id)
        )

        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener(onComplete)

    }


    /**
     * To get a comment from firestore collection
     *
     * @param commentId Id of the comment to be fetched
     * @param commentChannelId Optional param to get comment if the comment channel is known
     * @param onComplete Callback function for getting comment
    * */
    fun getComment(
        commentId: String,
        commentChannelId: String? = null,
        onComplete: (Result<Comment>?) -> Unit
    ) {
        if (commentChannelId != null) {
            val ref = Firebase.firestore.collection(COMMENT_CHANNELS).document(commentId)
            ref.get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val documentSnapshot = it.result
                        if (documentSnapshot.exists()) {
                            val comment = documentSnapshot.toObject(Comment::class.java)!!
                            onComplete(Result.Success(comment))
                        } else {
                            onComplete(null)
                        }
                    } else {
                        it.exception?.let { it1 -> Result.Error(it1) }
                    }
                }
        } else {
            val query = Firebase.firestore.collectionGroup(COMMENTS)
                .whereEqualTo(COMMENT_ID, commentId)
                .limit(1)

            query.get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val querySnapshot = it.result
                        if (querySnapshot.isEmpty) {
                            onComplete(null)
                        } else {
                            val comment = querySnapshot.first().toObject(Comment::class.java)
                            onComplete(Result.Success(comment))
                        }
                    } else {
                        onComplete(it.exception?.let { it1 -> Result.Error(it1) })
                    }
                }
        }
    }

    /**
     * To delete a notification in firebase
     *
     * @param notification The notification to be deleted
     * @param onComplete Callback function for deleting notification
    * */
    fun deleteNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection(USERS).document(UserManager.currentUserId)
            .collection(NOTIFICATIONS).document(notification.id)
        ref.delete().addOnCompleteListener(onComplete)
    }

    /**
     * @param projectRequest Project request sent by other user
     * @param onComplete Callback function for deleting project request
     *
     * Deleting project request
     * */
    fun deleteProjectRequest(
        projectRequest: ProjectRequest,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val requestRef = db.collection(PROJECT_REQUESTS).document(projectRequest.requestId)
        val requestSenderRef = db.collection(USERS).document(projectRequest.senderId)
        val requestSenderChanges =
            mapOf(PROJECT_REQUESTS to FieldValue.arrayRemove(projectRequest.requestId))

        val batch = db.batch()

        batch.updateParticularDocument(requestSenderRef, requestSenderChanges)
            .deleteParticularDocument(requestRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * @param currentUser The user who is accepting the invite
     * @param projectInvite The invite received by current user
     * @param onComplete Callback function for accepting the project invite
     *
     * To accept the project invite, following changes to be made in the database:
     * 1. Project document must include the new contributor in contributors list and subsequently, the contributors count must also increase.
     * 2. Chat channel associated with this project must also reflect change such as, contributorsList, contributorsCount, updatedAt and registrationToken of current user.
     * 3. The current user document must include the project id in collaborations, chatChannel associated with this project, collaborationsCount and updatedAt.
     * 4. The senders document must remove the current project invite.
     * 5. Lastly, the project invite itself needs to be deleted from current user invites collection.
     * 6. For cleanup, delete the notification received by the current user because it is of no use.
     * */
    fun acceptProjectInvite(
        currentUser: User,
        projectInvite: ProjectInvite,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val project = projectInvite.project
        val currentUserId = currentUser.id
        val currentUserRegistrationToken = currentUser.token

        val batch = db.batch()
        val projectInviteSenderReference = db.collection(USERS).document(projectInvite.senderId)

        val projectInviteReference =
            db.collection(USERS).document(currentUserId).collection(INVITES)
                .document(projectInvite.id)
        batch.delete(projectInviteReference)

        val currentUserChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayUnion(project.id),
            COLLABORATIONS_COUNT to FieldValue.increment(1),
            CHAT_CHANNELS to FieldValue.arrayUnion(project.chatChannel),
            UPDATED_AT to System.currentTimeMillis()
        )

        val projectInviteSenderDocumentChanges = mapOf(
            PROJECT_INVITES to FieldValue.arrayRemove(projectInvite.id),
            UPDATED_AT to System.currentTimeMillis()
        )

        val inviteNotificationRef =
            Firebase.firestore.collection(USERS).document(projectInvite.receiverId)
                .collection(NOTIFICATIONS)
                .document(projectInvite.notificationId)

        batch.addNewUserToProject(
            project.id,
            project.chatChannel,
            currentUserId,
            currentUserRegistrationToken
        )
            .updateCurrentUser(currentUserId, currentUserChanges)
            .updateParticularDocument(
                projectInviteSenderReference,
                projectInviteSenderDocumentChanges
            )
            .deleteParticularDocument(projectInviteReference)
            .deleteParticularDocument(inviteNotificationRef)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    private fun WriteBatch.addNewUserToProject(
        projectId: String,
        chatChannelId: String,
        userId: String,
        token: String
    ): WriteBatch {
        val projectReference = Firebase.firestore.collection(PROJECTS).document(projectId)
        val changes1 = mapOf(
            CONTRIBUTORS to FieldValue.arrayUnion(userId),
            CONTRIBUTORS_COUNT to FieldValue.increment(1),
            UPDATED_AT to System.currentTimeMillis()
        )
        val chatChannelReference =
            Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
        val changes2 = mutableMapOf(
            CONTRIBUTORS_COUNT to FieldValue.increment(1),
            CONTRIBUTORS to FieldValue.arrayUnion(userId),
            UPDATED_AT to System.currentTimeMillis()
        )

        if (token.isNotEmpty()) {
            changes2[TOKENS] = FieldValue.arrayUnion(token)
        }

        return this.update(projectReference, changes1)
            .update(chatChannelReference, changes2)
    }

    private fun WriteBatch.updateCurrentUser(
        currentUserId: String,
        changes: Map<String, Any>
    ): WriteBatch {
        val currentUserReference = Firebase.firestore.collection(USERS).document(currentUserId)
        return this.update(currentUserReference, changes)
    }

    private fun WriteBatch.updateParticularDocument(
        reference: DocumentReference,
        changes: Map<String, Any>
    ): WriteBatch {
        return this.update(reference, changes)
    }

    private fun WriteBatch.deleteParticularDocument(ref: DocumentReference): WriteBatch {
        return this.delete(ref)
    }

    /**
     * To cancel a project invite
     * @param projectInvite The invite to be cancelled
     * @param onComplete Callback function for cancelling project invite
    * */
    fun cancelProjectInvite(
        projectInvite: ProjectInvite,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserId = UserManager.currentUserId

        val currentUserRef = db.collection(USERS).document(currentUserId)
        val projectInviteRef = currentUserRef.collection(INVITES).document(projectInvite.id)
        val senderRef = db.collection(USERS).document(projectInvite.senderId)

        val inviteNotificationRef =
            Firebase.firestore.collection(USERS).document(projectInvite.receiverId).collection(
                NOTIFICATIONS
            ).document(projectInvite.notificationId)

        batch.delete(inviteNotificationRef)
            .update(senderRef, mapOf(PROJECT_INVITES to FieldValue.arrayRemove(projectInvite.id)))
            .delete(projectInviteRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * To delete project invite
     * @param projectInvite Project invite to be deleted
     * @param onComplete Callback function for deleting project invite
    * */
    fun deleteProjectInvite(projectInvite: ProjectInvite, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val senderRef = db.collection(USERS).document(projectInvite.senderId)
        val projectInviteRef = db.collection(USERS)
            .document(projectInvite.receiverId)
            .collection(INVITES)
            .document(projectInvite.id)

        batch.update(senderRef, mapOf(PROJECT_INVITES to FieldValue.arrayRemove(projectInvite.id)))
        batch.delete(projectInviteRef)

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getRandomInterests(onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        Firebase.firestore.collection(INTERESTS)
            .document(INTERESTS_COLLECTION)
            .get()
            .addOnCompleteListener(onComplete)
    }

    fun uploadUser(user: User, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS).document(user.id)
            .set(user)
            .addOnCompleteListener(onComplete)
    }

    fun sendNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS).document(notification.receiverId)
            .collection(NOTIFICATIONS).document(notification.id)
            .set(notification)
            .addOnCompleteListener(onComplete)
    }

    fun sendNotificationToChannel(
        notification: Notification,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(notification.receiverId)
            .collection(NOTIFICATIONS)
            .document(notification.id)
            .set(notification)
            .addOnCompleteListener(onComplete)

    }


    fun checkIfNotificationExistsByContent(
        oldNotification: Notification,
        onComplete: (exists: Boolean, error: Exception?) -> Unit
    ) {
        Firebase.firestore.collection(USERS)
            .document(oldNotification.receiverId)
            .collection(NOTIFICATIONS)
            .whereEqualTo(TITLE, oldNotification.title)
            .whereEqualTo(CONTENT, oldNotification.content)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val exists = !it.result.isEmpty
                    onComplete(exists, null)
                } else {
                    onComplete(false, it.exception)
                }
            }
    }

    fun checkIfNotificationExistsById(
        receiverId: String,
        notificationId: String,
        onComplete: (exists: Boolean, error: Exception?) -> Unit
    ) {
        Firebase.firestore.collection(USERS).document(receiverId)
            .collection(NOTIFICATIONS)
            .document(notificationId)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val exists = it.result.exists()
                    onComplete(exists, null)
                } else {
                    onComplete(false, it.exception)
                }
            }
    }

    fun downloadAllUserProjects(onComplete: (result: Result<List<Project>>?) -> Unit) {
        val currentUser = UserManager.currentUser
        Firebase.firestore.collection(PROJECTS)
            .whereEqualTo("$CREATOR.$USER_ID", currentUser.id)
            .whereEqualTo(EXPIRED_AT, -1)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val projects = querySnapshot.toObjects(Project::class.java)
                        onComplete(Result.Success(projects))
                    } else {
                        onComplete(null)
                    }
                } else {
                    onComplete(it.exception?.let { it1 -> Result.Error(it1) })
                }
            }


    }

    fun updateNotification(receiverId: String, notificationId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .document(receiverId)
            .collection(NOTIFICATIONS)
            .document(notificationId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .document(notification.receiverId)
            .collection(NOTIFICATIONS)
            .document(notification.id)
            .update(mapOf(READ to true))
            .addOnCompleteListener(onComplete)
    }

    /**
     * @param project The project to be deleted
     * @param onComplete Callback function for deletion
     *
     * To delete a project
     * 1. Delete the project
     * 2. Delete the chat channel based on the project
     * 3. Remove project id from collaboration list from all the contributors
     * 4. Remove project id from project list from current user document
     * 5. Remove all project requests that has this project id
     * 6. Remove all invites if there are any
     * */
    fun deleteProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserId = UserManager.currentUserId

        // because this function can only be invoked from archive fragment
        val projectRef = db.collection(USERS)
            .document(project.creator.userId)
            .collection(ARCHIVE)
            .document(project.id)

        val chatChannelRef = db.collection(CHAT_CHANNELS)
            .document(project.chatChannel)

        val currentUserRef = db.collection(USERS).document(currentUserId)

        val commentChannelRef = db.collection(COMMENT_CHANNELS)
            .document(project.commentChannel)

        batch.delete(projectRef)
        batch.delete(chatChannelRef)
        batch.delete(commentChannelRef)
        batch.update(
            currentUserRef, mapOf(
                PROJECTS to FieldValue.arrayRemove(project.id),
                PROJECTS_COUNT to FieldValue.increment(-1)
            )
        )

        for (id in project.contributors) {
            val ref = db.collection(USERS).document(id)
            batch.update(ref, COLLABORATIONS, FieldValue.arrayRemove(project.id))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun postDeleteProject(
        projectRequests: List<ProjectRequest>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        for (request in projectRequests) {
            val requestRef = db.collection(PROJECT_REQUESTS).document(request.requestId)
            batch.delete(requestRef)

            val senderRef = db.collection(USERS).document(request.senderId)
            batch.update(senderRef, PROJECT_REQUESTS, FieldValue.arrayRemove(request.requestId))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getAllRequestsForProject(
        project: Project,
        onComplete: (task: Result<List<ProjectRequest>>?) -> Unit
    ) {
        Firebase.firestore.collection(PROJECT_REQUESTS)
            .whereEqualTo(PROJECT_ID, project.id)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val projectRequests = querySnapshot.toObjects(ProjectRequest::class.java)
                        onComplete(Result.Success(projectRequests))
                    } else {
                        onComplete(null)
                    }
                } else {
                    onComplete(it.exception?.let { it1 -> Result.Error(it1) })
                }
            }
    }

    fun getChatChannel(channelId: String, onComplete: (Result<ChatChannel>?) -> Unit) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(channelId)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val chatChannel = it.result.toObject(ChatChannel::class.java)!!
                        onComplete(Result.Success(chatChannel))
                    } else {
                        onComplete(null)
                    }
                } else {
                    onComplete(it.exception?.let { it1 -> Result.Error(it1) })
                }
            }
    }


    fun removeSubscriptions(onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(PURCHASES)
            .get()
            .addOnSuccessListener {
                val batch = Firebase.firestore.batch()
                if (!it.isEmpty) {
                    for (d in it) {
                        batch.delete(d.reference)
                    }
                }
                batch.commit().addOnCompleteListener(onComplete)
            }
    }

    fun updateProject(projectId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(PROJECTS).document(projectId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    suspend fun updateProject(
        project: Project,
        onComplete: (newProject: Project, task: Task<Void>) -> Unit
    ) {
        val names = mutableListOf<String>()
        for (img in project.images) {
            names.add(randomId())
        }

        val newListOfImages = mutableListOf<String>()

        val alreadyUploadedImages = project.images.filter {
            it.contains(HTTPS)
        }

        newListOfImages.addAll(alreadyUploadedImages)

        val toBeUploadedImages = project.images.filter {
            !it.contains(HTTPS)
        }

        val downloadedUris = uploadItems("${project.id}/images", names, toBeUploadedImages.map { it.toUri() })
        newListOfImages.addAll(downloadedUris.map { it.toString() })

        project.images = newListOfImages

        val projectChanges = mutableMapOf(
            NAME to project.name,
            CONTENT to project.content,
            IMAGES to newListOfImages,
            TAGS to project.tags,
            LOCATION to project.location,
            SOURCES to project.sources,
            UPDATED_AT to System.currentTimeMillis()
        )

        val primeThumbnail = newListOfImages.first()
        val channelChanges = mapOf(
            PROJECT_TITLE to project.name,
            PROJECT_IMAGE to primeThumbnail,
            UPDATED_AT to System.currentTimeMillis()
        )

        val db = Firebase.firestore
        val batch = db.batch()

        val chatChannelRef = db.collection(CHAT_CHANNELS).document(project.chatChannel)
        val projectRef = db.collection(PROJECTS).document(project.id)

        batch.update(chatChannelRef, channelChanges)
            .update(projectRef, projectChanges)
            .commit()
            .addOnCompleteListener {
                onComplete(project, it)
            }
    }

    fun sendRegistrationTokenToChatChannels(token: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val batch = Firebase.firestore.batch()

        for (channel in currentUser.chatChannels) {
            val ref = Firebase.firestore.collection(CHAT_CHANNELS).document(channel)
            val changes = mapOf(
                TOKENS to FieldValue.arrayUnion(token)
            )
            batch.update(ref, changes)
        }

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    fun getCurrentUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        Firebase.firestore.collection(USERS).document(userId)
            .get()
            .addOnCompleteListener(onComplete)

    }

    fun getContributors(channel: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNELS, channel)
            .get()
            .addOnCompleteListener(onComplete)
    }

    suspend fun getContributors(channel: String): Result<List<User>> {
        return try {
            val task = Firebase.firestore.collection(USERS)
                .whereArrayContains(CHAT_CHANNELS, channel)
                .get()

            val result = task.await()

            val contributors = result.toObjects(User::class.java)
            Result.Success(contributors)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun updateChatChannel(channelId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(channelId)
            .update(changes).addOnCompleteListener(onComplete)
    }

    fun updateComment(commentChannelId: String, commentId: String, changes: Map<String, Any?>, onComplete: (Task<Void>) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS).document(commentChannelId)
            .collection(COMMENTS).document(commentId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateCommentChannel(commentChannelId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(commentChannelId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateProjectRequest(requestId: String, changes: Map<String, Any?>, onComplete: (Task<Void>) -> Unit) {
        Firebase.firestore.collection(PROJECT_REQUESTS).document(requestId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateMessage(chatChannelId: String, messageId: String, changes: Map<String, Any?>, onComplete: (Task<Void>) -> Unit) {
        Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannelId)
            .collection(MESSAGES)
            .document(messageId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateProjectInvite(receiverId: String, inviteId: String, changes: Map<String, Any?>, onUpdate: (Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS).document(receiverId)
            .collection(INVITES)
            .document(inviteId)
            .update(changes)
            .addOnCompleteListener(onUpdate)
    }

    fun getNotification(userId: String, notificationId: String, onComplete: (Result<Notification>?) -> Unit) {
        Firebase.firestore.collection(USERS).document(userId)
            .collection(NOTIFICATIONS)
            .document(notificationId)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val notification = it.toObject(Notification::class.java)!!
                    onComplete(Result.Success(notification))
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }


}