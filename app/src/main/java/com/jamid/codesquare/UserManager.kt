package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.MainActivity

object UserManager {

    private val currentUserData = MutableLiveData<User>().apply { value = null }
    val currentUser: LiveData<User> = currentUserData

    fun init(currentUserId: String) {
        addUserListener(currentUserId)
        addTokenListener()
    }

    private fun addTokenListener() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                FireUtility.sendRegistrationTokenToServer(token)
            })
    }

    private fun addUserListener(currentUserId: String) {
        Firebase.firestore.collection(USERS)
            .document(currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && value.exists()) {
                    val newUser = value.toObject(User::class.java)
                    if (newUser != null) {
                        currentUserData.postValue(newUser)
                    }
                }
            }
    }

    private const val TAG = "UserManager"
    private const val USERS = "users"

}