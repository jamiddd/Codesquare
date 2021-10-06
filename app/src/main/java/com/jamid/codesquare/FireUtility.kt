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

    const val TAG = "FireUtility"

    fun getQuerySnapshot(query: Query, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val task = query.get()
        task.addOnCompleteListener(onComplete)
    }

    suspend fun getQuerySnapshot(query: Query): Result<QuerySnapshot> {
        return try {
            val task = query.get()
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchItems(query: Query, lim: Int = 20, lastSnapshot: DocumentSnapshot? = null): Result<QuerySnapshot> {
        return if (lastSnapshot != null) {
            try {
                val task = query.startAfter(lastSnapshot)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
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

    fun getDocument(documentRef: DocumentReference, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
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

    fun createAccount(email: String, password: String, onComplete: (task: Task<AuthResult>) -> Unit) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(onComplete)
    }

    fun uploadDocument(ref: DocumentReference, data: Any, onComplete: (task: Task<Void>) -> Unit) {
        ref.set(data)
            .addOnCompleteListener(onComplete)
    }

    suspend fun uploadDocument(ref: DocumentReference, data: Any): Result<Any> {
        return try {
            val task = ref.set(data)
            task.await()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }


    fun updateDocument(ref: DocumentReference, data: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        ref.update(data)
            .addOnCompleteListener(onComplete)
    }

    suspend fun updateDocument(ref: DocumentReference, data: Map<String, Any?>): Result<Map<String, Any?>> {
        return try {
            val task = ref.update(data)
            task.await()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createProject(currentUser: User, project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("projects").document(project.id)

        val downloadUris = uploadImages(project.id, project.images.map { it.toUri() })

        val downloadUrls = downloadUris.map { it.toString() }
        project.images = downloadUrls

        val chatChannelRef = Firebase.firestore.collection("chatChannels").document()
        val chatChannelId = chatChannelRef.id

        project.chatChannel = chatChannelId

        val chatChannel = ChatChannel(
            chatChannelId,
            project.id,
            project.title,
            project.images.first(),
            project.contributors.size.toLong(),
            listOf(project.creator.userId),
            listOf(project.creator.userId),
            project.createdAt,
            project.updatedAt,
            null
        )

        val commentChannelRef = Firebase.firestore.collection("commentChannels").document()
        val commentChannelId = commentChannelRef.id
        project.commentChannel = commentChannelId

        val commentChannel = CommentChannel(
            commentChannelId,
            project.id,
            project.id,
            project.title,
            project.createdAt,
            null
        )

        val batch = Firebase.firestore.batch()

        batch.set(chatChannelRef, chatChannel)
        batch.set(commentChannelRef, commentChannel)

        batch.set(ref, project)

        val userChanges = mapOf<String, Any?>(
            "projectsCount" to FieldValue.increment(1),
            "projects" to FieldValue.arrayUnion(project.id),
            "chatChannels" to FieldValue.arrayUnion(project.chatChannel)
        )

        batch.update(Firebase.firestore.collection("users").document(currentUser.id), userChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    private suspend fun uploadImages(locationId: String, images: List<Uri>): List<Uri> {

        val listOfReferences = mutableListOf<StorageReference>()
        val listOfUploadTask = mutableListOf<UploadTask>()
        for (image in images) {
            val randomImageName = randomId()
            val ref = Firebase.storage.reference.child("$locationId/$randomImageName")
            listOfReferences.add(ref)

            val task = ref.putFile(image)
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
        val ref = Firebase.storage.reference.child("images/$locationId/$randomImageName")
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

    fun updateUser(userId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        updateDocument(ref, changes, onComplete)
    }

    suspend fun updateUser(userId: String, changes: Map<String, Any?>): Result<Map<String, Any?>> {
        val ref = Firebase.firestore.collection("users").document(userId)
        return try {
            val task = ref.update(changes)
            task.await()
            Result.Success(changes)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun checkIfUserNameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val query = Firebase.firestore.collection("users")
            .whereEqualTo("username", username)

        getQuerySnapshot(query, onComplete)
    }

    fun getLatestUserData(onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            val ref = Firebase.firestore.collection("users").document(uid)
            getDocument(ref, onComplete)
        } else {
            Log.d(TAG, "No user id found")
        }
    }

    fun signOut() {
        Firebase.auth.signOut()
    }

    suspend fun likeProject(userId: String, project: Project): Result<Project> {
        return try {
            val projectRef = Firebase.firestore.collection("projects")
                .document(project.id)

            val userRef = Firebase.firestore.collection("users")
                .document(userId)

            val batch = Firebase.firestore.batch()

            batch.update(projectRef, "likes", FieldValue.increment(1))
            batch.update(userRef, "likedProjects", FieldValue.arrayUnion(project.id))

            val task = batch.commit()
            task.await()
            Result.Success(project)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun dislikeProject(userId: String, project: Project): Result<Project> {
        return try {
            val projectRef = Firebase.firestore.collection("projects")
                .document(project.id)

            val userRef = Firebase.firestore.collection("users")
                .document(userId)

            val batch = Firebase.firestore.batch()

            batch.update(projectRef, "likes", FieldValue.increment(-1))
            batch.update(userRef, "likedProjects", FieldValue.arrayRemove(project.id))

            val task = batch.commit()
            task.await()
            Result.Success(project)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun saveProject(userId: String, project: Project): Result<Project> {
        return try {
            val userRef = Firebase.firestore.collection("users")
                .document(userId)

            val batch = Firebase.firestore.batch()

            batch.update(userRef, "savedProjects", FieldValue.arrayUnion(project.id))

            val savedProjectRef = userRef.collection("savedProjects").document(project.id)

            batch.set(savedProjectRef, project)

            val task = batch.commit()
            task.await()
            Result.Success(project)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun unSaveProject(userId: String, project: Project): Result<Project> {
        return try {
            val userRef = Firebase.firestore.collection("users")
                .document(userId)

            val batch = Firebase.firestore.batch()

            batch.update(userRef, "savedProjects", FieldValue.arrayRemove(project.id))

            batch.delete(userRef.collection("savedProjects").document(project.id))

            val task = batch.commit()
            task.await()
            Result.Success(project)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun joinProject(currentUser: User, project: Project): Result<ProjectRequest> {
        return try {
            val ref = Firebase.firestore.collection("projectRequests")
                .document()

            val requestId = ref.id

            val projectRef = Firebase.firestore.collection("projects").document(project.id)

            val projectRequest = ProjectRequest(requestId, project.id, currentUser.id, project.creator.userId, project, currentUser, System.currentTimeMillis())

            val userChanges = mapOf(
                "projectRequests" to FieldValue.arrayUnion(requestId)
            )

            val projectChanges = mapOf(
                "requests" to FieldValue.arrayUnion(requestId)
            )

            updateUser(currentUser.id, userChanges)
            updateDocument(projectRef, projectChanges)
            uploadDocument(ref, projectRequest)

            Result.Success(projectRequest)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun undoJoinProject(userId: String, projectId: String, requestId: String): Result<Void> {
        return try {
            val ref = Firebase.firestore.collection("projectRequests").document(requestId)

            val projectRef = Firebase.firestore.collection("projects").document(projectId)

            val userChanges = mapOf(
                "projectRequests" to FieldValue.arrayRemove(requestId)
            )

            val projectChanges = mapOf(
                "requests" to FieldValue.arrayRemove(requestId)
            )

            updateDocument(projectRef, projectChanges)
            updateUser(userId, userChanges)

            val task = ref.delete()
            Result.Success(task.result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun acceptProjectRequest(project: Project, projectRequest: ProjectRequest): Result<Void> {
        return try {
            val projectRef = Firebase.firestore.collection("projects").document(projectRequest.projectId)
            val senderRef = Firebase.firestore.collection("users").document(projectRequest.senderId)
            val chatChannelRef = Firebase.firestore.collection("chatChannels").document(project.chatChannel)
            val requestRef = Firebase.firestore.collection("projectRequests").document(projectRequest.requestId)

            val projectChanges = mapOf(
                "requests" to FieldValue.arrayRemove(projectRequest.requestId),
                "contributors" to FieldValue.arrayUnion(projectRequest.senderId)
            )

            val chatChannelChanges = mapOf(
                "contributors" to FieldValue.arrayUnion(projectRequest.senderId),
                "contributorsCount" to FieldValue.increment(1)
            )

            val senderChanges = mapOf(
                "collaborations" to FieldValue.arrayUnion(projectRequest.projectId),
                "collaborationsCount" to FieldValue.increment(1),
                "projectRequests" to FieldValue.arrayRemove(projectRequest.projectId),
                "chatChannels" to FieldValue.arrayUnion(project.chatChannel)
            )

            val batch = Firebase.firestore.batch()

            batch.update(projectRef, projectChanges)
            batch.update(chatChannelRef, chatChannelChanges)
            batch.update(senderRef, senderChanges)
            batch.delete(requestRef)

            val task = batch.commit()

            Result.Success(task.result)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun rejectRequest(projectRequest: ProjectRequest): Result<Void> {
        return try {

            val projectRef = Firebase.firestore.collection("projects").document(projectRequest.projectId)
            val senderRef = Firebase.firestore.collection("users").document(projectRequest.senderId)
            val requestRef = Firebase.firestore.collection("projectRequests").document(projectRequest.requestId)

            val projectChanges = mapOf(
                "requests" to FieldValue.arrayRemove(projectRequest.requestId),
            )

            val senderChanges = mapOf(
                "projectRequests" to FieldValue.arrayRemove(projectRequest.projectId)
            )

            val batch = Firebase.firestore.batch()

            batch.update(projectRef, projectChanges)
            batch.update(senderRef, senderChanges)
            batch.delete(requestRef)

            val task = batch.commit()

            Result.Success(task.result)

        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun likeUser(currentUser: User, userId: String): Result<User> {
        return try {
            val db = Firebase.firestore
            val batch = db.batch()

            val ref1 = db.collection("users").document(userId)
            batch.update(ref1, mapOf("starsCount" to FieldValue.increment(1)))

            val ref2 = db.collection("users").document(currentUser.id)
            batch.update(ref2, mapOf("likedUsers" to FieldValue.arrayUnion(userId)))

            val existingList = currentUser.likedUsers.toMutableList()
            existingList.add(userId)
            currentUser.likedUsers = existingList

            val task = batch.commit()
            task.await()
            Result.Success(currentUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun dislikeUser(currentUser: User, userId: String): Result<User> {
        return try {
            val db = Firebase.firestore
            val batch = db.batch()

            val ref1 = db.collection("users").document(userId)
            batch.update(ref1, mapOf("starsCount" to FieldValue.increment(-1)))

            val ref2 = db.collection("users").document(currentUser.id)
            batch.update(ref2, mapOf("likedUsers" to FieldValue.arrayRemove(userId)))

            val existingList = currentUser.likedUsers.toMutableList()
            existingList.remove(userId)
            currentUser.likedUsers = existingList

            val task = batch.commit()
            task.await()
            Result.Success(currentUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getProjectContributors(project: Project, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val task = Firebase.firestore.collection("users")
            .whereArrayContains("collaborations", project.id)
            .limit(7)
            .get()

        task.addOnCompleteListener(onComplete)
    }


    suspend fun sendComment(comment: Comment, parentCommentChannelId: String? = null): Result<Comment> {
        // - create new comment
        // - create a new channel for that comment
        // - the project comment's channel needs to contain this new comment
        // - the project needs to update comments count
        // - if it is a reply then update the parent comment

        return try {
            val db = Firebase.firestore
            val batch = db.batch()
            val commentRef = db.collection("commentChannels")
                .document(comment.commentChannelId)
                .collection("comments")
                .document(comment.commentId)

            val newCommentChannel = CommentChannel(randomId(), comment.commentId, comment.projectId, comment.postTitle, System.currentTimeMillis(), null)
            val newCommentChannelRef = db.collection("commentChannels").document(newCommentChannel.commentChannelId)
            batch.set(newCommentChannelRef, newCommentChannel)
            comment.threadChannelId = newCommentChannel.commentChannelId
            batch.set(commentRef, comment)

            val parentCommentChannelRef = db.collection("commentChannels").document(comment.commentChannelId)
            val parentCommentChannelChanges = mapOf("lastComment" to comment)
            batch.update(parentCommentChannelRef, parentCommentChannelChanges)

            val projectRef = db.collection("projects").document(comment.projectId)
            val projectChanges = mapOf("comments" to FieldValue.increment(1))
            batch.update(projectRef, projectChanges)



            // update the parent comment replies count
            if (comment.commentLevel.toInt() != 0) {
                val parentRef = db.collection("commentChannels")
                    .document(parentCommentChannelId!!).collection("comments")
                    .document(comment.parentId)

                batch.update(parentRef, mapOf("repliesCount" to FieldValue.increment(1)))
            }

            val task = batch.commit()
            task.await()
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }


    }

    suspend fun dislikeComment(currentUserId: String, comment: Comment): Result<Comment> {
        return try {
            val db = Firebase.firestore

            val batch = db.batch()

            val commentRef = Firebase.firestore.collection("commentChannels")
                .document(comment.commentChannelId)
                .collection("comments")
                .document(comment.commentId)

            batch.update(commentRef, mapOf("likes" to FieldValue.increment(-1)))

            val currentUserRef = db.collection("users").document(currentUserId)

            batch.update(currentUserRef, mapOf("likedComments" to FieldValue.arrayRemove(comment.commentId)))

            val task = batch.commit()
            task.await()
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun likeComment(currentUserId: String, comment: Comment): Result<Comment> {
        return try {
            val db = Firebase.firestore

            val batch = db.batch()

            val commentRef = Firebase.firestore.collection("commentChannels")
                .document(comment.commentChannelId)
                .collection("comments")
                .document(comment.commentId)

            batch.update(commentRef, mapOf("likes" to FieldValue.increment(1)))

            val currentUserRef = db.collection("users").document(currentUserId)

            batch.update(currentUserRef, mapOf("likedComments" to FieldValue.arrayUnion(comment.commentId)))

            val task = batch.commit()
            task.await()
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getChannelUsers(chatChannels: List<String>): Result<List<User>> {

        return try {
            val listOfReferences = mutableListOf<Query>()
            val users = mutableListOf<User>()

            for (channel in chatChannels) {
                val ref = Firebase.firestore.collection("users")
                    .whereArrayContains("chatChannels", channel)
                listOfReferences.add(ref)
            }

            for (ref in listOfReferences) {
                val task = ref.get()
                val querySnapshot = task.await()
                val usersList = querySnapshot.toObjects(User::class.java)
                users.addAll(usersList)
            }

            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun sendTextMessage(currentUser: User, chatChannelId: String, content: String): Result<Message> {

        return try {
            val db = Firebase.firestore
            val batch = db.batch()

            val chatChannelRef = db.collection("chatChannels").document(chatChannelId)

            val ref = chatChannelRef.collection("messages").document()
            val messageId = ref.id

            val message = Message(messageId, chatChannelId, text, content, currentUser.id, null, System.currentTimeMillis(), UserMinimal(currentUser.id, currentUser.name, currentUser.photo, currentUser.username), false, isCurrentUserMessage = true)

            batch.set(ref, message)

            val chatChannelChanges = mapOf(
                "lastMessage" to message,
                "updatedAt" to message.createdAt
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
    suspend fun sendMessagesSimultaneously(chatChannelId: String, listOfMessages: List<Message>): Result<List<Message>> {
        val db = Firebase.firestore

        val lastMessage = listOfMessages.last()
        val isLastMessageTextMsg = lastMessage.type == text

        val chatChannelRef = db.collection("chatChannels").document(chatChannelId)

        val sample = listOfMessages.first()
        val updatedList = if (isLastMessageTextMsg && listOfMessages.size > 1) {
            val mediaMessages = listOfMessages.slice(0..listOfMessages.size - 2)
            if (sample.type == image) {
                val downloadedImages = uploadImages(sample.chatChannelId, mediaMessages.map { it.content.toUri() })
                val images = downloadedImages.map { it.toString() }
                mediaMessages.forEachIndexed { index, message ->
                    message.content = images[index]
                }
            } else {
                TODO("Not implemented yet")
            }
            mediaMessages
        } else {
            if (sample.type == image) {
                val downloadedImages = uploadImages(sample.chatChannelId, listOfMessages.map { it.content.toUri() })
                val images = downloadedImages.map { it.toString() }
                listOfMessages.forEachIndexed { index, message ->
                    message.content = images[index]
                }
            } else {
                TODO("Not implemented yet")
            }
            listOfMessages
        }.toMutableList()

        if (isLastMessageTextMsg) {
            updatedList.add(lastMessage)
        }

        return try {
            val batch = Firebase.firestore.batch()

            for (message in updatedList) {
                val ref = chatChannelRef.collection("messages").document()
                message.messageId = ref.id
                batch.set(ref, message)
            }

            val chatChannelChanges = mapOf("lastMessage" to updatedList.last())

            batch.update(chatChannelRef, chatChannelChanges)

            val task = batch.commit()
            task.await()

            Result.Success(updatedList)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    fun downloadMedia(destinationFile: File, message: Message, onComplete: (task: Task<FileDownloadTask.TaskSnapshot>) -> Unit) {
        val uri = Uri.parse(message.content)
        val fileRef = uri.lastPathSegment
        fileRef?.let {
            val objRef = Firebase.storage.reference.child(fileRef)
            objRef.getFile(destinationFile).addOnCompleteListener(onComplete)
        }

    }

}