package com.jamid.codesquare

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.MessageDialogFragment

object UserManager {

    private val currentUserData = MutableLiveData<User>().apply { value = null }
    val currentUserLive: LiveData<User> = currentUserData
    var currentUser = User()
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


    var isEmailVerified = false

    fun logOut(context: Context, onPositive: () -> Unit) {
        val frag = MessageDialogFragment.builder("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->
                Firebase.auth.signOut()
                setAuthStateForceful(false)
                onPositive()
            }
            .setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
            }
            .build()

        frag.show((context as AppCompatActivity).supportFragmentManager, MessageDialogFragment.TAG)

    }

    fun updateUser(newUser: User) {
        currentUserId = newUser.id
        currentUser = newUser
        authStateData.postValue(true)
        currentUserData.postValue(newUser)
    }

    private fun getCurrentUser() {
        val mAuth = Firebase.auth
        val currentUser = mAuth.currentUser ?: return

        val uid = currentUser.uid
        FireUtility.getUser(uid) {
            val userResult = it ?: return@getUser
            when (userResult) {
                is Result.Error -> errors.postValue(userResult.exception)
                is Result.Success -> {
                    updateUser(userResult.data)
                }
            }
        }
    }

    private fun setAuthStateForceful(isSignedIn: Boolean) {
        authStateData.postValue(isSignedIn)
        if (!isSignedIn) {
            isEmailVerified = false
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
                authStateData.postValue(false)
                currentUserData.postValue(null)
                currentUser = User()
            }
        }
    }

    private const val TAG = "UserManager"

}