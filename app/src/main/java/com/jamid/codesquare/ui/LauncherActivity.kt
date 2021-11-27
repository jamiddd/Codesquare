package com.jamid.codesquare.ui

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.data.User
import com.jamid.codesquare.toast

abstract class LauncherActivity : AppCompatActivity(){

    companion object {
        private const val TAG = "LauncherActivity"
    }

    val viewModel: MainViewModel by viewModels()

    val requestGoogleSingInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                account?.idToken?.let { it1 ->
                    firebaseAuthWithGoogle(it1)
                }
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                viewModel.setCurrentError(e)
            }
        } else {
            toast("Activity result was not OK!")
        }
    }

    val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { it1 ->
                viewModel.setCurrentImage(it1)
            }
        }
    }

    val selectProjectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.setCurrentProjectImages(images.map {it1 -> it1.toString() })

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setCurrentProjectImages(listOf(it1.toString()))
                }
            }
        }
    }

    val selectChatDocumentsUploadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val documents = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { doc ->
                        documents.add(doc)
                    }
                }

                viewModel.setChatUploadDocuments(documents)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadDocuments(listOf(it1))
                }
            }
        }
    }

    val selectReportImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.setReportUploadImages(images)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadImages(listOf(it1))
                }
            }
        }
    }

    val selectChatImagesUploadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.setChatUploadImages(images)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadImages(listOf(it1))
                }
            }
        }
    }

    val selectMoreProjectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.addToExistingProjectImages(images.map {it1 -> it1.toString() })

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.addToExistingProjectImages(listOf(it1.toString()))
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FireUtility.signInWithGoogle(credential) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val ref = Firebase.firestore.collection("users")
                        .document(user.uid)

                    FireUtility.getDocument(ref) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val oldUser = it1.result.toObject(User::class.java)!!
                            viewModel.insertCurrentUser(oldUser)
                        } else {
                            val localUser = User.newUser(user.uid, user.displayName!!, user.email!!)
                            FireUtility.uploadDocument(ref, localUser) { it2 ->
                                if (it2.isSuccessful) {
                                    viewModel.insertCurrentUser(localUser)
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                    Firebase.auth.signOut()
                                }
                            }
                        }
                    }
                } else {
                    Firebase.auth.signOut()
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

}