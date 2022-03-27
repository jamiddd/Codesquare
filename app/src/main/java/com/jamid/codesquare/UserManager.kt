package com.jamid.codesquare

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import kotlinx.coroutines.delay

object UserManager {

    private val currentUserData = MutableLiveData<User>().apply { value = null }
    val currentUserLive: LiveData<User> = currentUserData
    lateinit var currentUser: User
    val errors = MutableLiveData<Exception>().apply { value = null }
    lateinit var currentUserId: String

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

    private val authStateData = MutableLiveData<Boolean>()

    /**
     * The state of the user.
     * 1. AuthState = null; // not initialized
     * 2. AuthState = true; // the user is logged in, and there is user data available for use in current user variable
     * 3. AuthState = false; // the user is logged out and hence there is no user data available
     * */
    val authState: LiveData<Boolean> = authStateData


    var isInitialized = false
    private var isSignedIn = false
    var isEmailVerified = false

    fun logOut(context: Context, onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Logging out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->
                Firebase.auth.signOut()
                setAuthStateForceful(false)
                onPositive()
            }.setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
            }
            .show()
    }

    fun updateUser(newUser: User) {
        isInitialized = true
        currentUserId = newUser.id
        currentUser = newUser
        isSignedIn = true
        authStateData.postValue(true)
        currentUserData.postValue(newUser)
    }

    private fun getCurrentUser(onComplete: ((Result<User>?) -> Unit)? = null) {
        if (::currentUserId.isInitialized) {
            FireUtility.getUser(currentUserId) {
                when (it) {
                    is Result.Error -> errors.postValue(it.exception)
                    is Result.Success -> {
                        updateUser(it.data)
                    }
                    null -> Log.d(TAG, "Document doesn't exist.")
                }
                if (onComplete != null) {
                    onComplete(it)
                }
            }
        }
    }

    private fun setAuthStateForceful(isSignedIn: Boolean) {
        authStateData.postValue(isSignedIn)
        this.isSignedIn = isSignedIn
        if (!isSignedIn) {
            isEmailVerified = false
            isInitialized = false
            currentUserData.postValue(null)
        }
    }

    // only after firebase is initialized
    fun initialize() {
        Firebase.auth.addAuthStateListener {
            val firebaseUser = it.currentUser
            if (firebaseUser != null) {
                isEmailVerified = firebaseUser.isEmailVerified || testEmails.contains(firebaseUser.email)
                currentUserId = firebaseUser.uid
                getCurrentUser()
            } else {
                Log.d(TAG, "The user is not signed in.")
                authStateData.postValue(false)
                currentUserData.postValue(null)
                currentUser = User()
                isSignedIn = false
            }
        }
    }

    private const val TAG = "UserManager"

}