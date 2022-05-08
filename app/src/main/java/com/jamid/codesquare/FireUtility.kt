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

    suspend fun createPost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val ref = Firebase.firestore.collection(POSTS).document(post.id)

        val listOfNames = mutableListOf<String>()
        for (i in post.images.indices) {
            listOfNames.add(randomId())
        }

        val downloadUris =
            uploadItems("images/posts/${post.id}", listOfNames, post.images.map { it.toUri() })

        val downloadUrls = downloadUris.map { it.toString() }
        post.images = downloadUrls

        val chatChannel = ChatChannel.newInstance(post)
        post.chatChannel = chatChannel.chatChannelId

        val chatChannelRef =
            Firebase.firestore.collection(CHAT_CHANNELS).document(post.chatChannel)

        val tokens = mutableListOf(currentUser.token)
        chatChannel.tokens = tokens


        val commentChannelRef = Firebase.firestore.collection(COMMENT_CHANNELS).document()
        val commentChannelId = commentChannelRef.id
        post.commentChannel = commentChannelId

        val commentChannel = CommentChannel(
            commentChannelId,
            post.id,
            post.id,
            post.name,
            0,
            post.createdAt,
            null
        )

        val batch = Firebase.firestore.batch()

        batch.set(chatChannelRef, chatChannel)
        batch.set(commentChannelRef, commentChannel)

        batch.set(ref, post)

        val userChanges = mapOf<String, Any?>(
            POSTS_COUNT to FieldValue.increment(1),
            POSTS to FieldValue.arrayUnion(post.id),
            CHAT_CHANNELS to FieldValue.arrayUnion(post.chatChannel)
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
        val ref = Firebase.storage.reference.child("images/users/$locationId/$randomImageName.jpg")
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
        shouldUpdatePosts: Boolean = true,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        // updating user
        val mAuth = Firebase.auth

        if (mAuth.currentUser != null) {
            val currentUserRef = db.collection(USERS).document(mAuth.currentUser!!.uid)
            batch.update(currentUserRef, changes)

            // updating posts where the creator is current user
            if (shouldUpdatePosts) {
                val currentUser = UserManager.currentUser
                for (post in currentUser.posts) {
                    val ref = db.collection(POSTS).document(post)
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

    fun likePost2(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val postRef = Firebase.firestore.collection(POSTS)
            .document(post.id)

        val now = System.currentTimeMillis()

        val postChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(postRef, postChanges)

        val likedByRef = postRef.collection("likedBy").document(currentUser.id)
        val likedBy = LikedBy(currentUser.id, currentUser.minify(), now)
        batch.set(likedByRef, likedBy)

        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val likePostRef = currentUserRef.collection("likedPosts").document(post.id)
        val likedPostDoc = mapOf(ID to post.id, CREATED_AT to now)
        batch.set(likePostRef, likedPostDoc)

        val currentUserChanges = mapOf("likedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isLiked = true
            post.likesCount += 1
            post.updatedAt = now
            onComplete(post, it)
        }
    }

    fun likePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val postRef = Firebase.firestore.collection(POSTS)
            .document(post.id)

        val batch = Firebase.firestore.batch()
        val userRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        batch.update(postRef, LIKES_COUNT, FieldValue.increment(1))
        batch.update(userRef, LIKED_POSTS, FieldValue.arrayUnion(post.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun dislikePost2(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val postRef = Firebase.firestore.collection(POSTS)
            .document(post.id)

        val now = System.currentTimeMillis()

        val postChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(postRef, postChanges)

        val likedByRef = postRef.collection("likedBy").document(currentUser.id)
        batch.delete(likedByRef)

        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val likePostRef = currentUserRef.collection("likedPosts").document(post.id)
        batch.delete(likePostRef)

        val currentUserChanges = mapOf("likedPostsCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isLiked = false
            post.likesCount -= 1
            post.updatedAt = now
            onComplete(post, it)
        }
    }

    fun dislikePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val postRef = Firebase.firestore.collection(POSTS)
            .document(post.id)

        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(currentUser.id)

        val batch = Firebase.firestore.batch()

        batch.update(postRef, LIKES_COUNT, FieldValue.increment(-1))
        batch.update(currentUserRef, LIKED_POSTS, FieldValue.arrayRemove(post.id))

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun savePost2(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val now = System.currentTimeMillis()

        val currentUserRef = db.collection(USERS).document(currentUser.id)
        val savedPostDocRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        val savedPostDoc = mapOf(ID to post.id, CREATED_AT to now)
        batch.set(savedPostDocRef, savedPostDoc)

        val currentUserChanges = mapOf("savedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isSaved = true
            onComplete(post, it)
        }
    }

    fun savePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef =
            Firebase.firestore.collection(USERS).document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()
        batch.update(currentUserRef, SAVED_POSTS, FieldValue.arrayUnion(post.id))
        val savedPostRef = currentUserRef.collection(SAVED_POSTS).document(post.id)

        batch.set(savedPostRef, post)
        batch.commit().addOnCompleteListener(onComplete)
    }

    fun undoSavePost(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val now = System.currentTimeMillis()

        val currentUserRef = db.collection(USERS).document(currentUser.id)
        val savedPostDocRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        batch.delete(savedPostDocRef)

        val currentUserChanges = mapOf("savedPostsCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isSaved = false
            onComplete(post, it)
        }
    }

    fun unSavePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUserRef = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)

        val batch = Firebase.firestore.batch()

        batch.update(currentUserRef, SAVED_POSTS, FieldValue.arrayRemove(post.id))

        batch.delete(currentUserRef.collection(SAVED_POSTS).document(post.id))

        batch.commit().addOnCompleteListener(onComplete)
    }


    /**
     * @param notificationId A notification to send along with the action of sending a
     * request to join the post
     * @param post Post for which the user is sending request to
     * @param onComplete Callback function for completion of sending post request
     *
     * User can send a post request to join a post. The creator of the post can see
     * this request. The request can be removed in the future.
     *
     * */
    fun joinPost(
        notificationId: String,
        post: Post,
        onComplete: (task: Task<Void>, postRequest: PostRequest) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()
        val currentUser = UserManager.currentUser
        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val postRequestRef = db.collection(POST_REQUESTS).document()
        val requestId = postRequestRef.id

        val postRef = db.collection(POSTS).document(post.id)
        val postRequest = PostRequest(
            requestId,
            post.id,
            currentUser.id,
            post.creator.userId,
            post.minify(),
            currentUser.minify(),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            notificationId
        )

        val userChanges = mapOf(POST_REQUESTS to FieldValue.arrayUnion(requestId))

        val postChanges = mapOf(REQUESTS to FieldValue.arrayUnion(requestId))

        // create new post request
        batch.set(postRequestRef, postRequest)
            .update(currentUserRef, userChanges)
            .update(postRef, postChanges)
            .commit().addOnCompleteListener {
                onComplete(it, postRequest)
            }
    }

    /**
     * @param postRequest The post request to revoke
     * @param onComplete Callback function for undoing post request
     *
     * Undoing a post request which was sent earlier
    * */
    fun undoJoinPost(postRequest: PostRequest, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserRef = db.collection(USERS).document(postRequest.senderId)
        val postRequestRef = db.collection(POST_REQUESTS).document(postRequest.requestId)
        val postRef = db.collection(POSTS).document(postRequest.postId)

        val userChanges = mapOf(
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.requestId)
        )

        val postChanges = mapOf(
            REQUESTS to FieldValue.arrayRemove(postRequest.requestId)
        )

        val requestNotificationRef =
            Firebase.firestore.collection(USERS).document(postRequest.senderId).collection(
                NOTIFICATIONS
            ).document(postRequest.notificationId)


        batch.update(postRef, postChanges)

        batch.update(currentUserRef, userChanges)

        batch.delete(postRequestRef)

        batch.delete(requestNotificationRef)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * @param post The post associated with the given post request
     * @param postRequest The post request to accept
     * @param onComplete Callback function for completion of accepting post request
     *
     * Accepting post request. The user who requested to join, will be added to the post.
     *
     * */
    fun acceptPostRequest(
        post: Post,
        postRequest: PostRequest,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        val senderRef = db.collection(USERS).document(postRequest.senderId)
        val postRequestRef = db.collection(POST_REQUESTS).document(postRequest.requestId)

        val senderChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayUnion(postRequest.postId),
            COLLABORATIONS_COUNT to FieldValue.increment(1),
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.postId),
            CHAT_CHANNELS to FieldValue.arrayUnion(post.chatChannel)
        )

        val currentUserNotificationRef =
            Firebase.firestore.collection(USERS).document(postRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(postRequest.notificationId)

        batch.addNewUserToPost(post.id, post.chatChannel, SourceInfo(postRequest.requestId, null, postRequest.senderId), UserManager.currentUser.token)
            .updateParticularDocument(senderRef, senderChanges)
            .deleteParticularDocument(postRequestRef)
            .deleteParticularDocument(currentUserNotificationRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * @param postRequest The post request to reject
     * @param onComplete Callback function for rejecting post request
     *
     * Rejecting a post request.
     *
    * */
    fun rejectRequest(postRequest: PostRequest, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val postRef = db.collection(POSTS).document(postRequest.postId)
        val senderRef = db.collection(USERS).document(postRequest.senderId)
        val requestRef = db.collection(POST_REQUESTS).document(postRequest.requestId)

        val postChanges = mapOf(REQUESTS to FieldValue.arrayRemove(postRequest.requestId))

        val senderChanges = mapOf(
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.postId)
        )
        val batch = db.batch()
        val requestNotificationRef =
            Firebase.firestore.collection(USERS).document(postRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(postRequest.notificationId)

        batch.update(postRef, postChanges)
            .update(senderRef, senderChanges)
            .delete(requestRef)
            .delete(requestNotificationRef)
            .commit()
            .addOnCompleteListener(onComplete)

    }

    fun likeUser2(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        val now = System.currentTimeMillis()

        val currentUserReference = db.collection(USERS).document(currentUser.id)
        val otherUserReference = db.collection(USERS).document(userId)

        /* creating new doc for liked user */
        val likedUserDocRef = currentUserReference.collection(LIKED_USERS).document(userId)
        val likedUserDoc = mapOf(ID to userId, CREATED_AT to now)
        batch.set(likedUserDocRef, likedUserDoc)

        /* updating other user */
        val otherUserChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(otherUserReference, otherUserChanges)


        val likedByDocRef = otherUserReference.collection("likedBy").document(currentUser.id)
        val likedByDoc = LikedBy(currentUser.id, currentUser.minify(), now)
        batch.set(likedByDocRef, likedByDoc)


        /* updating current user */
        val currentUserChanges = mapOf("likedUsersCount" to FieldValue.increment(1), UPDATED_AT to System.currentTimeMillis())
        batch.update(currentUserReference, currentUserChanges)

        batch.commit()
            .addOnCompleteListener(onComplete)

    }

    fun dislikeUser2(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        val now = System.currentTimeMillis()

        val currentUserReference = db.collection(USERS).document(currentUser.id)
        val otherUserReference = db.collection(USERS).document(userId)

        /* deleting liked user */
        val newLikedUserDocRef = currentUserReference.collection(LIKED_USERS).document(userId)
        batch.delete(newLikedUserDocRef)

        /* updating other user */
        val otherUserChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(otherUserReference, otherUserChanges)

        val likedByDocRef = otherUserReference.collection("likedBy").document(currentUser.id)
        batch.delete(likedByDocRef)

        /* updating current user */
        val currentUserChanges = mapOf("likedUsersCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserReference, currentUserChanges)

        batch.commit()
            .addOnCompleteListener(onComplete)
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
            comment.postId,
            comment.postTitle,
            0,
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

        val parentCommentChannelChanges = mapOf(LAST_COMMENT to comment, COMMENTS_COUNT to FieldValue.increment(1), UPDATED_AT to System.currentTimeMillis())
        batch.update(parentCommentChannelRef, parentCommentChannelChanges)

        val postRef = db.collection(POSTS).document(comment.postId)
        val postChanges = mapOf(COMMENTS_COUNT to FieldValue.increment(1), UPDATED_AT to System.currentTimeMillis())
        batch.update(postRef, postChanges)

        // update the parent comment replies count
        if (comment.commentLevel.toInt() != 0) {
            val parentRef = commentCollectionRef
                .document(parentCommentChannelId!!).collection(COMMENTS)
                .document(comment.parentId)

            batch.update(parentRef, mapOf(REPLIES_COUNT to FieldValue.increment(1), UPDATED_AT to System.currentTimeMillis()))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun likeComment2(comment: Comment, onComplete: (newComment: Comment, task: Task<Transaction>) -> Unit) {
        val db = Firebase.firestore
        val currentUser = UserManager.currentUser

        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val now = System.currentTimeMillis()
        comment.isLiked = true
        comment.likesCount += 1
        comment.updatedAt = now

        Log.d(TAG, "likeComment2: Liking comment with id: ${comment.commentId}")

        db.runTransaction {
            val commentChannelRef = db.collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
            val commentChannelSnapshot = it.get(commentChannelRef)
            val lastCommentId = commentChannelSnapshot.get("lastComment.commentId") as String?
            if (lastCommentId != null) {
                if (lastCommentId == comment.commentId) {
                    // update comment channel
                    val commentChannelChanges = mapOf(LAST_COMMENT to comment, UPDATED_AT to now)
                    it.update(commentChannelRef, commentChannelChanges)
                } else {
                    Log.e(TAG, "likeComment2: Ignoring changes to comment channel because comment is not the last comment", )
                }
            } else {
                Log.e(TAG, "likeComment2: There is no last comment, this must be the first comment")
            }

            val commentRef = Firebase.firestore
                .collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
                .collection(COMMENTS)
                .document(comment.commentId)

            val commentChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
            it.update(commentRef, commentChanges)

            val likedCommentRef = currentUserRef.collection(LIKED_COMMENTS).document(comment.commentId)
            val likedCommentDoc = mapOf(ID to comment.commentId, COMMENT_CHANNEL_ID to comment.commentChannelId, CREATED_AT to now)
            it.set(likedCommentRef, likedCommentDoc)

            val likedByRef = commentRef.collection("likedBy").document(currentUser.id)
            val likedByDoc = LikedBy(currentUser.id, currentUser.minify(), now)
            it.set(likedByRef, likedByDoc)

            val currentUserChanges = mapOf("likedCommentsCount" to FieldValue.increment(1), UPDATED_AT to now)
            it.update(currentUserRef, currentUserChanges)
        }.addOnCompleteListener {

            if (!it.isSuccessful) {
                Log.e(TAG, "likeComment2: Liking comment unsuccessful ${it.exception?.localizedMessage}")
            }

            onComplete(comment, it)
        }
    }

    fun dislikeComment2(comment: Comment, onComplete: (newComment: Comment, task: Task<Transaction>) -> Unit) {
        val db = Firebase.firestore
        val currentUser = UserManager.currentUser

        val currentUserRef = db.collection(USERS).document(currentUser.id)

        val now = System.currentTimeMillis()
        comment.isLiked = false
        comment.likesCount -= 1
        comment.updatedAt = now

        db.runTransaction {
            val commentChannelRef = db.collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
            val commentChannelSnapshot = it.get(commentChannelRef)
            val lastCommentId = commentChannelSnapshot.get("lastComment.commentId") as String?
            if (lastCommentId != null) {
                if (lastCommentId == comment.commentId) {
                    // update comment channel
                    val commentChannelChanges = mapOf(LAST_COMMENT to comment, UPDATED_AT to now)
                    it.update(commentChannelRef, commentChannelChanges)
                } else {
                    Log.d(
                        TAG,
                        "dislikeComment2: Ignoring changes to comment channel because comment is not the last comment"
                    )
                }
            } else {
                Log.d(
                    TAG,
                    "dislikeComment2: There is no last comment, this must be the first comment"
                )
            }

            val commentRef = Firebase.firestore
                .collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
                .collection(COMMENTS)
                .document(comment.commentId)

            val commentChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
            it.update(commentRef, commentChanges)

            val likedCommentRef = currentUserRef.collection(LIKED_COMMENTS).document(comment.commentId)
            it.delete(likedCommentRef)

            val likedByRef = commentRef.collection("likedBy").document(currentUser.id)
            it.delete(likedByRef)

            val currentUserChanges = mapOf("likedCommentsCount" to FieldValue.increment(-1), UPDATED_AT to now)
            it.update(currentUserRef, currentUserChanges)

        }.addOnCompleteListener {
            onComplete(comment, it)
        }
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
                    "images/chatChannels/$chatChannelId",
                    mediaMessages.map { it.content },
                    mediaMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems(
                    "documents/chatChannels/$chatChannelId",
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
                    "images/chatChannels/$chatChannelId",
                    listOfMessages.map { it.content },
                    listOfMessages.map { it.metadata!!.url.toUri() }).map { it.toString() }
            } else {
                uploadItems(
                    "documents/chatChannels/$chatChannelId",
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
            "images/chatChannels/${message.chatChannelId}/$name"
        } else {
            "documents/chatChannels/${message.chatChannelId}/$name"
        }
        val objRef = Firebase.storage.reference.child(path)
        objRef.getFile(destinationFile).addOnCompleteListener(onComplete)
    }



    fun deleteComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)
            .delete()
            .addOnCompleteListener(onComplete)
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
                        "images/chatChannels/${it.chatChannelId}",
                        imagesMessages.map { it1 -> it1.content },
                        imageUris
                    )
                } else {
                    emptyList()
                }

                val downloadedDocumentUris = if (documentMessages.isNotEmpty()) {
                    uploadItems(
                        "documents/chatChannels/${it.chatChannelId}",
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

        val toBeUploadedImages = uploadItems("images/reports/${report.id}", names, report.snapshots.map { it.toUri() })
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


    fun removeUserFromPost(
        user: User,
        postId: String,
        chatChannelId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val contributorRef = Firebase.firestore
            .collection(USERS)
            .document(user.id)

        val batch = Firebase.firestore.batch()

        val userChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayRemove(postId),
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

        val postRef = Firebase.firestore
            .collection(POSTS)
            .document(postId)

        val postChanges = mapOf(
            CONTRIBUTORS to FieldValue.arrayRemove(user.id)
        )

        batch.update(postRef, postChanges)
        batch.update(chatChannelRef, channelChanges)
        batch.update(contributorRef, userChanges)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * For inviting an user to the post created by current user
     *
     * @param post The post on which the current user wants to invite someone
     * @param receiverId The receiver of this invite
     * @param notificationId A notification associated with this invite
     * @param onComplete Callback function for completion of inviting user to post
    * */
    fun inviteUserToPost(
        post: Post,
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

        val postInvite = PostInvite(
            ref.id,
            post.id,
            receiverId,
            currentUser.id,
            currentUser.minify(),
            post.minify(),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            notificationId = notificationId
        )

        val currentUserChanges = mapOf(
            POST_INVITES to FieldValue.arrayUnion(postInvite.id)
        )

        batch.set(ref, postInvite)
            .update(db.collection(USERS).document(post.creator.userId), currentUserChanges)
            .commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * To revoke an invite which was earlier sent by the current user and not yet taken action by the receiver
     *
     * @param invite The invite to revoke
     * @param onComplete Callback function for completion of revoking invite
    * */
    fun revokeInvite(invite: PostInvite, onComplete: (task: Task<Void>) -> Unit) {
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
            POST_INVITES to FieldValue.arrayRemove(invite.id)
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
     * @param postId Post id associated with the invite
     * @param otherUserId The receiver of the invite
     * @param currentUserId The sender of the invite
     * @param onComplete Callback function for completion of getting existing invite
    * */
    fun getExistingInvite(
        postId: String,
        otherUserId: String,
        currentUserId: String,
        onComplete: (Result<PostInvite>?) -> Unit
    ) {
        Firebase.firestore.collection(USERS)
            .document(otherUserId)
            .collection(INVITES)
            .whereEqualTo(SENDER_ID, currentUserId)
            .whereEqualTo(POST_ID, postId)
            .limit(1)
            .get()
            .addOnCompleteListener {
                val result = if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (!querySnapshot.isEmpty) {
                        // there were results, which means there is an existing invite with this detail
                        val postInvite =
                            querySnapshot.toObjects(PostInvite::class.java).first()
                        Result.Success(postInvite)
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
     * To get an existing post request
     *
     * @param postId Post id associated with the request
     * @param senderId The user who sent the request to current user
     * @param onComplete Callback function for getting existing post request
    * */
    fun getPostRequest(
        postId: String,
        senderId: String,
        onComplete: (Result<PostRequest>?) -> Unit
    ) {
        Firebase.firestore.collection(POST_REQUESTS)
            .whereEqualTo(SENDER_ID, senderId)
            .whereEqualTo(POST_ID, postId)
            .limit(1)
            .get()
            .addOnCompleteListener {
                val result = if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (!querySnapshot.isEmpty) {
                        val postRequest =
                            querySnapshot.toObjects(PostRequest::class.java).first()
                        Result.Success(postRequest)
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
     * To get a post from firestore, not intended for use in main thread
     * @see getPost use this method to get post from firestore in main thread
     * @param postId The id of the post to fetch
     * @return Result of post data
    * */
    suspend fun getPost(postId: String): Result<Post>? {
        val ref = Firebase.firestore.collection(POSTS).document(postId)
        return when (val result = getDocument(ref)) {
            is Result.Error -> {
                Result.Error(result.exception)
            }
            is Result.Success -> {
                val snapshot = result.data
                if (snapshot.exists()) {
                    val post = snapshot.toObject(Post::class.java)!!
                    Result.Success(processPosts(arrayOf(post)).first())
                } else {
                    null
                }
            }
        }
    }

    /**
     * @param postId Id of the post
     * @param onComplete Callback function for getting the post
    * */
    fun getPost(postId: String, onComplete: (Result<Post>?) -> Unit) {
        val ref = Firebase.firestore.collection(POSTS).document(postId)
        getDocument(ref) {
            if (it.isSuccessful) {
                if (it.result.exists()) {
                    onComplete(Result.Success(processPosts(arrayOf(it.result.toObject(Post::class.java)!!)).first()))
                } else {
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
                    Result.Success(processUsers(user).first())
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
                    onComplete(Result.Success(processUsers(it.result.toObject(User::class.java)!!).first()))
                } else {
                    onComplete(null)
                }
            } else {
                onComplete(it.exception?.let { it1 -> Result.Error(it1) })
            }
        }
    }

    /**
     * To archive a post
     * 1. The post must be removed from global posts directory and added to user's personal archive gallery
     * 2. The post cannot accept further invites as it will be unreachable, but the users who already sent request needs to be notified that the post has been archived
     * 3. The users who are linked to this post must also be notified that the post has been archived
     * 4. Chat and comment channels of the post should be archived too
     *
     * @param post The post to be archived
     * @param onComplete Callback function for archive function
     *
     * */
    fun archivePost(
        post: Post,
        duration: Long = 30L * 24L * 60L * 60L * 1000L,
        onComplete: (newPost: Post, task: Task<Void>) -> Unit
    ) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore

        val postRef = db.collection(POSTS).document(post.id)
        val currentUserRef = db.collection(USERS).document(currentUserId)
        val archiveRef = currentUserRef.collection("archivedPosts").document(post.id)
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(post.commentChannel)
        val chatChannelRef = db.collection(CHAT_CHANNELS).document(post.chatChannel)

        val batch = db.batch()

        val now = System.currentTimeMillis()

        val postChanges = mapOf(ARCHIVED to true, UPDATED_AT to now, EXPIRED_AT to now + duration)
        batch.update(postRef, postChanges)

        val referenceItemDoc = ReferenceItem(post.id, now)
        batch.set(archiveRef, referenceItemDoc)

        // changing the status of the CommentChannel and ChatChannel to archived
        val changes = mapOf(ARCHIVED to true)

        // updating comment channel
        batch.update(commentChannelRef, changes)

        // updating chat channel
        batch.update(chatChannelRef, changes)

        // updating all contributors that they aren't actually contributing now
        val contributorsListExcludingCurrentUser = post.contributors.filter { it != currentUserId }
        for (contributor in contributorsListExcludingCurrentUser) {
            val ref = db.collection(USERS).document(contributor)
            val changes1 = mapOf(
                COLLABORATIONS to FieldValue.arrayRemove(post.id),
                COLLABORATIONS_COUNT to FieldValue.increment(-1),
                UPDATED_AT to now
            )
            batch.update(ref, changes1)
        }

        // updating current user
        val currentUserChanges = mapOf(
            ARCHIVED_POSTS to FieldValue.arrayUnion(post.id),
            POSTS to FieldValue.arrayRemove(post.id),
            POSTS_COUNT to FieldValue.increment(-1),
            UPDATED_AT to System.currentTimeMillis()
        )
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.archived = true
            post.updatedAt = now
            post.expiredAt = now + duration

            onComplete(post, it)
        }
    }


    /**
     * @param post Post ato be un-archived
     * @param onComplete Callback function for un-archiving the post
    * */
    fun unArchivePost(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val currentUserId = UserManager.currentUserId
        val db = Firebase.firestore

        val currentUserRef = db.collection(USERS).document(currentUserId)

        val now = System.currentTimeMillis()

        val postRef = db.collection(POSTS).document(post.id)
        val archivedPostRef = currentUserRef.collection(ARCHIVED_POSTS).document(post.id)
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(post.commentChannel)
        val chatChannelRef = db.collection(CHAT_CHANNELS).document(post.chatChannel)

        val batch = db.batch()


        val postChanges = mapOf(ARCHIVED to false, UPDATED_AT to now, EXPIRED_AT to (-1).toLong())

        batch.update(postRef, postChanges)

        batch.delete(archivedPostRef)

        // updating all contributors that they aren't actually contributing now
        val contributorsListExcludingCurrentUser = post.contributors.filter { it != currentUserId }
        for (contributor in contributorsListExcludingCurrentUser) {
            val ref = db.collection(USERS).document(contributor)
            val changes1 = mapOf(
                COLLABORATIONS to FieldValue.arrayUnion(post.id),
                COLLABORATIONS_COUNT to FieldValue.increment(1),
                UPDATED_AT to now
            )
            batch.update(ref, changes1)
        }

        // changing the status of the CommentChannel and ChatChannel to archived
        val changes = mapOf(ARCHIVED to false)

        // updating comment channel
        batch.update(commentChannelRef, changes)

        // updating  chat channel
        batch.update(chatChannelRef, changes)

        // updating current user
        val currentUserChanges = mapOf(
            ARCHIVED_POSTS to FieldValue.arrayRemove(post.id),
            POSTS to FieldValue.arrayUnion(post.id),
            POSTS_COUNT to FieldValue.increment(1),
            UPDATED_AT to now
        )
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.archived = false
            post.updatedAt = now
            post.expiredAt = -1
            onComplete(post, it)
        }

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
     * @param postRequest Post request sent by other user
     * @param onComplete Callback function for deleting post request
     *
     * Deleting post request
     * */
    fun deletePostRequest(
        postRequest: PostRequest,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val requestRef = db.collection(POST_REQUESTS).document(postRequest.requestId)
        val requestSenderRef = db.collection(USERS).document(postRequest.senderId)
        val requestSenderChanges =
            mapOf(POST_REQUESTS to FieldValue.arrayRemove(postRequest.requestId))

        val batch = db.batch()

        batch.updateParticularDocument(requestSenderRef, requestSenderChanges)
            .deleteParticularDocument(requestRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }

    /**
     * @param currentUser The user who is accepting the invite
     * @param postInvite The invite received by current user
     * @param onComplete Callback function for accepting the post invite
     *
     * To accept the post invite, following changes to be made in the database:
     * 1. Post document must include the new contributor in contributors list and subsequently, the contributors count must also increase.
     * 2. Chat channel associated with this post must also reflect change such as, contributorsList, contributorsCount, updatedAt and registrationToken of current user.
     * 3. The current user document must include the post id in collaborations, chatChannel associated with this post, collaborationsCount and updatedAt.
     * 4. The senders document must remove the current post invite.
     * 5. Lastly, the post invite itself needs to be deleted from current user invites collection.
     * 6. For cleanup, delete the notification received by the current user because it is of no use.
     * */
    fun acceptPostInvite(
        currentUser: User,
        postInvite: PostInvite,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val post = postInvite.post
        val currentUserId = currentUser.id
        val currentUserRegistrationToken = currentUser.token

        val batch = db.batch()
        val postInviteSenderReference = db.collection(USERS).document(postInvite.senderId)

        val postInviteReference =
            db.collection(USERS).document(currentUserId).collection(INVITES)
                .document(postInvite.id)
        batch.delete(postInviteReference)

        val currentUserChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayUnion(post.id),
            COLLABORATIONS_COUNT to FieldValue.increment(1),
            CHAT_CHANNELS to FieldValue.arrayUnion(post.chatChannel),
            UPDATED_AT to System.currentTimeMillis()
        )

        val postInviteSenderDocumentChanges = mapOf(
            POST_INVITES to FieldValue.arrayRemove(postInvite.id),
            UPDATED_AT to System.currentTimeMillis()
        )

        val inviteNotificationRef =
            Firebase.firestore.collection(USERS).document(postInvite.receiverId)
                .collection(NOTIFICATIONS)
                .document(postInvite.notificationId)

        batch.addNewUserToPost(
            post.id,
            post.chatChannel,
            SourceInfo(null, postInvite.id, currentUserId),
            currentUserRegistrationToken
        )
            .updateCurrentUser(currentUserId, currentUserChanges)
            .updateParticularDocument(
                postInviteSenderReference,
                postInviteSenderDocumentChanges
            )
            .deleteParticularDocument(postInviteReference)
            .deleteParticularDocument(inviteNotificationRef)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }


    data class SourceInfo(val requestId: String?, val inviteId: String?, val senderId: String)

    private fun WriteBatch.addNewUserToPost(
        postId: String,
        chatChannelId: String,
        sourceInfo: SourceInfo,
        token: String
    ): WriteBatch {
        val postReference = Firebase.firestore.collection(POSTS).document(postId)
        val changes1 = mutableMapOf(
            CONTRIBUTORS to FieldValue.arrayUnion(sourceInfo.senderId),
            CONTRIBUTORS_COUNT to FieldValue.increment(1),
            UPDATED_AT to System.currentTimeMillis()
        )

        if (sourceInfo.requestId != null) {
            changes1[REQUESTS] = FieldValue.arrayRemove(sourceInfo.requestId)
        }

        /*if (sourceInfo.inviteId != null) {

        }*/

        val chatChannelReference =
            Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
        val changes2 = mutableMapOf(
            CONTRIBUTORS_COUNT to FieldValue.increment(1),
            CONTRIBUTORS to FieldValue.arrayUnion(sourceInfo.senderId),
            UPDATED_AT to System.currentTimeMillis()
        )

        if (token.isNotEmpty()) {
            changes2[TOKENS] = FieldValue.arrayUnion(token)
        }

        return this.update(postReference, changes1)
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
     * To cancel a post invite
     * @param postInvite The invite to be cancelled
     * @param onComplete Callback function for cancelling post invite
    * */
    fun cancelPostInvite(
        postInvite: PostInvite,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserId = UserManager.currentUserId

        val currentUserRef = db.collection(USERS).document(currentUserId)
        val postInviteRef = currentUserRef.collection(INVITES).document(postInvite.id)
        val senderRef = db.collection(USERS).document(postInvite.senderId)

        val inviteNotificationRef =
            Firebase.firestore.collection(USERS).document(postInvite.receiverId).collection(
                NOTIFICATIONS
            ).document(postInvite.notificationId)

        batch.delete(inviteNotificationRef)
            .update(senderRef, mapOf(POST_INVITES to FieldValue.arrayRemove(postInvite.id)))
            .delete(postInviteRef)
            .commit()
            .addOnCompleteListener(onComplete)
    }


    /**
     * To delete post invite
     * @param postInvite Post invite to be deleted
     * @param onComplete Callback function for deleting post invite
    * */
    fun deletePostInvite(postInvite: PostInvite, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val senderRef = db.collection(USERS).document(postInvite.senderId)
        val postInviteRef = db.collection(USERS)
            .document(postInvite.receiverId)
            .collection(INVITES)
            .document(postInvite.id)

        batch.update(senderRef, mapOf(POST_INVITES to FieldValue.arrayRemove(postInvite.id)))
        batch.delete(postInviteRef)

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getRandomInterests(onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        Firebase.firestore.collection(INTERESTS)
            .document(INTERESTS_COLLECTION)
            .get()
            .addOnCompleteListener(onComplete)
    }

    fun uploadUser(user: User, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(USERS)
            .document(user.id)
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

    fun downloadAllUserPosts(onComplete: (result: Result<List<Post>>?) -> Unit) {
        val currentUser = UserManager.currentUser
        Firebase.firestore.collection(POSTS)
            .whereEqualTo("$CREATOR.$USER_ID", currentUser.id)
            // TODO(".whereEqualTo("archived", false)")
            .whereEqualTo(EXPIRED_AT, -1)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val posts = querySnapshot.toObjects(Post::class.java)
                        onComplete(Result.Success(posts))
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
     * @param post The post to be deleted
     * @param onComplete Callback function for deletion
     *
     * To delete a post
     * 1. Delete the post
     * 2. Delete the chat channel based on the post
     * 3. Remove post id from collaboration list from all the contributors
     * 4. Remove post id from post list from current user document
     * 5. Remove all post requests that has this post id
     * 6. Remove all invites if there are any
     * */
    fun deletePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val db = Firebase.firestore
        val batch = db.batch()

        val currentUserId = UserManager.currentUserId

        // because this function can only be invoked from archive fragment
        val postRef = db.collection(USERS)
            .document(post.creator.userId)
            .collection(ARCHIVE)
            .document(post.id)

        val chatChannelRef = db.collection(CHAT_CHANNELS)
            .document(post.chatChannel)

        val currentUserRef = db.collection(USERS).document(currentUserId)

        val commentChannelRef = db.collection(COMMENT_CHANNELS)
            .document(post.commentChannel)

        batch.delete(postRef)
        batch.delete(chatChannelRef)
        batch.delete(commentChannelRef)
        batch.update(
            currentUserRef, mapOf(
                POSTS to FieldValue.arrayRemove(post.id),
                POSTS_COUNT to FieldValue.increment(-1)
            )
        )

        for (id in post.contributors) {
            val ref = db.collection(USERS).document(id)
            batch.update(ref, COLLABORATIONS, FieldValue.arrayRemove(post.id))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun postDeletePost(
        postRequests: List<PostRequest>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val db = Firebase.firestore
        val batch = db.batch()

        for (request in postRequests) {
            val requestRef = db.collection(POST_REQUESTS).document(request.requestId)
            batch.delete(requestRef)

            val senderRef = db.collection(USERS).document(request.senderId)
            batch.update(senderRef, POST_REQUESTS, FieldValue.arrayRemove(request.requestId))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getAllRequestsForPost(
        post: Post,
        onComplete: (task: Result<List<PostRequest>>?) -> Unit
    ) {
        Firebase.firestore.collection(POST_REQUESTS)
            .whereEqualTo(POST_ID, post.id)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val querySnapshot = it.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val postRequests = querySnapshot.toObjects(PostRequest::class.java)
                        onComplete(Result.Success(postRequests))
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

    fun updatePost(postId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(POSTS).document(postId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    suspend fun updatePost(
        post: Post,
        onComplete: (newPost: Post, task: Task<Void>) -> Unit
    ) {
        val names = mutableListOf<String>()
        for (img in post.images) {
            names.add(randomId())
        }

        val newListOfImages = mutableListOf<String>()

        val alreadyUploadedImages = post.images.filter {
            it.contains(HTTPS)
        }

        newListOfImages.addAll(alreadyUploadedImages)

        val toBeUploadedImages = post.images.filter {
            !it.contains(HTTPS)
        }

        val downloadedUris = uploadItems("images/posts/${post.id}", names, toBeUploadedImages.map { it.toUri() })
        newListOfImages.addAll(downloadedUris.map { it.toString() })

        post.images = newListOfImages

        val postChanges = mutableMapOf(
            NAME to post.name,
            CONTENT to post.content,
            IMAGES to newListOfImages,
            TAGS to post.tags,
            LOCATION to post.location,
            SOURCES to post.sources,
            UPDATED_AT to System.currentTimeMillis()
        )

        val primeThumbnail = newListOfImages.first()
        val channelChanges = mapOf(
            POST_TITLE to post.name,
            POST_IMAGE to primeThumbnail,
            UPDATED_AT to System.currentTimeMillis()
        )

        val db = Firebase.firestore
        val batch = db.batch()

        val chatChannelRef = db.collection(CHAT_CHANNELS).document(post.chatChannel)
        val postRef = db.collection(POSTS).document(post.id)

        batch.update(chatChannelRef, channelChanges)
            .update(postRef, postChanges)
            .commit()
            .addOnCompleteListener {
                onComplete(post, it)
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

    fun updateComment(updatedComment: Comment, changes: Map<String, Any?>, onComplete: (newComment: Comment, Task<Transaction>) -> Unit) {
        val db = Firebase.firestore
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(updatedComment.commentChannelId)
        db.runTransaction {
            val commentChannelSnap = it.get(commentChannelRef)
            val lastComment = commentChannelSnap.get(LAST_COMMENT) as Comment?

            if (lastComment != null) {
                if (lastComment.commentId == updatedComment.commentId) {
                    val commentChannelChanges = mapOf(LAST_COMMENT to updatedComment)
                    it.update(commentChannelRef, commentChannelChanges)
                }
            }

            val commentRef = commentChannelRef
                .collection(COMMENTS).document(updatedComment.commentId)

            it.update(commentRef, changes)
        }.addOnCompleteListener {
            onComplete(updatedComment, it)
        }
    }

    fun updatePostRequest(requestId: String, changes: Map<String, Any?>, onComplete: (Task<Void>) -> Unit) {
        Firebase.firestore.collection(POST_REQUESTS).document(requestId)
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

    fun updatePostInvite(receiverId: String, inviteId: String, changes: Map<String, Any?>, onUpdate: (Task<Void>) -> Unit) {
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

    fun getMessage(chatChannelId: String, messageId: String, onComplete: (Result<Message>?) -> Unit) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES).document(messageId)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val message = it.toObject(Message::class.java)!!
                    onComplete(Result.Success(message))
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }

    fun getCommentChannel(commentChannelId: String, onComplete: (Result<CommentChannel>?) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(commentChannelId)
            .get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val commentChannel = it.toObject(CommentChannel::class.java)!!
                    onComplete(Result.Success(commentChannel))
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }

    fun insertInterestItem(interestItem: InterestItem, onComplete: (task: Task<Void>) -> Unit) {
        Firebase.firestore.collection(INTERESTS).document(interestItem.itemId)
            .set(interestItem)
            .addOnCompleteListener(onComplete)
    }

    /*fun setSecondLastCommentAsLastComment(lastComment: Comment, onComplete: (Result<Comment>) -> Unit) {
        Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(lastComment.commentChannelId)
            .collection(COMMENTS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .startAfter(lastComment.createdAt)
            .limit(1)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {

                    if (!it.isEmpty) {
                        val secondLastComment = it.toObjects(Comment::class.java).first()

                        val commentChannelChanges = mapOf(LAST_COMMENT to secondLastComment, UPDATED_AT to System.currentTimeMillis())

                        Firebase.firestore.collection(COMMENT_CHANNELS)
                            .document(lastComment.commentChannelId)
                            .update(commentChannelChanges)
                            .addOnSuccessListener {
                                onComplete(Result.Success(secondLastComment))
                            }.addOnFailureListener { it1 ->
                                onComplete(Result.Error(it1))
                            }
                    } else {
                        // there is no second comment
                        Firebase.firestore.collection(POSTS)
                            .document(lastComment.postId)
                            .get()
                            .addOnSuccessListener { it1 ->

                                if (it1 != null && it1.exists()) {
                                    val post = it1.toObject(Post::class.java)!!
                                    if (post.commentsCount.toInt() == 1) {
                                        // is the last comment

                                    }
                                }

                            }.addOnFailureListener { it1 ->
                                onComplete(Result.Error(it1))
                            }

                    }

                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }*/


}