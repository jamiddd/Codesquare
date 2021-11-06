package com.jamid.codesquare

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        val listOfNames = mutableListOf<String>()
        for (i in project.images.indices) {
            listOfNames.add(randomId())
        }

        val downloadUris = uploadItems("${project.id}/images", listOfNames, project.images.map { it.toUri() })

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

    private suspend fun uploadItems(locationPath: String, names: List<String>, items: List<Uri>): List<Uri> {
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

    fun updateUser2(currentUser: User, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        // updating user
        val currentUserRef = db.collection("users").document(currentUser.id)
        batch.update(currentUserRef, changes)

        // updating projects where the creator is current user
        for (project in currentUser.projects) {
            val ref = db.collection("projects").document(project)
            batch.update(ref, "creator", currentUser.minify())
        }

        batch.commit().addOnCompleteListener(onComplete)
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

            val batch = Firebase.firestore.batch()

            val currentUserRef = Firebase.firestore.collection("users").document(currentUser.id)

            val projectRequestRef = Firebase.firestore.collection("projectRequests").document()
            val requestId = projectRequestRef.id

            val projectRef = Firebase.firestore.collection("projects").document(project.id)

            val projectRequest = ProjectRequest(requestId, project.id, currentUser.id, project.creator.userId, project, currentUser, System.currentTimeMillis())

            val userChanges = mapOf("projectRequests" to FieldValue.arrayUnion(requestId))

            val projectChanges = mapOf("requests" to FieldValue.arrayUnion(requestId))

            // create new project request
            batch.set(projectRequestRef, projectRequest)

            // update the current user
            batch.update(currentUserRef, userChanges)

            // update the projects
            batch.update(projectRef, projectChanges)

            val task = batch.commit()

            // assuring that the tasks complete
            task.await()

            Result.Success(projectRequest)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun undoJoinProject(userId: String, projectId: String, requestId: String): Result<Void> {
        return try {

            val batch = Firebase.firestore.batch()

            val currentUserRef = Firebase.firestore.collection("users").document(userId)

            val projectRequestRef = Firebase.firestore.collection("projectRequests").document(requestId)

            val projectRef = Firebase.firestore.collection("projects").document(projectId)

            val userChanges = mapOf(
                "projectRequests" to FieldValue.arrayRemove(requestId)
            )

            val projectChanges = mapOf(
                "requests" to FieldValue.arrayRemove(requestId)
            )

            batch.delete(projectRequestRef)

            batch.update(projectRef, projectChanges)

            batch.update(currentUserRef, userChanges)

            val task = batch.commit()

            task.await()

            Result.Success(task.result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun acceptProjectRequest(project: Project, projectRequest: ProjectRequest, onComplete: (task: Task<Void>) -> Unit) {
        val batch = Firebase.firestore.batch()

        val projectRef = Firebase.firestore.collection("projects").document(projectRequest.projectId)
        val senderRef = Firebase.firestore.collection("users").document(projectRequest.senderId)
        val chatChannelRef = Firebase.firestore.collection("chatChannels").document(project.chatChannel)
        val projectRequestRef = Firebase.firestore.collection("projectRequests").document(projectRequest.requestId)

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

        batch.update(projectRef, projectChanges)
        batch.update(chatChannelRef, chatChannelChanges)
        batch.update(senderRef, senderChanges)
        batch.delete(projectRequestRef)

        batch.commit().addOnCompleteListener(onComplete)

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

    fun getProjectContributors(limit: Long = 0, project: Project, onComplete: (task: Task<QuerySnapshot>) -> Unit) {

        val task = if (limit.toInt() == 0) {
            Firebase.firestore.collection("users")
                .whereArrayContains("collaborations", project.id)
                .get()
        } else {
            Firebase.firestore.collection("users")
                .whereArrayContains("collaborations", project.id)
                .limit(limit)
                .get()
        }

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

    fun getChannelUsers(channel: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val db = Firebase.firestore
        val ref = db.collection("users")
            .whereArrayContains("chatChannels", channel)

        ref.get().addOnCompleteListener(onComplete)
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

            val message = Message(messageId, chatChannelId, text, content, currentUser.id, null, emptyList(), emptyList(), System.currentTimeMillis(), currentUser, false, isCurrentUserMessage = true)

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
            val downloadedContents = if (sample.type == image) {
                uploadItems("$chatChannelId/images", mediaMessages.map { it.content }, mediaMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems("$chatChannelId/documents", mediaMessages.map { it.content }, mediaMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            }

            mediaMessages.mapIndexed { index, message ->
                message.metadata?.url = downloadedContents[index]
                message
            }

        } else {
            val downloadedContents = if (sample.type == image) {
                uploadItems("$chatChannelId/images", listOfMessages.map { it.content }, listOfMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems("$chatChannelId/documents", listOfMessages.map { it.content }, listOfMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
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
                val ref = chatChannelRef.collection("messages").document()
                message.messageId = ref.id
                batch.set(ref, message)
            }

            val chatChannelChanges = mapOf("lastMessage" to updatedList.last(), "updatedAt" to System.currentTimeMillis())

            batch.update(chatChannelRef, chatChannelChanges)

            val task = batch.commit()
            task.await()

            Result.Success(updatedList)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    // name must be with extension
    fun downloadMedia(destinationFile: File, name: String, message: Message, onComplete: (task: Task<FileDownloadTask.TaskSnapshot>) -> Unit) {
        val path = if (message.type == image) {
            "${message.chatChannelId}/images/$name"
        } else {
            "${message.chatChannelId}/documents/$name"
        }
        val objRef = Firebase.storage.reference.child(path)
        objRef.getFile(destinationFile).addOnCompleteListener(onComplete)
    }

    fun deleteProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {

        val batch = Firebase.firestore.batch()

        batch.delete(Firebase.firestore.collection("projects")
            .document(project.id))

        val creatorId = project.creator.userId

        batch.update(Firebase.firestore.collection("users")
            .document(creatorId), "projects", FieldValue.arrayRemove(project.id))

        for (id in project.contributors) {
            val ref = Firebase.firestore.collection("users").document(id)
            batch.update(ref, "contributors", FieldValue.arrayRemove(project.id))
        }

        batch.delete(Firebase.firestore.collection("chatChannels").document(project.chatChannel))

        batch.delete(Firebase.firestore.collection("commentChannels").document(project.commentChannel))

        batch.commit()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    // loop through all the requests
                    Firebase.firestore.collection("projectRequests")
                        .whereEqualTo("projectId", project.id)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot != null && !querySnapshot.isEmpty) {

                                val newBatch = Firebase.firestore.batch()
                                val requests = querySnapshot.toObjects(ProjectRequest::class.java)

                                for (request in requests) {
                                    val ref = Firebase.firestore.collection("users").document(request.senderId)
                                    newBatch.update(ref, "projectRequests", FieldValue.arrayRemove(project.id))

                                    val ref2 = Firebase.firestore.collection("projectRequests").document(request.requestId)
                                    newBatch.delete(ref2)
                                }

                                newBatch.commit().addOnCompleteListener(onComplete)
                            }
                        }

                } else {
                    Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                }
            }


    }

    fun getOtherUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        ref.get().addOnCompleteListener(onComplete)
    }

    fun deleteUser(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        ref.delete().addOnCompleteListener(onComplete)
    }

    fun deleteComment(commentChannelId: String, commentId: String, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection("commentChannels")
            .document(commentChannelId)
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnCompleteListener(onComplete)
    }

    /*suspend fun sendMediaForwards(uri: Uri, currentUser: User, message: Message, channels: List<ChatChannel>): Exception? {
        return try {

            for (channel in channels) {
                val newMessage = Message(randomId(), channel.chatChannelId, message.type, randomId(), currentUser.id, message.metadata, emptyList(), emptyList(), System.currentTimeMillis(), currentUser, false, true)
                when (val result = sendMessagesSimultaneously(channel.chatChannelId, listOf(newMessage))) {
                    is Result.Error -> throw result.exception
                    is Result.Success -> {}
                }
            }
            null
        } catch (e: Exception) {
            e
        }

    }*/


    suspend fun sendMultipleMessageToMultipleChannels(currentUser: User, messages: List<Message>, channels: List<ChatChannel>): Result<List<Message>> {
        return try {

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
                    uploadItems("${it.chatChannelId}/images", imagesMessages.map { it1 -> it1.content }, imageUris)
                } else {
                    emptyList()
                }

                val downloadedDocumentUris = if (documentMessages.isNotEmpty()) {
                    uploadItems("${it.chatChannelId}/documents", documentMessages.map { it1 -> it1.content }, documentUris)
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

                    val ref = db.collection("chatChannels")
                        .document(it.chatChannelId)
                        .collection("messages")
                        .document()

                    val newMessage = Message(ref.id, it.chatChannelId, messages[i].type, messages[i].content, currentUser.id, messages[i].metadata, emptyList(), emptyList(), now, currentUser,
                        isDownloaded = false,
                        isCurrentUserMessage = true
                    )

                    newMessages.add(newMessage)

                    Log.d(TAG, newMessages.map {it1 -> it1.content }.toString())

                    batch.set(ref, newMessage)

                    if (i == messages.size - 1) {
                        batch.update(db.collection("chatChannels").document(it.chatChannelId), mapOf("lastMessage" to newMessage, "updatedAt" to now))
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

    suspend fun sendSingleMediaMessageToMultipleChannels(
        uri: Uri,
        currentUser: User,
        message: Message,
        channels: List<ChatChannel>
    ): Result<List<Message>> {

        val db = Firebase.firestore

        val now = System.currentTimeMillis()

        return try {

            val batch = db.batch()

            val messages = mutableListOf<Message>()

            for (channel in channels) {
                val newMessage = Message(randomId(), channel.chatChannelId, message.type, randomId(), currentUser.id, message.metadata, emptyList(), emptyList(), now, currentUser,
                    isDownloaded = false,
                    isCurrentUserMessage = true
                )

                val chatChannelRef = db.collection("chatChannels").document(channel.chatChannelId)

                val downloadedContents = if (newMessage.type == image) {
                    uploadItems("${channel.chatChannelId}/images", listOf(newMessage.content), listOf(uri)).map { it.toString() }
                } else {
                    uploadItems("${channel.chatChannelId}/documents", listOf(newMessage.content), listOf(uri)).map { it.toString() }
                }

                newMessage.metadata?.url = downloadedContents.first()

                val ref = chatChannelRef.collection("messages").document()
                newMessage.messageId = ref.id

                messages.add(newMessage)

                batch.set(ref, newMessage)
                val changes = mapOf(
                    "lastMessage" to newMessage,
                    "updatedAt" to now
                )

                batch.update(chatChannelRef, changes)
            }

            batch.commit().await()
            Result.Success(messages)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun updateDeliveryListOfMessages(chatChannel: ChatChannel, currentUserId: String, messages: List<Message>, onComplete: (task: Task<Void>) -> Unit){
        val batch = Firebase.firestore.batch()

        for (message in messages) {
            if (message.senderId != currentUserId) {
                val messageRef = Firebase.firestore.collection("chatChannels")
                    .document(message.chatChannelId)
                    .collection("messages")
                    .document(message.messageId)

                val changes = mapOf("deliveryList" to FieldValue.arrayUnion(currentUserId))
                val newList = message.deliveryList.addItemToList(currentUserId)
                message.deliveryList = newList

                batch.update(messageRef, changes)

                if (chatChannel.lastMessage?.messageId == message.messageId) {
                    batch.update(Firebase.firestore.collection("chatChannels")
                        .document(chatChannel.chatChannelId),
                        mapOf(
                            "lastMessage" to message,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getAllChatChannels(userId: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        Firebase.firestore.collection("chatChannels")
            .whereArrayContains("contributors", userId)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(onComplete)
    }

    fun updateReadList(chatChannel: ChatChannel, currentUser: User, message: Message, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()
        val ref = db.collection("chatChannels")
            .document(message.chatChannelId)
            .collection("messages")
            .document(message.messageId)

        val newList = message.readList.addItemToList(currentUser.id)
        message.readList = newList

        if (chatChannel.lastMessage?.messageId == message.messageId) {
            batch.update(Firebase.firestore.collection("chatChannels")
                .document(chatChannel.chatChannelId),
                    mapOf(
                        "lastMessage" to message,
                        "updatedAt" to System.currentTimeMillis()
                    )
            )
        }

        batch.update(ref, mapOf("readList" to FieldValue.arrayUnion(currentUser.id)))

        batch.commit().addOnCompleteListener(onComplete)
    }

}