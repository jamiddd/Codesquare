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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.jamid.codesquare.data.*
import kotlinx.coroutines.tasks.await

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
            "projects" to FieldValue.arrayUnion(project.id)
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

}