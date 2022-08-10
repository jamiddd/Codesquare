package com.jamid.codesquare

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.jamid.codesquare.data.*
import kotlinx.coroutines.tasks.await
import java.io.File
// something simple
object FireUtility {

    private const val TAG = "FireUtility"

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private val db: FirebaseFirestore
        get() = Firebase.firestore

    private val storage: FirebaseStorage
        get() = Firebase.storage

    private val usersCollectionRef: CollectionReference = db.collection(USERS)

    /* To be accessed only when signed in */
    private val currentUserRef: DocumentReference by lazy {
        usersCollectionRef.document(
            auth.currentUser?.uid ?: randomId()
        )
    }
    private val postsCollectionRef: CollectionReference = db.collection(POSTS)
    private val chatChannelCollectionRef: CollectionReference = db.collection(CHAT_CHANNELS)
    private val commentChannelCollectionRef: CollectionReference = db.collection(COMMENT_CHANNELS)
    private val postRequestsCollectionRef: CollectionReference = db.collection(POST_REQUESTS)
    private val interestsCollectionRef: CollectionReference = db.collection(INTERESTS)
    private val rankedRulesCollectionRef: CollectionReference = db.collection("rankedRules")
    private val competitionsCollectionRef: CollectionReference = db.collection("competitions")
    private val storageRef: StorageReference = storage.reference

    /*private val imagesRef: StorageReference = storageRef.child("images")
    private val documentsRef: StorageReference = storageRef.child("documents")
    private val videosRef: StorageReference = storageRef.child("videos")*/

    private fun getQuerySnapshot(query: Query, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val task = query.get()
        task.addOnCompleteListener(onComplete)
    }

    suspend fun fetchItems(
        query: Query,
        lim: Int = 20,
        lastSnapshot: DocumentSnapshot? = null,
        ignoreTime: Boolean = false
    ): Result<QuerySnapshot> {
        return if (lastSnapshot != null) {
            try {
                val task = if (ignoreTime) {
                    query.startAfter(lastSnapshot)
                        .limit(lim.toLong())
                        .get()
                } else {
                    query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                        .startAfter(lastSnapshot)
                        .limit(lim.toLong())
                        .get()
                }

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            try {
                val task = if (ignoreTime) {
                    query.limit(lim.toLong())
                        .get()
                } else {
                    query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                        .limit(lim.toLong())
                        .get()
                }

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    /* fun signIn(email: String, password: String, onComplete: (task: Task<AuthResult>) -> Unit) {
         Firebase.auth.signInWithEmailAndPassword(email, password)
             .addOnCompleteListener(onComplete)
     }*/

    fun signIn2(email: String, password: String, onComplete: (Result<User>) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser = it.user
                if (firebaseUser != null) {
                    getUser(firebaseUser.uid) { user ->
                        if (user != null) {
                            UserManager.updateUser(user)
                            onComplete(Result.Success(user))
                        } else {
                            onComplete(Result.Error(Exception("No user found with id ${firebaseUser.uid}")))
                        }
                    }
                } else {
                    onComplete(Result.Error(Exception("Signed in successfully. But couldn't get firebaseUser")))
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }


    private fun getDocument(
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

    /*fun createAccount(
        email: String,
        password: String,
        onComplete: (task: Task<AuthResult>) -> Unit
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(onComplete)
    }*/

    fun createAccount2(
        name: String,
        email: String,
        password: String,
        onComplete: (Result<User>) -> Unit
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser = it.user
                if (firebaseUser != null) {
                    val localUser = User.newUser(firebaseUser.uid, name, email)
                    uploadUser(localUser) { uploadUserTask ->
                        if (uploadUserTask.isSuccessful) {
                            UserManager.updateUser(localUser)
                            onComplete(Result.Success(localUser))
                        } else {
                            uploadUserTask.exception?.let { it1 -> Result.Error(it1) }
                                ?.let { it2 -> onComplete(it2) }
                        }
                    }
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }


    suspend fun createPost(
        post: Post,
        mediaList: List<MediaItem>,
        thumbnailUrl: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val currentUser = UserManager.currentUser
        val ref = postsCollectionRef.document(post.id)

        val listOfUploadItems = mutableListOf<UploadItem>()

        for (i in mediaList.indices) {
            val mediaItem = mediaList[i]
            val uploadItem = if (mediaItem.type == video) {
                UploadItem(
                    mediaItem.url.toUri(),
                    "videos/posts/${post.id}",
                    randomId() + mediaItem.ext
                )
            } else {
                UploadItem(
                    mediaItem.url.toUri(),
                    "images/posts/${post.id}",
                    randomId() + mediaItem.ext
                )
            }
            listOfUploadItems.add(uploadItem)
        }

        listOfUploadItems.add(
            UploadItem(
                thumbnailUrl.toUri(),
                "images/posts/${post.id}/thumbnails",
                randomId() + ".jpg"
            )
        )

        val upList = uploadItems2(listOfUploadItems)

        post.thumbnail = upList.last()
        post.mediaList = upList.subList(0, upList.size - 1)
        post.mediaString = getMediaStringForPost(mediaList)

        val chatChannel = ChatChannel.newInstance(post)
        post.chatChannel = chatChannel.chatChannelId

        val chatChannelRef =
            chatChannelCollectionRef.document(post.chatChannel)

        val tokens = mutableListOf(currentUser.token)
        chatChannel.tokens = tokens

        val commentChannelRef = commentChannelCollectionRef.document()
        val commentChannelId = commentChannelRef.id
        post.commentChannel = commentChannelId

        val commentChannel = CommentChannel(
            commentChannelId,
            post.id,
            post.id,
            0,
            post.createdAt,
            null
        )

        val batch = db.batch()

        batch.set(chatChannelRef, chatChannel)
        batch.set(commentChannelRef, commentChannel)

        batch.set(ref, post)

        val userChanges = mapOf<String, Any?>(
            POSTS_COUNT to FieldValue.increment(1),
            POSTS to FieldValue.arrayUnion(post.id),
            CHAT_CHANNELS to FieldValue.arrayUnion(post.chatChannel)
        )

        batch.update(usersCollectionRef.document(currentUser.id), userChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    suspend fun uploadItems2(items: List<UploadItem>): List<String> {
        val listOfRef = mutableListOf<StorageReference>()
        val listOfUploadTask = mutableListOf<UploadTask>()

        for (item in items) {
            val ref = Firebase.storage.reference.child("${item.path}/${item.name}")
            listOfRef.add(ref)

            val task = ref.putFile(item.fileUri)
            listOfUploadTask.add(task)
        }

        val listOfDownloadedImages = mutableListOf<String>()

        return try {
            for (t in listOfUploadTask.indices) {
                val task = listOfUploadTask[t]
                task.await() // upload complete

                val currentImageRef = listOfRef[t]
                val newTask = currentImageRef.downloadUrl

                val imageDownloadUri = newTask.await() // got download uri
                listOfDownloadedImages.add(imageDownloadUri.toString())
            }

            listOfDownloadedImages
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage!!)
            emptyList()
        }
    }

    private suspend fun uploadItems(
        locationPath: String,
        names: List<String>,
        items: List<Uri>
    ): List<Uri> {

        val listOfReferences = mutableListOf<StorageReference>()
        val listOfUploadTask = mutableListOf<UploadTask>()

        for (i in items.indices) {
            val ref = storageRef.child("$locationPath/${names[i]}")
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
        val ref = storageRef.child("images/users/$locationId/$randomImageName.jpg")
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


    suspend fun usernameExist(username: String): Result<Boolean> {
        return try {
            val task = usersCollectionRef
                .whereEqualTo(USERNAME, username)
                .get()

            val result = task.await()

            if (!result.isEmpty) {
                Result.Success(true)
            } else {
                Result.Success(false)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun updateUser3(changes: UserUpdate): Result<Map<String, Any>> {
        return try {
            val userId = UserManager.currentUserId
            val dataMap = mutableMapOf<String, Any>()

            val currentPhoto = UserManager.currentUser.photo

            if (changes.photo == null) {
                // photo is removed
                if (currentPhoto.isNotBlank()) {
                    dataMap["photo"] = ""
                } else {
                    Log.d(TAG, "updateUser3: No need to do anything as there was no image already.")
                }
            } else {
                // image has changed
                if (currentPhoto != changes.photo.toString()) {
                    if (changes.isPreUploadedImage) {
                        // no need to upload
                        dataMap["photo"] = changes.photo.toString()
                    } else {
                        // need to upload
                        val uploadItem =
                            UploadItem(changes.photo, "images/users/$userId", randomId() + ".jpg")
                        val images = uploadItems2(listOf(uploadItem))
                        if (images.isNotEmpty()) {
                            val profilePhoto = images.first()
                            dataMap["photo"] = profilePhoto
                        } else {
                            return Result.Error(ImageUploadException("Couldn't download image."))
                        }
                    }
                } else {
                    Log.d(TAG, "updateUser3: No need to do anything. Photo has not been changed.")
                }
            }

            if (changes.username != null && UserManager.currentUser.username != changes.username) {
                dataMap[USERNAME] = changes.username
            }

            if (changes.name != null && UserManager.currentUser.name != changes.name) {
                dataMap[NAME] = changes.name
            }

            if (changes.tag != null && UserManager.currentUser.tag != changes.tag) {
                dataMap["tag"] = changes.tag
            }

            if (changes.about != null && UserManager.currentUser.about != changes.about) {
                dataMap["about"] = changes.about
            }

            if (changes.interests.isNotEmpty() && UserManager.currentUser.interests != changes.interests) {
                dataMap["interests"] = changes.interests
            }

            val updateTask = usersCollectionRef.document(userId).update(dataMap)
            updateTask.await()
            Result.Success(dataMap)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }


    fun updateUser2(
        changes: Map<String, Any?>,
        shouldUpdatePosts: Boolean = true,
        onComplete: (task: Task<Void>) -> Unit
    ) {

        val batch = db.batch()

        // updating user
        val mAuth = Firebase.auth

        if (mAuth.currentUser != null) {
            batch.update(currentUserRef, changes)

            // updating posts where the creator is current user
            if (shouldUpdatePosts) {

                val currentUser = UserManager.currentUser

                val miniUser = UserMinimal(
                    currentUser.id,
                    changes["name"] as String? ?: currentUser.name,
                    changes["photo"] as String? ?: currentUser.photo,
                    changes["username"] as String? ?: currentUser.username,
                    currentUser.premiumState
                )

                for (post in currentUser.posts) {
                    val ref = postsCollectionRef.document(post)
                    batch.update(ref, CREATOR, miniUser)
                }
            }

            batch.commit()
                .addOnCompleteListener(onComplete)
        }
    }

    fun checkIfUserNameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val query = usersCollectionRef
            .whereEqualTo(USERNAME, username)

        getQuerySnapshot(query, onComplete)
    }

    /*fun upvotePost(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val batch = db.batch()

        val currentUser = UserManager.currentUser
        val postRef = postsCollectionRef.document(post.id)

        val now = System.currentTimeMillis()

        *//* Update post *//*
        val postChanges = mutableMapOf(
            "upvoteCount" to FieldValue.increment(1),
            UPDATED_AT to now
        )

        if (post.isDownvote) {
            postChanges["downvoteCount"] = FieldValue.increment(-1)
        }

        batch.update(postRef, postChanges)
        *//* Update post end *//*

        *//* Upvote references *//*
        val upvotedByRef = postRef.collection("upvotedBy")
            .document(currentUser.id)
        val upvotedBy = LikedBy(currentUser.id, currentUser.minify(), now)
        batch.set(upvotedByRef, upvotedBy)

        val upvotedPostRef = currentUserRef.collection("upvotedPosts").document(post.id)
        val upvotedPostDoc = mapOf(ID to post.id, CREATED_AT to now)
        batch.set(upvotedPostRef, upvotedPostDoc)

        if (post.isDownvote) {
            val downvotedByRef = postRef.collection("downvotedBy")
                .document(currentUser.id)
            batch.delete(downvotedByRef)

            val downvotedPostRef = currentUserRef.collection("downvotedPosts").document(post.id)
            batch.delete(downvotedPostRef)
        }
        *//* Upvote references end *//*

        *//* Current user changes *//*
        val currentUserChanges = mutableMapOf(
            "upvotedPostsCount" to FieldValue.increment(1),
            UPDATED_AT to now
        )

        if (post.isDownvote) {
            currentUserChanges["downvotedPostsCount"] = FieldValue.increment(-1)
        }

        batch.update(currentUserRef, currentUserChanges)
        *//* Current user changes end *//*

        batch.commit().addOnCompleteListener {

            TODO("Rank post system has not been implemented")
            *//*post.isUpvote = true
            post.upvoteCount += 1

            if (post.isDownvote) {
                post.downvoteCount -= 1
            }

            post.updatedAt = now
            onComplete(post, it)*//*
        }

    }*/

    fun likePost2(post: Post, onComplete: (Task<Void>) -> Unit) {
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val postRef = postsCollectionRef
            .document(post.id)

        post.isLiked = true
        post.likesCount += 1
        post.updatedAt = System.currentTimeMillis()

        val now = System.currentTimeMillis()

        val postChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(postRef, postChanges)

        val likedByRef = postRef.collection("likedBy").document(currentUser.id)
        batch.set(likedByRef, currentUser.minify())

        val likePostRef = currentUserRef.collection("likedPosts").document(post.id)
        batch.set(likePostRef, post.apply { updatedAt = now })

        val currentUserChanges =
            mapOf("likedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    /* fun likePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
         val currentUser = UserManager.currentUser

         val postRef = postsCollectionRef
             .document(post.id)

         val batch = db.batch()
         val userRef = usersCollectionRef
             .document(currentUser.id)

         batch.update(postRef, LIKES_COUNT, FieldValue.increment(1))
         batch.update(userRef, LIKED_POSTS, FieldValue.arrayUnion(post.id))

         batch.commit().addOnCompleteListener(onComplete)
     }

     fun downvotePost(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
         val batch = db.batch()

         val currentUser = UserManager.currentUser
         val postRef = postsCollectionRef
             .document(post.id)


         val now = System.currentTimeMillis()

         *//* Update post *//*
        val postChanges = mutableMapOf(
            "downvoteCount" to FieldValue.increment(1),
            UPDATED_AT to now
        )

        if (post.isUpvote) {
            postChanges["upvoteCount"] = FieldValue.increment(-1)
        }

        batch.update(postRef, postChanges)
        *//* Update post end *//*

        *//* Downvote references *//*
        val downvotedByRef = postRef.collection("downvotedBy").document(currentUser.id)
        batch.set(downvotedByRef, mapOf(ID to currentUser.id, "userMinimal" to currentUser.minify(), CREATED_AT to now))

        val downvotedPostRef = currentUserRef.collection("downvotedPosts").document(post.id)
        batch.set(downvotedPostRef, mapOf(ID to post.id, CREATED_AT to now))

        if (post.isUpvote) {
            val upvotedByRef = postRef.collection("upvotedBy")
                .document(currentUser.id)
            batch.delete(upvotedByRef)

            val upvotedPostRef = currentUserRef.collection("upvotedPosts").document(post.id)
            batch.delete(upvotedPostRef)
        }
        *//* Downvote references end *//*


        *//* Current user changes *//*
        val currentUserChanges = mutableMapOf("downvotedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)

        if (post.isUpvote) {
            currentUserChanges["upvotedPostsCount"] = FieldValue.increment(-1)
        }

        batch.update(currentUserRef, currentUserChanges)
        *//* Current user changes end *//*


        batch.commit().addOnCompleteListener {
            TODO("Rank post system has not been implemented yet")
           *//* post.isUpvote = false

            if (post.isUpvote) {
                post.upvoteCount -= 1
            }

            post.downvoteCount += 1

            post.updatedAt = now
            onComplete(post, it)*//*
        }

    }*/

    fun dislikePost2(post: Post, onComplete: (Task<Void>) -> Unit) {
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val postRef = postsCollectionRef
            .document(post.id)

        val now = System.currentTimeMillis()

        post.isLiked = false
        post.likesCount -= 1
        post.updatedAt = now

        val postChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(postRef, postChanges)

        val likedByRef = postRef.collection("likedBy").document(currentUser.id)
        batch.delete(likedByRef)

        val likePostRef = currentUserRef.collection("likedPosts").document(post.id)
        batch.delete(likePostRef)

        val currentUserChanges =
            mapOf("likedPostsCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*fun dislikePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val postRef = postsCollectionRef
            .document(post.id)

        val batch = db.batch()

        batch.update(postRef, LIKES_COUNT, FieldValue.increment(-1))
        batch.update(currentUserRef, LIKED_POSTS, FieldValue.arrayRemove(post.id))

        batch.commit().addOnCompleteListener(onComplete)
    }*/

    fun savePost(post: Post, onComplete: ((Task<Void>) -> Unit)? = null) {
        val batch = db.batch()
        val now = System.currentTimeMillis()

        val saveRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        batch.set(saveRef, post.apply { updatedAt = now })

        val currentUserChanges =
            mapOf("savedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        val task = batch.commit()

        if (onComplete != null) {
            task.addOnCompleteListener(onComplete)
        }
    }

    fun savePost2(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val batch = db.batch()
        val now = System.currentTimeMillis()

        val savedPostDocRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        val savedPostDoc = mapOf(ID to post.id, CREATED_AT to now)
        batch.set(savedPostDocRef, savedPostDoc)

        val currentUserChanges =
            mapOf("savedPostsCount" to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isSaved = true
            onComplete(post, it)
        }
    }

    /* fun savePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {

         val batch = db.batch()
         batch.update(currentUserRef, SAVED_POSTS, FieldValue.arrayUnion(post.id))
         val savedPostRef = currentUserRef.collection(SAVED_POSTS).document(post.id)

         batch.set(savedPostRef, post)
         batch.commit().addOnCompleteListener(onComplete)
     }*/

    fun unsavePost(post: Post, onComplete: ((Task<Void>) -> Unit)? = null) {
        val batch = db.batch()
        val now = System.currentTimeMillis()

        val saveRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        batch.delete(saveRef)

        val currentUserChanges =
            mapOf("savedPostsCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        val task = batch.commit()

        if (onComplete != null) {
            task.addOnCompleteListener(onComplete)
        }
    }

    fun undoSavePost(post: Post, onComplete: (newPost: Post, task: Task<Void>) -> Unit) {
        val batch = db.batch()
        val now = System.currentTimeMillis()

        val savedPostDocRef = currentUserRef.collection(SAVED_POSTS).document(post.id)
        batch.delete(savedPostDocRef)

        val currentUserChanges =
            mapOf("savedPostsCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener {
            post.isSaved = false
            onComplete(post, it)
        }
    }

/*
    fun unSavePost(post: Post, onComplete: (task: Task<Void>) -> Unit) {

        val batch = db.batch()

        batch.update(currentUserRef, SAVED_POSTS, FieldValue.arrayRemove(post.id))

        batch.delete(currentUserRef.collection(SAVED_POSTS).document(post.id))

        batch.commit().addOnCompleteListener(onComplete)
    }
*/


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
        val batch = db.batch()
        val currentUser = UserManager.currentUser

        val postRequestRef = postRequestsCollectionRef.document()
        val requestId = postRequestRef.id

        val postRef = postsCollectionRef.document(post.id)
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
        val batch = db.batch()

        val postRequestRef = postRequestsCollectionRef.document(postRequest.requestId)
        val postRef = postsCollectionRef.document(postRequest.postId)

        val userChanges = mapOf(
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.requestId)
        )

        val postChanges = mapOf(
            REQUESTS to FieldValue.arrayRemove(postRequest.requestId)
        )

        val requestNotificationRef =
            usersCollectionRef.document(postRequest.senderId).collection(
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
        val batch = db.batch()

        val senderRef = usersCollectionRef.document(postRequest.senderId)
        val postRequestRef = postRequestsCollectionRef.document(postRequest.requestId)

        val senderChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayUnion(postRequest.postId),
            COLLABORATIONS_COUNT to FieldValue.increment(1),
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.postId),
            CHAT_CHANNELS to FieldValue.arrayUnion(post.chatChannel)
        )

        val currentUserNotificationRef =
            usersCollectionRef.document(postRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(postRequest.notificationId)

        batch.addNewUserToPost(
            post.id,
            post.chatChannel,
            SourceInfo(postRequest.requestId, null, postRequest.senderId),
            UserManager.currentUser.token
        )
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
        val postRef = postsCollectionRef.document(postRequest.postId)
        val senderRef = usersCollectionRef.document(postRequest.senderId)
        val requestRef = postRequestsCollectionRef.document(postRequest.requestId)

        val postChanges = mapOf(REQUESTS to FieldValue.arrayRemove(postRequest.requestId))

        val senderChanges = mapOf(
            POST_REQUESTS to FieldValue.arrayRemove(postRequest.postId)
        )
        val batch = db.batch()
        val requestNotificationRef =
            usersCollectionRef.document(postRequest.receiverId).collection(
                NOTIFICATIONS
            ).document(postRequest.notificationId)

        batch.update(postRef, postChanges)
            .update(senderRef, senderChanges)
            .delete(requestRef)
            .delete(requestNotificationRef)
            .commit()
            .addOnCompleteListener(onComplete)

    }

    fun likeUser2(userMinimal: UserMinimal, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val batch = db.batch()

        val now = System.currentTimeMillis()

        val otherUserReference = usersCollectionRef.document(userMinimal.userId)

        /* creating new doc for liked user */
        val likedUserDocRef = currentUserRef.collection(LIKED_USERS).document(userMinimal.userId)
        batch.set(likedUserDocRef, userMinimal)

        /* updating other user */
        val otherUserChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
        batch.update(otherUserReference, otherUserChanges)

        val likedByDocRef = otherUserReference.collection("likedBy").document(currentUser.id)
        batch.set(likedByDocRef, currentUser.minify())

        /* updating current user */
        val currentUserChanges = mapOf(
            "likedUsersCount" to FieldValue.increment(1),
            UPDATED_AT to System.currentTimeMillis()
        )

        batch.update(currentUserRef, currentUserChanges)

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun dislikeUser2(userId: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val batch = db.batch()

        val now = System.currentTimeMillis()

        val otherUserReference = usersCollectionRef.document(userId)

        /* deleting liked user */
        val newLikedUserDocRef = currentUserRef.collection(LIKED_USERS).document(userId)
        batch.delete(newLikedUserDocRef)

        /* updating other user */
        val otherUserChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(otherUserReference, otherUserChanges)

        val likedByDocRef = otherUserReference.collection("likedBy").document(currentUser.id)
        batch.delete(likedByDocRef)

        /* updating current user */
        val currentUserChanges =
            mapOf("likedUsersCount" to FieldValue.increment(-1), UPDATED_AT to now)
        batch.update(currentUserRef, currentUserChanges)

        batch.commit()
            .addOnCompleteListener(onComplete)
    }


    fun sendComment(
        comment: Comment,
        parentCommentChannelId: String?,
        onComplete: (task: Task<Void>) -> Unit
    ) {

        val batch = db.batch()

        val commentCollectionRef = commentChannelCollectionRef

        val commentRef = commentCollectionRef.document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        val newCommentChannel = CommentChannel(
            randomId(),
            comment.commentId,
            comment.postId,
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

        val parentCommentChannelChanges = mapOf(
            LAST_COMMENT to comment,
            COMMENTS_COUNT to FieldValue.increment(1),
            UPDATED_AT to System.currentTimeMillis()
        )
        batch.update(parentCommentChannelRef, parentCommentChannelChanges)

        val postRef = postsCollectionRef.document(comment.postId)
        val postChanges = mapOf(
            COMMENTS_COUNT to FieldValue.increment(1),
            UPDATED_AT to System.currentTimeMillis()
        )
        batch.update(postRef, postChanges)

        // update the parent comment replies count
        if (comment.commentLevel.toInt() != 0) {
            val parentRef = commentCollectionRef
                .document(parentCommentChannelId!!).collection(COMMENTS)
                .document(comment.parentId)

            batch.update(
                parentRef,
                mapOf(
                    REPLIES_COUNT to FieldValue.increment(1),
                    UPDATED_AT to System.currentTimeMillis()
                )
            )
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun likeComment2(
        comment: Comment,
        onComplete: (newComment: Comment, task: Task<Transaction>) -> Unit
    ) {

        val currentUser = UserManager.currentUser

        val now = System.currentTimeMillis()
        comment.isLiked = true
        comment.likesCount += 1
        comment.updatedAt = now

        Log.d(TAG, "likeComment2: Liking comment with id: ${comment.commentId}")

        db.runTransaction {
            val commentChannelRef = commentChannelCollectionRef
                .document(comment.commentChannelId)
            val commentChannelSnapshot = it.get(commentChannelRef)
            val lastCommentId = commentChannelSnapshot.get("lastComment.commentId") as String?
            if (lastCommentId != null) {
                if (lastCommentId == comment.commentId) {
                    // update comment channel
                    val commentChannelChanges = mapOf(LAST_COMMENT to comment, UPDATED_AT to now)
                    it.update(commentChannelRef, commentChannelChanges)
                } else {
                    Log.e(
                        TAG,
                        "likeComment2: Ignoring changes to comment channel because comment is not the last comment"
                    )
                }
            } else {
                Log.e(TAG, "likeComment2: There is no last comment, this must be the first comment")
            }

            val commentRef = db
                .collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
                .collection(COMMENTS)
                .document(comment.commentId)

            val commentChanges = mapOf(LIKES_COUNT to FieldValue.increment(1), UPDATED_AT to now)
            it.update(commentRef, commentChanges)

            val likedCommentRef =
                currentUserRef.collection(LIKED_COMMENTS).document(comment.commentId)
            val likedCommentDoc = mapOf(
                ID to comment.commentId,
                COMMENT_CHANNEL_ID to comment.commentChannelId,
                CREATED_AT to now
            )
            it.set(likedCommentRef, likedCommentDoc)

            val likedByRef = commentRef.collection("likedBy").document(currentUser.id)
            val likedByDoc = LikedBy(currentUser.id, currentUser.minify(), now)
            it.set(likedByRef, likedByDoc)

            val currentUserChanges =
                mapOf("likedCommentsCount" to FieldValue.increment(1), UPDATED_AT to now)
            it.update(currentUserRef, currentUserChanges)
        }.addOnCompleteListener {

            if (!it.isSuccessful) {
                Log.e(
                    TAG,
                    "likeComment2: Liking comment unsuccessful ${it.exception?.localizedMessage}"
                )
            }

            onComplete(comment, it)
        }
    }

    fun dislikeComment2(
        comment: Comment,
        onComplete: (newComment: Comment, task: Task<Transaction>) -> Unit
    ) {

        val currentUser = UserManager.currentUser
        val now = System.currentTimeMillis()
        comment.isLiked = false
        comment.likesCount -= 1
        comment.updatedAt = now

        db.runTransaction {
            val commentChannelRef = commentChannelCollectionRef
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

            val commentRef = db
                .collection(COMMENT_CHANNELS)
                .document(comment.commentChannelId)
                .collection(COMMENTS)
                .document(comment.commentId)

            val commentChanges = mapOf(LIKES_COUNT to FieldValue.increment(-1), UPDATED_AT to now)
            it.update(commentRef, commentChanges)

            val likedCommentRef =
                currentUserRef.collection(LIKED_COMMENTS).document(comment.commentId)
            it.delete(likedCommentRef)

            val likedByRef = commentRef.collection("likedBy").document(currentUser.id)
            it.delete(likedByRef)

            val currentUserChanges =
                mapOf("likedCommentsCount" to FieldValue.increment(-1), UPDATED_AT to now)
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

            val batch = db.batch()

            val chatChannelRef = chatChannelCollectionRef.document(chatChannelId)

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
                isDownloaded = false,
                isSavedToFiles = false,
                isCurrentUserMessage = true
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

    /*fun getMessageVideoThumbnail(message: Message, onComplete: (task: Task<Uri>) -> Unit){
        val path = "videos/messages/${message.messageId}/thumb_${message.content}.jpg"
        storageRef.child(path).downloadUrl
            .addOnCompleteListener(onComplete)
    }*/

    suspend fun sendMessages(
        messages: List<Message>,
        isForward: Boolean = false
    ): Result<List<Message>> {

        val chatChannelId = messages[0].chatChannelId
        val chatChannelRef = chatChannelCollectionRef.document(chatChannelId)

        suspend fun getDownloadableUrl(list: List<Message>) {
            val uploadItems = list.map { message ->
                val name = message.content + message.metadata!!.ext
                UploadItem(
                    message.metadata!!.url.toUri(),
                    "${message.type}s/messages/${message.messageId}",
                    name
                )
            }

            val uploadedItemUrls = uploadItems2(uploadItems)

            for (i in list.indices) {
                list[i].metadata!!.url = uploadedItemUrls[i]
            }
        }

        fun concatWithTextMessage(list: List<Message>, textMessage: Message): List<Message> {
            val newList = mutableListOf<Message>()
            newList.addAll(list)
            newList += textMessage
            return newList
        }

        suspend fun send(updatedList: List<Message>): Result<List<Message>> {
            return try {
                val batch = db.batch()

                for (message in updatedList) {
                    val ref = chatChannelRef.collection(MESSAGES).document(message.messageId)
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

        return if (messages.size > 1) {
            val lastMessage = messages.last()
            if (lastMessage.type == text) {
                val mediaMessages = messages.subList(0, messages.size - 2)

                if (!isForward)
                    getDownloadableUrl(mediaMessages)


                val updatedList = concatWithTextMessage(mediaMessages, lastMessage)
                send(updatedList)
            } else {
                if (!isForward)
                    getDownloadableUrl(messages)

                send(messages)
            }
        } else {
            val singleMessage = messages[0]

            if (singleMessage.type == text) {
                send(listOf(singleMessage))
            } else {
                val mediaMessages = listOf(singleMessage)

                if (!isForward)
                    getDownloadableUrl(mediaMessages)

                send(mediaMessages)
            }
        }

    }


    /* // size of the list must be 2 if text included and 1 if only media
     suspend fun sendMessagesSimultaneously(
         chatChannelId: String,
         listOfMessages: List<Message>
     ): Result<List<Message>> {

         TODO("Need to implement media item list")

         val lastMessage = listOfMessages.last()
         val isLastMessageTextMsg = lastMessage.type == text

         val chatChannelRef = chatChannelCollectionRef.document(chatChannelId)

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
             val batch = db.batch()

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

     }*/

    // name must be with extension
    fun downloadMessageMedia(
        destinationFile: File,
        name: String,
        message: Message,
        onComplete: (task: Task<FileDownloadTask.TaskSnapshot>) -> Unit
    ) {
        val path = "${message.type}s/messages/${message.messageId}/$name"
        downloadMedia(destinationFile, path, onComplete)
    }

    private fun downloadMedia(
        dest: File,
        path: String,
        onComplete: (task: Task<FileDownloadTask.TaskSnapshot>) -> Unit
    ) {
        val objRef = storageRef.child(path)
        objRef.getFile(dest).addOnCompleteListener(onComplete)
    }

    fun deleteComment(comment: Comment, onComplete: (task: Task<Void>) -> Unit) {
        commentChannelCollectionRef
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

                    val ref = chatChannelCollectionRef
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
                        isSavedToFiles = false,
                        isCurrentUserMessage = true
                    )

                    newMessages.add(newMessage)

                    Log.d(TAG, newMessages.map { it1 -> it1.content }.toString())

                    batch.set(ref, newMessage)

                    if (i == messages.size - 1) {
                        batch.update(
                            chatChannelCollectionRef.document(it.chatChannelId),
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
        val batch = db.batch()

        for (message in messages) {
            if (message.senderId != currentUserId) {
                val messageRef = chatChannelCollectionRef
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

        val batch = db.batch()
        val ref = chatChannelCollectionRef
            .document(message.chatChannelId)
            .collection(MESSAGES)
            .document(message.messageId)

        val newList = message.readList.addItemToList(currentUserId)
        message.readList = newList

        if (chatChannel.lastMessage?.messageId == message.messageId) {
            batch.update(
                chatChannelCollectionRef
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

        val toBeUploadedImages =
            uploadItems("images/reports/${report.id}", names, report.snapshots.map { it.toUri() })
        report.snapshots = toBeUploadedImages.map { it.toString() }

        db.collection(REPORTS)
            .document(report.id)
            .set(report)
            .addOnCompleteListener(onComplete)

    }

    fun sendFeedback(feedback: Feedback, onComplete: (task: Task<Void>) -> Unit) {
        db.collection(FEEDBACKS)
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
        chatChannelCollectionRef.document(chatChannelId)
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
        chatChannelCollectionRef.document(chatChannelId)
            .update(ADMINISTRATORS, FieldValue.arrayRemove(userId))
            .addOnCompleteListener(onComplete)
    }


    fun removeUserFromPost(
        user: User,
        postId: String,
        chatChannelId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        val contributorRef = db
            .collection(USERS)
            .document(user.id)

        val batch = db.batch()

        val userChanges = mapOf(
            COLLABORATIONS to FieldValue.arrayRemove(postId),
            COLLABORATIONS_COUNT to FieldValue.increment(-1),
            CHAT_CHANNELS to FieldValue.arrayRemove(chatChannelId)
        )

        val chatChannelRef = db
            .collection(CHAT_CHANNELS)
            .document(chatChannelId)

        val channelChanges = mapOf(
            CONTRIBUTORS to FieldValue.arrayRemove(user.id),
            ADMINISTRATORS to FieldValue.arrayRemove(user.id),
            CONTRIBUTORS_COUNT to FieldValue.increment(-1),
            TOKENS to FieldValue.arrayRemove(user.token),
            UPDATED_AT to System.currentTimeMillis()
        )

        val postRef = db
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

        val batch = db.batch()
        val ref = usersCollectionRef
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
            .update(usersCollectionRef.document(post.creator.userId), currentUserChanges)
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

        val batch = db.batch()

        val inviteRef = usersCollectionRef
            .document(invite.receiverId)
            .collection(INVITES)
            .document(invite.id)


        val inviteNotificationRef =
            usersCollectionRef.document(invite.receiverId)
                .collection(NOTIFICATIONS)
                .document(invite.notificationId)

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
        usersCollectionRef
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
        postRequestsCollectionRef
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

    fun getPost(postId: String, onComplete: (Post?) -> Unit) {
        postsCollectionRef.document(postId).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val post = it.toObject(Post::class.java)!!
                    processPost(post)
                    onComplete(post)
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener {
                Log.e(TAG, "getPost: ${it.localizedMessage}")
                onComplete(null)
            }
    }

    /**
     * To get a post from firestore, not intended for use in main thread
     * @see getPost use this method to get post from firestore in main thread
     * @param postId The id of the post to fetch
     * @return Result of post data
     * */
    /*suspend fun getPost(postId: String): Result<Post>? {
        val ref = postsCollectionRef.document(postId)
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
    }*/

    /**
     * @param postId Id of the post
     * @param onComplete Callback function for getting the post
     * */
    /*fun getPost(postId: String, onComplete: (Result<Post>?) -> Unit) {
        val ref = postsCollectionRef.document(postId)
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
    }*/

    /**
     * @param userId The id of the user to fetch
     * @return Result<User> Contains either the user or an exception due to failure.
     * This method is not intended to run on main thread
     * */
   /* suspend fun getUser(userId: String): Result<User>? {
        val ref = usersCollectionRef.document(userId)
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
    }*/

    private val onFailureListener = OnFailureListener {
        Log.e(TAG, "Something went wrong: ${it.localizedMessage}")
    }

    fun getUser(userId: String, onComplete: (User?) -> Unit) {
        usersCollectionRef.document(userId).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    onComplete(processUsers(it.toObject(User::class.java)!!).firstOrNull())
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener(onFailureListener)
    }

    /**
     * @param userId The id of the user to fetch
     * @param onComplete Callback function containing result of user data
     *
     * To get the user from firebase using userId [For use in main thread]
     * */
    /*fun getUser(userId: String, onComplete: (Result<User>?) -> Unit) {
        val ref = usersCollectionRef.document(userId)
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
    }*/

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


        val postRef = postsCollectionRef.document(post.id)
        val archiveRef = currentUserRef.collection("archivedPosts").document(post.id)
        val commentChannelRef = commentChannelCollectionRef.document(post.commentChannel)
        val chatChannelRef = chatChannelCollectionRef.document(post.chatChannel)

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
            val ref = usersCollectionRef.document(contributor)
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
        val now = System.currentTimeMillis()

        val postRef = postsCollectionRef.document(post.id)
        val archivedPostRef = currentUserRef.collection(ARCHIVED_POSTS).document(post.id)
        val commentChannelRef = commentChannelCollectionRef.document(post.commentChannel)
        val chatChannelRef = chatChannelCollectionRef.document(post.chatChannel)

        val batch = db.batch()


        val postChanges = mapOf(ARCHIVED to false, UPDATED_AT to now, EXPIRED_AT to (-1).toLong())

        batch.update(postRef, postChanges)

        batch.delete(archivedPostRef)

        // updating all contributors that they aren't actually contributing now
        val contributorsListExcludingCurrentUser = post.contributors.filter { it != currentUserId }
        for (contributor in contributorsListExcludingCurrentUser) {
            val ref = usersCollectionRef.document(contributor)
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
            val ref = commentChannelCollectionRef.document(commentId)
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
            val query = db.collectionGroup(COMMENTS)
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
        val ref = usersCollectionRef.document(UserManager.currentUserId)
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

        val requestRef = postRequestsCollectionRef.document(postRequest.requestId)
        val requestSenderRef = usersCollectionRef.document(postRequest.senderId)
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

        val post = postInvite.post
        val currentUserId = currentUser.id
        val currentUserRegistrationToken = currentUser.token

        val batch = db.batch()
        val postInviteSenderReference = usersCollectionRef.document(postInvite.senderId)

        val postInviteReference =
            usersCollectionRef.document(currentUserId).collection(INVITES)
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
            usersCollectionRef.document(postInvite.receiverId)
                .collection(NOTIFICATIONS)
                .document(postInvite.notificationId)

        batch.addNewUserToPost(
            post.id,
            post.chatChannel,
            SourceInfo(null, postInvite.id, currentUserId),
            currentUserRegistrationToken
        )
            .updateCurrentUser(currentUserChanges)
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
        val postReference = postsCollectionRef.document(postId)
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
            chatChannelCollectionRef.document(chatChannelId)
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
        changes: Map<String, Any>
    ): WriteBatch {
        return this.update(currentUserRef, changes)
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
        val batch = db.batch()
        val postInviteRef = currentUserRef.collection(INVITES).document(postInvite.id)
        val senderRef = usersCollectionRef.document(postInvite.senderId)

        val inviteNotificationRef =
            usersCollectionRef.document(postInvite.receiverId).collection(
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

        val batch = db.batch()

        val senderRef = usersCollectionRef.document(postInvite.senderId)
        val postInviteRef = usersCollectionRef
            .document(postInvite.receiverId)
            .collection(INVITES)
            .document(postInvite.id)

        batch.update(senderRef, mapOf(POST_INVITES to FieldValue.arrayRemove(postInvite.id)))
        batch.delete(postInviteRef)

        batch.commit().addOnCompleteListener(onComplete)
    }

    /*fun getRandomInterests(onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        interestsCollectionRef
            .document(INTERESTS_COLLECTION)
            .get()
            .addOnCompleteListener(onComplete)
    }*/

    fun uploadUser(user: User, onComplete: (task: Task<Void>) -> Unit) {
        usersCollectionRef
            .document(user.id)
            .set(user, SetOptions.merge())
            .addOnCompleteListener(onComplete)
    }

    fun sendNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        usersCollectionRef.document(notification.receiverId)
            .collection(NOTIFICATIONS).document(notification.id)
            .set(notification)
            .addOnCompleteListener(onComplete)
    }

    fun sendNotificationToChannel(
        notification: Notification,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        chatChannelCollectionRef.document(notification.receiverId)
            .collection(NOTIFICATIONS)
            .document(notification.id)
            .set(notification)
            .addOnCompleteListener(onComplete)

    }


    fun checkIfNotificationExistsByContent(
        oldNotification: Notification,
        onComplete: (exists: Boolean, error: Exception?) -> Unit
    ) {
        usersCollectionRef
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
        usersCollectionRef.document(receiverId)
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
        postsCollectionRef
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

    fun updateNotification(
        receiverId: String,
        notificationId: String,
        changes: Map<String, Any?>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        usersCollectionRef
            .document(receiverId)
            .collection(NOTIFICATIONS)
            .document(notificationId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateNotification(notification: Notification, onComplete: (task: Task<Void>) -> Unit) {
        usersCollectionRef
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
        val batch = db.batch()

        // because this function can only be invoked from archive fragment
        val postRef = usersCollectionRef
            .document(post.creator.userId)
            .collection(ARCHIVE)
            .document(post.id)

        val chatChannelRef = chatChannelCollectionRef
            .document(post.chatChannel)

        val commentChannelRef = commentChannelCollectionRef
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
            val ref = usersCollectionRef.document(id)
            batch.update(ref, COLLABORATIONS, FieldValue.arrayRemove(post.id))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun postDeletePost(
        postRequests: List<PostRequest>,
        onComplete: (task: Task<Void>) -> Unit
    ) {

        val batch = db.batch()

        for (request in postRequests) {
            val requestRef = postRequestsCollectionRef.document(request.requestId)
            batch.delete(requestRef)

            val senderRef = usersCollectionRef.document(request.senderId)
            batch.update(senderRef, POST_REQUESTS, FieldValue.arrayRemove(request.requestId))
        }

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun getAllRequestsForPost(
        post: Post,
        onComplete: (task: Result<List<PostRequest>>?) -> Unit
    ) {
        postRequestsCollectionRef
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

    private fun logError(e: Exception) {
        Log.e(TAG, "logError: ${e.localizedMessage}")
    }

    fun getChatChannel(chatChannelId: String, onComplete: (ChatChannel?) -> Unit) {
        chatChannelCollectionRef.document(chatChannelId)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    onComplete(it.toObject(ChatChannel::class.java))
                } else {
                    onComplete(null)
                }
            }.addOnFailureListener {
                logError(it)
                onComplete(null)
            }
    }


    fun removeSubscriptions(onComplete: (task: Task<Void>) -> Unit) {
        usersCollectionRef
            .document(UserManager.currentUserId)
            .collection(PURCHASES)
            .get()
            .addOnSuccessListener {
                val batch = db.batch()
                if (!it.isEmpty) {
                    for (d in it) {
                        batch.delete(d.reference)
                    }
                }
                batch.commit().addOnCompleteListener(onComplete)
            }
    }

    fun updatePost(
        postId: String,
        changes: Map<String, Any?>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        postsCollectionRef.document(postId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    private fun getMediaStringForPost(mediaItems: List<MediaItem>): String {
        var s = ""

        for (item in mediaItems) {
            s += if (item.type == video) {
                "1"
            } else {
                "0"
            }
        }

        return s
    }

    suspend fun updatePost(
        post: Post,
        mediaList: List<MediaItem>,
        thumbnailUrl: String,
        onComplete: (newPost: Post, task: Task<Void>) -> Unit
    ) {
        val alreadyUploadedItems = post.mediaList.map { it.toUri() }.filter {
            it.scheme == "https"
        }.map { it.toString() }

        val nn = mediaList.filter { it.url.toUri().scheme == "content" }

        val itemsToBeUploaded = mutableListOf<UploadItem>()
        for (item in nn) {
            val uploadItem = if (item.type == video) {
                UploadItem(item.url.toUri(), "videos/posts/${post.id}", randomId() + item.ext)
            } else {
                UploadItem(item.url.toUri(), "images/posts/${post.id}", randomId() + item.ext)
            }

            itemsToBeUploaded.add(uploadItem)
        }

        var hasThumbnailChanged = false
        if (thumbnailUrl.toUri().scheme == "content") {
            hasThumbnailChanged = true
            itemsToBeUploaded.add(
                UploadItem(
                    thumbnailUrl.toUri(),
                    "images/posts/${post.id}/thumbnails",
                    randomId() + ".jpg"
                )
            )
        }

        val newlyUploadedItems = uploadItems2(itemsToBeUploaded)

        val downloadUrls = mutableListOf<String>()
        downloadUrls.addAll(alreadyUploadedItems)
        downloadUrls.addAll(newlyUploadedItems)

        if (hasThumbnailChanged) {
            post.thumbnail = downloadUrls.last()
        }

        post.mediaList = downloadUrls.subList(0, downloadUrls.size - 1)
        post.mediaString = getMediaStringForPost(mediaList)

        val postChanges = mutableMapOf(
            NAME to post.name,
            CONTENT to post.content,
            "mediaList" to downloadUrls,
            "mediaString" to post.mediaString,
            TAGS to post.tags,
            LOCATION to post.location,
            SOURCES to post.sources,
            "thumbnail" to post.thumbnail,
            UPDATED_AT to System.currentTimeMillis()
        )

        val channelChanges = mapOf(
            POST_TITLE to post.name,
            POST_IMAGE to post.thumbnail,
            UPDATED_AT to System.currentTimeMillis()
        )


        val batch = db.batch()

        val chatChannelRef = chatChannelCollectionRef.document(post.chatChannel)
        val postRef = postsCollectionRef.document(post.id)

        batch.update(chatChannelRef, channelChanges)
            .update(postRef, postChanges)
            .commit()
            .addOnCompleteListener {
                onComplete(post, it)
            }
    }

    fun sendRegistrationTokenToChatChannels(token: String, onComplete: (task: Task<Void>) -> Unit) {
        val currentUser = UserManager.currentUser

        val batch = db.batch()

        for (channel in currentUser.chatChannels) {
            val ref = chatChannelCollectionRef.document(channel)
            val changes = mapOf(
                TOKENS to FieldValue.arrayUnion(token)
            )
            batch.update(ref, changes)
        }

        batch.commit()
            .addOnCompleteListener(onComplete)
    }

    fun getCurrentUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        usersCollectionRef.document(userId)
            .get()
            .addOnCompleteListener(onComplete)

    }

    fun getContributors(channel: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        usersCollectionRef
            .whereArrayContains(CHAT_CHANNELS, channel)
            .get()
            .addOnCompleteListener(onComplete)
    }

    suspend fun getContributors(channel: String): Result<List<User>> {
        return try {
            val task = usersCollectionRef
                .whereArrayContains(CHAT_CHANNELS, channel)
                .get()

            val result = task.await()

            val contributors = result.toObjects(User::class.java)
            Result.Success(contributors)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun updateChatChannel(
        channelId: String,
        changes: Map<String, Any?>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        chatChannelCollectionRef
            .document(channelId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    fun updateComment(
        updatedComment: Comment,
        changes: Map<String, Any?>,
        onComplete: (newComment: Comment, Task<Transaction>) -> Unit
    ) {

        val commentChannelRef =
            commentChannelCollectionRef.document(updatedComment.commentChannelId)
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

    fun updatePostRequest(
        requestId: String,
        changes: Map<String, Any?>,
        onComplete: (Task<Void>) -> Unit
    ) {
        postRequestsCollectionRef.document(requestId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }

    /*fun updateMessage(chatChannelId: String, messageId: String, changes: Map<String, Any?>, onComplete: (Task<Void>) -> Unit) {
        chatChannelCollectionRef
            .document(chatChannelId)
            .collection(MESSAGES)
            .document(messageId)
            .update(changes)
            .addOnCompleteListener(onComplete)
    }*/

    fun updatePostInvite(
        receiverId: String,
        inviteId: String,
        changes: Map<String, Any?>,
        onUpdate: (Task<Void>) -> Unit
    ) {
        usersCollectionRef.document(receiverId)
            .collection(INVITES)
            .document(inviteId)
            .update(changes)
            .addOnCompleteListener(onUpdate)
    }

    fun getNotification(
        userId: String,
        notificationId: String,
        onComplete: (Result<Notification>?) -> Unit
    ) {
        usersCollectionRef.document(userId)
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

    fun getMessage(
        chatChannelId: String,
        messageId: String,
        onComplete: (Result<Message>?) -> Unit
    ) {
        chatChannelCollectionRef.document(chatChannelId)
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
        commentChannelCollectionRef
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
        interestsCollectionRef.document(interestItem.itemId)
            .set(interestItem)
            .addOnCompleteListener(onComplete)
    }

    /*fun checkForBlockedUser(userId: String, onCheck: (isBlocked: Boolean?) -> Unit) {
        usersCollectionRef
            .document(UserManager.currentUserId)
            .collection("blockedUsers")
            .document(userId)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onCheck(it.result.exists())
                } else {
                    onCheck(null)
                }
            }
    }
*/


    fun unblockUser(user: User, onComplete: (task: Task<Void>) -> Unit) {

        val batch = db.batch()
        val now = System.currentTimeMillis()

        batch.update(
            currentUserRef,
            mapOf("blockedUsers" to FieldValue.arrayRemove(user.id), UPDATED_AT to now)
        )

        val otherUserRef = usersCollectionRef.document(user.id)
        batch.update(
            otherUserRef,
            mapOf(
                "blockedBy" to FieldValue.arrayRemove(UserManager.currentUserId),
                UPDATED_AT to now
            )
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun blockUser(user: User, onComplete: (task: Task<Void>) -> Unit) {

        val batch = db.batch()
        val now = System.currentTimeMillis()
        batch.update(
            currentUserRef,
            mapOf("blockedUsers" to FieldValue.arrayUnion(user.id), UPDATED_AT to now)
        )

        val otherUserRef = usersCollectionRef.document(user.id)
        batch.update(
            otherUserRef,
            mapOf(
                "blockedBy" to FieldValue.arrayUnion(UserManager.currentUserId),
                UPDATED_AT to now
            )
        )

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun checkIfPostAssociatedWithBlockedUser(
        postMinimal2: PostMinimal2,
        onCheck: (isBlocked: Boolean?) -> Unit
    ) {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        getPost(postMinimal2.objectID) { post ->
            post?.let {
                onCheck(
                    ((blockedUsers.contains(post.creator.userId) || blockedUsers.intersect(post.contributors)
                        .isNotEmpty()) || (blockedBy.contains(post.creator.userId) || blockedBy.intersect(
                        post.contributors
                    ).isNotEmpty()))
                )
            }
        }
    }

    fun addNewRankedRule(rankRule: RankedRule, onComplete: (task: Task<Void>) -> Unit) {
        rankedRulesCollectionRef
            .document(rankRule.id)
            .set(rankRule)
            .addOnCompleteListener(onComplete)
    }

    fun getRankedRules(onComplete: (Result<List<RankedRule>>) -> Unit) {
        rankedRulesCollectionRef
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.isEmpty) {
                        onComplete(Result.Error(Exception("No ranked rules found")))
                    } else {
                        val rankedRules = it.result.toObjects(RankedRule::class.java)
                        onComplete(Result.Success(rankedRules))
                    }

                } else {
                    it.exception?.let { it1 ->
                        onComplete(Result.Error(it1))
                    }
                }
            }
    }

    fun getCompetitions(onComplete: (result: Result<List<Competition>>) -> Unit) {
        competitionsCollectionRef
            .whereEqualTo("currentStatus", "open")
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    onComplete(Result.Success(emptyList()))
                } else {
                    onComplete(Result.Success(it.toObjects(Competition::class.java)))
                }
            }.addOnFailureListener {
                onComplete(Result.Error(it))
            }
    }

    suspend fun getItems(key: String?, size: Int): Result<List<Post>> {
        return try {
            val xx = db.collection(POSTS)
                .whereEqualTo(ARCHIVED, false)

            val getPostsTask = if (key != null) {
                val getLastPostTask = db.collection(POSTS).document(key).get()
                val lastPostSnapshot = getLastPostTask.await()

                xx.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .startAfter(lastPostSnapshot)
                    .limit(size.toLong())
                    .get()
            } else {
                xx
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(size.toLong())
                    .get()
            }

            val postsQuerySnapshot = getPostsTask.await()
            val posts = postsQuerySnapshot.toObjects(Post::class.java)

            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun getUserSync(id: String): User? {
        val task = usersCollectionRef.document(id).get()
        val res = task.await()
        val user = res.toObject(User::class.java)
        return if (user != null) {
            processUser(user)
            user
        } else {
            null
        }
    }

    suspend fun getPostSync(id: String): Post? {
        val task = postsCollectionRef.document(id).get()
        val res = task.await()
        val post = res.toObject(Post::class.java)
        return if (post != null) {
            processPost(post)
            post
        } else {
            null
        }
    }

    fun createChannel(user: User, onComplete: (ChatChannel?) -> Unit) {
        val chatChannel = ChatChannel.newInstance(user)
        val ref = chatChannelCollectionRef.document(chatChannel.chatChannelId)
        ref.set(chatChannel)
            .addOnSuccessListener {
                onComplete(chatChannel)
            }.addOnFailureListener{
                Log.e(TAG, "createChannel: ${it.localizedMessage}")
                onComplete(null)
            }
    }

    fun authorizeChat(chatChannel: ChatChannel, onComplete: (Task<Void>) -> Unit) {
        val batch = db.batch()

        batch.update(chatChannelCollectionRef.document(chatChannel.chatChannelId), "authorized", true)
        val user1 = chatChannel.data1!!
        val user2 = chatChannel.data2!!

        batch.update(usersCollectionRef.document(user1.userId), CHAT_CHANNELS, FieldValue.arrayUnion(chatChannel.chatChannelId))
        batch.update(usersCollectionRef.document(user2.userId), CHAT_CHANNELS, FieldValue.arrayUnion(chatChannel.chatChannelId))

        usersCollectionRef.document(user2.userId)

        batch.commit().addOnCompleteListener(onComplete)
    }

    fun deleteTempPrivateChat(chatChannel: ChatChannel, onComplete: (task: Task<Void>) -> Unit) {

        val batch = db.batch()

        val channelRef = chatChannelCollectionRef.document(chatChannel.chatChannelId)
        batch.delete(channelRef)

        val data1 = chatChannel.data1
        if (data1 != null) {
            val user1Ref = usersCollectionRef.document(data1.userId)
            batch.update(user1Ref, CHAT_CHANNELS, FieldValue.arrayRemove(chatChannel.chatChannelId))
        }

        val data2 = chatChannel.data2
        if (data2 != null) {
            val user2Ref = usersCollectionRef.document(data2.userId)
            batch.update(user2Ref, CHAT_CHANNELS, FieldValue.arrayRemove(chatChannel.chatChannelId))
        }

        batch.commit().addOnCompleteListener(onComplete)

    }


    /*fun setSecondLastCommentAsLastComment(lastComment: Comment, onComplete: (Result<Comment>) -> Unit) {
        commentChannelCollectionRef
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

                        commentChannelCollectionRef
                            .document(lastComment.commentChannelId)
                            .update(commentChannelChanges)
                            .addOnSuccessListener {
                                onComplete(Result.Success(secondLastComment))
                            }.addOnFailureListener { it1 ->
                                onComplete(Result.Error(it1))
                            }
                    } else {
                        // there is no second comment
                        db.collection(POSTS)
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