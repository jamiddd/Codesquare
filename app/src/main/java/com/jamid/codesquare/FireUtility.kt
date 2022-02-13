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

    /*suspend fun uploadDocument(ref: DocumentReference, data: Any): Result<Any> {
        return try {
            val task = ref.set(data)
            task.await()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/


    fun updateDocument(
        ref: DocumentReference,
        data: Map<String, Any?>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        ref.update(data)
            .addOnCompleteListener(onComplete)
    }

    /*suspend fun updateDocument(ref: DocumentReference, data: Map<String, Any?>): Result<Map<String, Any?>> {
        return try {
            val task = ref.update(data)
            task.await()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

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
            Firebase.firestore.collection("chatChannels").document(project.chatChannel)

        val tokens = mutableListOf<String>()

        tokens.addAll(currentUser.registrationTokens)

        val chatChannel = ChatChannel(
            project.chatChannel,
            project.id,
            project.name,
            project.images.first(),
            project.contributors.size.toLong(),
            listOf(project.creator.userId),
            listOf(project.creator.userId),
            project.createdAt,
            project.updatedAt,
            null,
            tokens
        )

        val commentChannelRef = Firebase.firestore.collection("commentChannels").document()
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
            "projectsCount" to FieldValue.increment(1),
            "projects" to FieldValue.arrayUnion(project.id),
            "chatChannels" to FieldValue.arrayUnion(project.chatChannel)
        )

        batch.update(Firebase.firestore.collection("users").document(currentUser.id), userChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    suspend fun uploadItems(
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

    /*fun updateUser(userId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        updateDocument(ref, changes, onComplete)
    }*/

    fun updateUser2(
        changes: Map<String, Any?>,
        shouldUpdateProjects: Boolean = true,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val currentUser = UserManager.currentUser
        val batch = db.batch()

        // updating user
        val currentUserRef = db.collection(USERS).document(currentUser.id)
        batch.update(currentUserRef, changes)

        // updating projects where the creator is current user
        if (shouldUpdateProjects) {
            for (project in currentUser.projects) {
                val ref = db.collection(PROJECTS).document(project)
                val miniUser = currentUser.minify()
                Log.d(TAG, miniUser.toString())
                batch.update(ref, "creator", miniUser)
            }
        }

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    /*suspend fun updateUser(userId: String, changes: Map<String, Any?>): Result<Map<String, Any?>> {
        val ref = Firebase.firestore.collection("users").document(userId)
        return try {
            val task = ref.update(changes)
            task.await()
            Result.Success(changes)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun checkIfUserNameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val query = Firebase.firestore.collection("users")
            .whereEqualTo("username", username)

        getQuerySnapshot(query, onComplete)
    }

    /*fun getLatestUserData(onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
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
    }*/

    fun likeProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val projectRef = Firebase.firestore.collection(PROJECTS)
            .document(project.id)

        val batch = Firebase.firestore.batch()
        val userRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        batch.update(projectRef, "likes", FieldValue.increment(1))
        batch.update(userRef, "likedProjects", FieldValue.arrayUnion(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun likeProject(currentUser: User, project: Project, notification: Notification): Result<Project> {
        return try {
            val projectRef = Firebase.firestore.collection("projects")
                .document(project.id)

            val userRef = Firebase.firestore.collection("users")
                .document(currentUser.id)

            val notificationRef = Firebase.firestore.collection("users")
                .document(project.creator.userId)
                .collection("notifications")
                .document(notification.id)

            val batch = Firebase.firestore.batch()

            if (notification.senderId != notification.receiverId) {
                Firebase.firestore.collection("users").document(currentUser.id)
                    .collection("notifications")
                    .whereEqualTo("content", notification.content)
                    .get()
                    .addOnSuccessListener {
                        if (it.isEmpty) {
                            notificationRef.set(notification)
                        }
                    }
//                batch.set(notificationRef, notification)
            }

            batch.update(projectRef, "likes", FieldValue.increment(1))
            batch.update(userRef, "likedProjects", FieldValue.arrayUnion(project.id))

            val task = batch.commit()
            task.await()
            Result.Success(project)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }*/

    fun dislikeProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val projectRef = Firebase.firestore.collection(PROJECTS)
            .document(project.id)

        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        val batch = Firebase.firestore.batch()

        batch.update(projectRef, "likes", FieldValue.increment(-1))
        batch.update(currentUserRef, "likedProjects", FieldValue.arrayRemove(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun dislikeProject(userId: String, project: Project): Result<Project> {
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
    }*/

    fun saveProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef =
            Firebase.firestore.collection(USERS).document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()
        batch.update(currentUserRef, "savedProjects", FieldValue.arrayUnion(project.id))
        val savedProjectRef = currentUserRef.collection("savedProjects").document(project.id)

        batch.set(savedProjectRef, project)
        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun saveProject(userId: String, project: Project): Result<Project> {
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
    }*/

    fun unSaveProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef = Firebase.firestore.collection("users")
            .document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()

        batch.update(currentUserRef, "savedProjects", FieldValue.arrayRemove(project.id))

        batch.delete(currentUserRef.collection("savedProjects").document(project.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun unSaveProject(userId: String, project: Project): Result<Project> {
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
    }*/

    /*fun inviteUserToProjectAlt(project: Project, userId: String, notification: Notification) {
        val db = Firebase.firestore
        db.runTransaction {
            val projectInviteRef = Firebase.firestore.collection("users")
                .document(userId).collection("invites")
                .document()

            val projectInvite = ProjectInvite(projectInviteRef.id, project.id, userId, project.creator.userId, System.currentTimeMillis())

            val notificationRef = Firebase.firestore
                .collection("users")
                .document(userId)
                .collection("notifications")
                .document(notification.id)


            val prevNotificationRef = Firebase.firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("content", notification.content)



        }
    }*/

    /*suspend fun checkIfNotificationAlreadyExists(notification: Notification, userId: String): Result<Pair<QuerySnapshot, Boolean>> {
        return try {
            val task = Firebase.firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("content", notification.content)
                .get()

            val result = task.await()
            Result.Success((result to result.isEmpty))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

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
            project,
            currentUser,
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
            .update(currentUserRef, userChanges)
            .delete(projectRequestRef)
            .delete(requestNotificationRef)
            .commit()
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

        batch.addNewUserToProject(project.id, project.chatChannel, projectRequest.senderId)
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

    /*suspend fun likeUser(userId: String, notification: Notification): Result<User> {
        return try {
            val currentUser = UserManager.currentUser
            val db = Firebase.firestore
            val batch = db.batch()

            val ref1 = db.collection("users").document(userId)
            batch.update(ref1, mapOf("likesCount" to FieldValue.increment(1)))

            val ref2 = db.collection("users").document(currentUser.id)
            batch.update(ref2, mapOf("likedUsers" to FieldValue.arrayUnion(userId)))

            batch.set(Firebase.firestore
                .collection("users")
                .document(notification.receiverId)
                .collection("notifications")
                .document(notification.id), notification)

            val existingList = currentUser.likedUsers.toMutableList()
            existingList.add(userId)
            currentUser.likedUsers = existingList

            val task = batch.commit()
            task.await()
            Result.Success(currentUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun dislikeUser(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        val ref1 = db.collection("users").document(userId)
        batch.update(ref1, mapOf("likesCount" to FieldValue.increment(-1)))

        val ref2 = db.collection("users").document(currentUser.id)
        batch.update(ref2, mapOf("likedUsers" to FieldValue.arrayRemove(userId)))

        val existingList = currentUser.likedUsers.toMutableList()
        existingList.remove(userId)
        currentUser.likedUsers = existingList

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun dislikeUser(userId: String, notification: Notification?): Result<User> {
        return try {
            val currentUser = UserManager.currentUser
            val db = Firebase.firestore
            val batch = db.batch()

            val ref1 = db.collection("users").document(userId)
            batch.update(ref1, mapOf("likesCount" to FieldValue.increment(-1)))

            val ref2 = db.collection("users").document(currentUser.id)
            batch.update(ref2, mapOf("likedUsers" to FieldValue.arrayRemove(userId)))

            if (notification != null) {
                batch.delete(Firebase.firestore
                    .collection("users")
                    .document(notification.receiverId)
                    .collection("notifications")
                    .document(notification.id))
            }

            val existingList = currentUser.likedUsers.toMutableList()
            existingList.remove(userId)
            currentUser.likedUsers = existingList

            val task = batch.commit()
            task.await()
            Result.Success(currentUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun getProjectContributors(
        project: Project,
        limit: Long = 0,
        onComplete: (task: Task<QuerySnapshot>) -> Unit
    ) {
        val task = if (limit.toInt() == 0) {
            Firebase.firestore.collection(USERS)
                .whereArrayContains(COLLABORATIONS, project.id)
                .get()
        } else {
            Firebase.firestore.collection(USERS)
                .whereArrayContains(COLLABORATIONS, project.id)
                .limit(limit)
                .get()
        }

        task.addOnCompleteListener(onComplete)
    }

    fun sendComment(
        comment: Comment,
        parentCommentChannelId: String?,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()
        val commentRef = db.collection("commentChannels")
            .document(comment.commentChannelId)
            .collection("comments")
            .document(comment.commentId)

        val newCommentChannel = CommentChannel(
            randomId(),
            comment.commentId,
            comment.projectId,
            comment.postTitle,
            System.currentTimeMillis(),
            null
        )
        val newCommentChannelRef =
            db.collection("commentChannels").document(newCommentChannel.commentChannelId)
        batch.set(newCommentChannelRef, newCommentChannel)
        comment.threadChannelId = newCommentChannel.commentChannelId
        batch.set(commentRef, comment)

        val parentCommentChannelRef =
            db.collection("commentChannels").document(comment.commentChannelId)
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

        batch.commit().addOnCompleteListener(onComplete)

    }

    /*suspend fun sendComment(comment: Comment, parentCommentChannelId: String? = null): Result<Comment> {
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


    }*/

    fun dislikeComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore

        val batch = db.batch()

        val commentRef = Firebase.firestore.collection("commentChannels")
            .document(comment.commentChannelId)
            .collection("comments")
            .document(comment.commentId)

        batch.update(
            commentRef,
            mapOf(
                "likesCount" to FieldValue.increment(-1),
                "likes" to FieldValue.arrayRemove(UserManager.currentUserId)
            )
        )

        val currentUserRef = db.collection("users").document(UserManager.currentUserId)

        batch.update(
            currentUserRef,
            mapOf("likedComments" to FieldValue.arrayRemove(comment.commentId))
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun dislikeComment(currentUserId: String, comment: Comment): Result<Comment> {
        return try {
            val db = Firebase.firestore

            val batch = db.batch()

            val commentRef = Firebase.firestore.collection("commentChannels")
                .document(comment.commentChannelId)
                .collection("comments")
                .document(comment.commentId)

            batch.update(
                commentRef,
                mapOf(
                    "likesCount" to FieldValue.increment(-1),
                    "likes" to FieldValue.arrayRemove(currentUserId)
                )
            )

            val currentUserRef = db.collection("users").document(currentUserId)

            batch.update(
                currentUserRef,
                mapOf("likedComments" to FieldValue.arrayRemove(comment.commentId))
            )

            val task = batch.commit()
            task.await()
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun likeComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val currentUserId = UserManager.currentUserId
        val batch = db.batch()

        val commentRef = Firebase.firestore.collection("commentChannels")
            .document(comment.commentChannelId)
            .collection("comments")
            .document(comment.commentId)

        batch.update(
            commentRef,
            mapOf(
                "likesCount" to FieldValue.increment(1),
                "likes" to FieldValue.arrayUnion(currentUserId)
            )
        )

        val currentUserRef = db.collection("users").document(currentUserId)

        batch.update(
            currentUserRef,
            mapOf("likedComments" to FieldValue.arrayUnion(comment.commentId))
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*suspend fun likeComment(
        currentUserId: String,
        comment: Comment,
        notification: Notification
    ): Result<Comment> {
        return try {
            val db = Firebase.firestore

            val batch = db.batch()

            val commentRef = Firebase.firestore.collection("commentChannels")
                .document(comment.commentChannelId)
                .collection("comments")
                .document(comment.commentId)

            batch.update(
                commentRef,
                mapOf(
                    "likesCount" to FieldValue.increment(1),
                    "likes" to FieldValue.arrayUnion(currentUserId)
                )
            )

            if (notification.senderId != notification.receiverId) {
                batch.set(
                    Firebase.firestore.collection("users")
                        .document(notification.receiverId)
                        .collection("notifications")
                        .document(notification.id), notification
                )
            }

            val currentUserRef = db.collection("users").document(currentUserId)

            batch.update(
                currentUserRef,
                mapOf("likedComments" to FieldValue.arrayUnion(comment.commentId))
            )

            val task = batch.commit()
            task.await()
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    /*fun getChannelUsers(channel: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val db = Firebase.firestore
        val ref = db.collection("users")
            .whereArrayContains("chatChannels", channel)

        ref.get().addOnCompleteListener(onComplete)
    }*/

    /*suspend fun getChannelUsers(chatChannels: List<String>): Result<List<User>> {
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

    }*/

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

            val chatChannelRef = db.collection("chatChannels").document(chatChannelId)

            val ref = chatChannelRef.collection("messages").document()
            val messageId = ref.id

            val message = Message(
                messageId,
                chatChannelId,
                text,
                content,
                currentUser.id,
                null,
                emptyList(),
                emptyList(),
                System.currentTimeMillis(),
                replyTo,
                replyMessage,
                currentUser,
                false,
                isCurrentUserMessage = true,
                -1
            )

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
    suspend fun sendMessagesSimultaneously(
        chatChannelId: String,
        listOfMessages: List<Message>
    ): Result<List<Message>> {
        val db = Firebase.firestore

        val lastMessage = listOfMessages.last()
        val isLastMessageTextMsg = lastMessage.type == text

        val chatChannelRef = db.collection("chatChannels").document(chatChannelId)

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
                val ref = chatChannelRef.collection("messages").document()
                message.messageId = ref.id
                batch.set(ref, message)
            }

            val chatChannelChanges = mapOf(
                "lastMessage" to updatedList.last(),
                "updatedAt" to System.currentTimeMillis()
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

    /*fun deleteProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {

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


    }*/

    suspend fun getOtherUser(userId: String): Result<User>? {
        return try {
            val ref = Firebase.firestore.collection("users").document(userId)
            val task = ref.get()
            val result = task.await()
            if (result.exists()) {
                val user = result.toObject(User::class.java)!!
                Result.Success(user)
            } else {
                null
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /*fun getOtherUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        ref.get().addOnCompleteListener(onComplete)
    }

    fun deleteUser(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        ref.delete().addOnCompleteListener(onComplete)
    }*/

    fun deleteComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        val commentRef = Firebase.firestore
            .collection("commentChannels")
            .document(comment.commentChannelId)
            .collection("comments")
            .document(comment.commentId)
        commentRef.delete().addOnCompleteListener(onComplete)
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

                    val ref = db.collection("chatChannels")
                        .document(it.chatChannelId)
                        .collection("messages")
                        .document()

                    val newMessage = Message(
                        ref.id,
                        it.chatChannelId,
                        messages[i].type,
                        messages[i].content,
                        currentUser.id,
                        messages[i].metadata,
                        emptyList(),
                        emptyList(),
                        now,
                        null,
                        null,
                        currentUser,
                        isDownloaded = false,
                        isCurrentUserMessage = true
                    )

                    newMessages.add(newMessage)

                    Log.d(TAG, newMessages.map { it1 -> it1.content }.toString())

                    batch.set(ref, newMessage)

                    if (i == messages.size - 1) {
                        batch.update(
                            db.collection("chatChannels").document(it.chatChannelId),
                            mapOf("lastMessage" to newMessage, "updatedAt" to now)
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

    /*suspend fun sendSingleMediaMessageToMultipleChannels(
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
                val newMessage = Message(randomId(), channel.chatChannelId, message.type, randomId(), currentUser.id, message.metadata, emptyList(), emptyList(), now, null, null, currentUser,
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
    }*/

    fun updateDeliveryListOfMessages(
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
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
            }
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*fun getAllChatChannels(userId: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        Firebase.firestore.collection("chatChannels")
            .whereArrayContains("contributors", userId)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(onComplete)
    }*/

    fun updateReadList(
        chatChannel: ChatChannel,
        message: Message,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore
        val batch = db.batch()
        val ref = db.collection("chatChannels")
            .document(message.chatChannelId)
            .collection("messages")
            .document(message.messageId)

        val newList = message.readList.addItemToList(currentUserId)
        message.readList = newList

        if (chatChannel.lastMessage!!.messageId == message.messageId) {
            batch.update(
                Firebase.firestore.collection("chatChannels")
                    .document(chatChannel.chatChannelId),
                mapOf(
                    "lastMessage.readList" to FieldValue.arrayUnion(currentUserId)
                )
            )
        }

        batch.update(ref, mapOf("readList" to FieldValue.arrayUnion(currentUserId)))

        batch.commit().addOnCompleteListener(onComplete)
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    /*
    * Messages related functions
    * */

    /* fun getMessageRef(chatChannelId: String, messageId: String = randomId()): DocumentReference {
         return Firebase.firestore.collection("chatChannels").document(chatChannelId)
             .collection("messages")
             .document(messageId)
     }*/

    /*private fun getMessagesRef(chatChannelId: String): CollectionReference {
        return Firebase.firestore.collection("chatChannels").document(chatChannelId).collection("messages")
    }
*/


    /* fun getMessageDocumentSnapshot(chatChannelId: String, messageId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
         val ref = getMessageRef(chatChannelId, messageId)
         getDocument(ref, onComplete)
     }*/

    /* fun getMessagesQuerySnapshot(chatChannelId: String, lastSnapshot: DocumentSnapshot? = null, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
         val query = if (lastSnapshot == null) {
             getMessagesRef(chatChannelId)
                 .orderBy(CREATED_AT, Query.Direction.DESCENDING)
         } else {
             getMessagesRef(chatChannelId)
                 .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                 .startAfter(lastSnapshot)
         }

         query.get()
             .addOnCompleteListener(onComplete)

     }
 */
    /*fun callFunction(data: MutableMap<String, Any>, onComplete: ((task: Task<HttpsCallableResult>) -> Unit)? = null) {
        Firebase.functions.getHttpsCallable("onProjectLiked")
            .call(data)
            .addOnCompleteListener {
                if (onComplete != null) {
                    onComplete(it)
                }
                if (it.isSuccessful) {
                    Log.d(TAG, "Project like notification sent")
                } else {
                    Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                }
            }

    }*/

    fun sendRegistrationTokenToServer(token: String) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.uid)
                .update("registrationTokens", FieldValue.arrayUnion(token))
                .addOnCompleteListener {
                    /*if (it.isSuccessful) {

                    } else {

                    }*/
                }

        }
    }

    fun sendReport(report: Report, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection("reports")
            .document(report.id)
            .set(report)
            .addOnCompleteListener(onComplete)
    }

    fun sendFeedback(feedback: Feedback, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection("feedbacks")
            .document(feedback.id)
            .set(feedback)
            .addOnCompleteListener(onComplete)
    }

    /*suspend fun getNotifications(currentUserId: String, lastSnapshot: DocumentSnapshot? = null): Result<QuerySnapshot> {
        return try {
            val task = if (lastSnapshot != null) {
                Firebase.firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .startAfter(lastSnapshot)
                    .limit(50)
                    .get()
            } else {
                Firebase.firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
            }
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    // the changes to the chat channel will be reflected in the local database because there is listener
    // attached to the channels
    fun setOtherUserAsAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection("chatChannels").document(chatChannelId)
            .update("administrators", FieldValue.arrayUnion(userId))
            .addOnCompleteListener(onComplete)
    }

    // the changes to the chat channel will be reflected in the local database because there is listener
    // attached to the channels
    fun removeUserFromAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection("chatChannels").document(chatChannelId)
            .update("administrators", FieldValue.arrayRemove(userId))
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
            CHANNELS to FieldValue.arrayRemove(chatChannelId)
        )

        val chatChannelRef = Firebase.firestore
            .collection(CHAT_CHANNELS)
            .document(chatChannelId)

        val channelChanges = mapOf(
            CONTRIBUTORS to FieldValue.arrayRemove(user.id),
            ADMINISTRATORS to FieldValue.arrayRemove(user.id),
            CONTRIBUTORS_COUNT to FieldValue.increment(-1)
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

    // Leaving the group
    //
    // 1. remove project id from user document collaborations -------------X
    // 2. remove chat channel from user document channels list -------------X
    // 3. remove user id from chatChannel contributors and if admin, from administrators
    // 4. add user id to blocked list
    /*suspend fun removeUserFromProject(
        user: User,
        projectId: String,
        chatChannelId: String
    ): Result<String> {
        val contributorRef = Firebase.firestore
            .collection(USERS)
            .document(user.id)

        val batch = Firebase.firestore.batch()

        return try {

            val userChanges = mapOf(
                COLLABORATIONS to FieldValue.arrayRemove(projectId),
                CHANNELS to FieldValue.arrayRemove(chatChannelId)
            )

            val chatChannelRef = Firebase.firestore
                .collection(CHAT_CHANNELS)
                .document(chatChannelId)

            val channelChanges = mapOf(
                CONTRIBUTORS to FieldValue.arrayRemove(user.id),
                ADMINISTRATORS to FieldValue.arrayRemove(user.id),
                CONTRIBUTORS_COUNT to FieldValue.increment(-1)
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

            val task = batch.commit()

            task.await()

            Result.Success(projectId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun castVoteRemoveUser(
        user: User,
        projectId: String,
        currentUserId: String,
        onComplete: (task: Task<Transaction>) -> Unit
    ) {

        val decision = ProjectDecision(user.id, user, "user", emptyList(), projectId)

        Firebase.firestore.runTransaction {
            val ref = Firebase.firestore.collection("projects").document(projectId)
                .collection("toBeRemoved")
                .document(user.id)

            val dec = it.get(ref)

            if (dec.exists()) {
                it.update(ref, "votersList", FieldValue.arrayUnion(currentUserId))
            } else {
                it.set(ref, decision)
            }
        }.addOnCompleteListener(onComplete)
    }

    fun undoVoteCast(
        user: User,
        projectId: String,
        currentUserId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val ref = Firebase.firestore.collection("projects").document(projectId)
            .collection("toBeRemoved")
            .document(user.id)

        ref.update("votersList", FieldValue.arrayRemove(currentUserId))
            .addOnCompleteListener(onComplete)
    }

    /*fun getAllMessagesByUser(
        chatChannelId: String,
        user: User,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        Firebase.firestore.collection("chatChannels").document(chatChannelId)
            .collection("messages")
            .whereEqualTo("senderId", user.id)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val batch = Firebase.firestore.batch()
                    val qs = it.result
                    for (snapshot in qs) {
                        batch.delete(snapshot.reference)
                    }
                    batch.commit()
                        .addOnCompleteListener(onComplete)
                } else {
                    Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                }
            }
    }*/

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
            project.creator.userId,
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
    fun getExistingProjectRequest(
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

        val projectRef = db.collection(PROJECTS).document(project.id)
        val archivedProjectRef =
            db.collection(USERS).document(currentUserId).collection(ARCHIVE).document(project.id)
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
        val currentUserRegistrationTokens = currentUser.registrationTokens

        val batch = db.batch()
        if (project != null) {
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
                currentUserRegistrationTokens
            )
                .updateCurrentUser(currentUserId, currentUserChanges)
                .updateParticularDocument(
                    projectInviteSenderReference,
                    projectInviteSenderDocumentChanges
                )
                .deleteParticularDocument(projectInviteReference)
                .deleteParticularDocument(inviteNotificationRef)
        }

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    private fun WriteBatch.addNewUserToProject(
        projectId: String,
        chatChannelId: String,
        userId: String,
        userRegistrationTokens: List<String> = emptyList()
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

        if (userRegistrationTokens.isNotEmpty()) {
            changes2[REGISTRATION_TOKENS] = FieldValue.arrayUnion(userRegistrationTokens.first())
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
        Firebase.firestore.collection("interests").document("interestsCollection")
            .get()
            .addOnCompleteListener(onComplete)
    }

    fun uploadUser(user: User, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection("users").document(user.id)
            .set(user)
            .addOnCompleteListener(onComplete)
    }

    fun sendNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS).document(notification.receiverId)
            .collection(NOTIFICATIONS).document(notification.id)
            .set(notification)
            .addOnCompleteListener(onComplete)
    }

    fun listenForNotifications(onReceive: (notifications: List<Notification>) -> Unit) {
        val currentUser = UserManager.currentUser
        Firebase.firestore.collection(USERS).document(currentUser.id)
            .collection(NOTIFICATIONS)
            .limit(30)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                }

                if (value != null && !value.isEmpty) {
                    val notifications = value.toObjects(Notification::class.java)
                    onReceive(notifications)
                }

            }
    }

    fun checkIfNotificationExistsByContent(
        oldNotification: Notification,
        onComplete: (exists: Boolean, error: Exception?) -> Unit
    ) {
        Firebase.firestore.collection(USERS).document(oldNotification.receiverId)
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
            .whereEqualTo("creator.userId", currentUser.id)
            .whereEqualTo("expiredAt", -1)
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

    fun updateNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .document(notification.receiverId)
            .collection(NOTIFICATIONS)
            .document(notification.id)
            .update(mapOf("read" to true))
            .addOnCompleteListener(onComplete)
    }

    fun removeUserFromChatChannel(
        user: User,
        chatChannel: ChatChannel,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val administrators = chatChannel.administrators
        val channelTokens = chatChannel.registrationTokens

        val newAdministrators = administrators.removeItemFromList(user.id)
        val newTokens = channelTokens.removeItemsFromList(user.registrationTokens)

        Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannel.chatChannelId)
            .update(mapOf(ADMINISTRATORS to newAdministrators, REGISTRATION_TOKENS to newTokens))
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



    /*fun removeSubscriptions(onComplete: (task: Task<Void>) -> Unit) {

        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)

        currentUserRef.update("premiumState", -1).addOnCompleteListener(onComplete)
    }*/

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

    suspend fun updateProject(
        project: Project,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val names = mutableListOf<String>()
        for (img in project.images) {
            names.add(randomId())
        }

        val newListOfImages = mutableListOf<String>()

        val alreadyUploadedImages = project.images.filter {
            it.contains("https")
        }

        newListOfImages.addAll(alreadyUploadedImages)

        val toBeUploadedImages = project.images.filter {
            !it.contains("https")
        }

        val downloadedUris = uploadItems("${project.id}/images", names, toBeUploadedImages.map { it.toUri() })
        newListOfImages.addAll(downloadedUris.map { it.toString() })

        val changes = mutableMapOf(
            "name" to project.name,
            "content" to project.content,
            "images" to newListOfImages,
            "tags" to project.tags,
            "sources" to project.sources
        )

        Firebase.firestore.collection(PROJECTS).document(project.id)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun getCommentChannel(commentChannelId: String, onComplete: (Result<CommentChannel>?) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS).document(commentChannelId)
            .get()
            .addOnCompleteListener {
                val result = if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val commentChannel = it.result.toObject(CommentChannel::class.java)!!
                        Result.Success(commentChannel)
                    } else {
                        null
                    }
                } else {
                    it.exception?.let { it1 -> Result.Error(it1) }
                }
                onComplete(result)
            }
    }

}