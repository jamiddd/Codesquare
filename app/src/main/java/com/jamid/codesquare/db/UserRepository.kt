package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// something simple
class UserRepository(db: CollabDatabase, private val scope: CoroutineScope) {

    private val _isSignedIn = MutableLiveData<Boolean>().apply { value = false }
    var currentUser = User()

    private val testEmails = listOf(
        "richard.green@gmail.com",
        "julia.lowe@gmail.com",
        "ricky.carlson@gmail.com",
        "raymond.wright@gmail.com",
        "tyler.gibson@gmail.com",
        "frances.anderson@gmail.com",
        "patrick.graves@gmail.com",
        "amelia.reid@gmail.com"
    )

    private val userDao = db.userDao()

    val userVerificationResult = MutableLiveData<Result<Boolean>>()

    init {
        Firebase.auth.addAuthStateListener {
            val currentFirebaseUser = it.currentUser
            if (currentFirebaseUser == null) {
                // is not signed in
                _isSignedIn.postValue(false)
            } else {
                // is signed in
                _isSignedIn.postValue(true)

                FireUtility.updateUser2(mapOf("online" to true), false) { updateTask ->
                    if (updateTask.isComplete) {
                        FireUtility.getCurrentUser(currentFirebaseUser.uid) { currentUserDocumentSnapshotTask ->
                            if (currentUserDocumentSnapshotTask.isSuccessful) {
                                val currentUserDocumentSnapshot = currentUserDocumentSnapshotTask.result
                                if (currentUserDocumentSnapshot != null && currentUserDocumentSnapshot.exists()) {
                                    val currentUser = currentUserDocumentSnapshot.toObject(User::class.java)
                                    if (currentUser != null) {
                                        this.currentUser = currentUser
                                        insertUser(currentUser)
                                        addTokenListener()
                                    }
                                } else {
                                    Log.d(TAG, "OnInit: Either the current user document snapshot is null or doesn't exist.")
                                }
                            } else {
                                Log.e(
                                    TAG,
                                    "OnInit: ${currentUserDocumentSnapshotTask.exception?.localizedMessage}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addTokenListener() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                FireUtility.sendRegistrationTokenToServer(token) {
                    if (it.isSuccessful) {
                        if (token != null)
                            FireUtility.sendRegistrationTokenToChatChannels(token) { it1 ->
                                if (!it1.isSuccessful) {
                                    UserManager.errors.postValue(it1.exception)
                                } else {
                                    Log.d(TAG, "Updated all channels with registration token")
                                }
                            }
                    } else {
                        UserManager.errors.postValue(it.exception)
                    }
                }
            }
    }

    private fun insertUser(user: User) = scope.launch (Dispatchers.IO) {
        userDao.insert(processUser(user))
    }

   /* private fun insertUsers(users: List<User>) = scope.launch (Dispatchers.IO) {
        userDao.insert(processUsers(users))
    }*/

    suspend fun getUser(userId: String): User? {
        return userDao.getUser(userId)
    }

    /*private fun processUsers(users: List<User>): List<User> {
        val newList = mutableListOf<User>()
        for (user in users) {
            newList.add(processUser(user))
        }
        return newList
    }*/

    private fun processUser(user: User): User {
        user.isCurrentUser = currentUser.id == user.id
//        user.isLiked = currentUser.likedUsers.contains(user.id)
        return user
    }

    fun getContributors(chatChannelId: String, limit: Int = 6): LiveData<List<User>> {
        return userDao.getContributors("%${chatChannelId}%", limit)
    }

    suspend fun setListenerForEmailVerification(numberOfRetry: Int, secondsInBetween: Long) {
        for (i in 1..numberOfRetry) {
            delay(secondsInBetween * 1000)

            Log.d(
                TAG,
                "setListenerForEmailVerification: Process for email verification running after ${i * 5} seconds"
            )

            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                if (currentUser.isEmailVerified || testEmails.contains(currentUser.email)) {
                    // email verified
                    userVerificationResult.postValue(Result.Success(true))
                    return
                }

                val task = currentUser.reload()
                task.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val isEmailVerified = Firebase.auth.currentUser?.isEmailVerified == true
                        // check if verified
                        userVerificationResult.postValue(Result.Success(isEmailVerified))
                    } else {
                        it.exception?.let { it1 -> userVerificationResult.postValue(Result.Error(it1)) }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "UserRepository"
    }

}