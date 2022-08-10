package com.jamid.codesquare.listeners

import com.google.firebase.auth.FirebaseUser
// something simple
interface GoogleSignInListener {
    fun onSignedIn(user: FirebaseUser)
    fun onError(throwable: Throwable)
}