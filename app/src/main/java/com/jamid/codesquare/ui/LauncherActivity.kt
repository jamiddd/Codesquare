package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.User

@ExperimentalPagingApi
abstract class LauncherActivity : AppCompatActivity(){

    var loadingDialog: AlertDialog? = null

    companion object {
        private const val TAG = "LauncherActivity"
    }

    val viewModel: MainViewModel by viewModels()
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreate()
    }

    @SuppressLint("VisibleForTests")
    open fun onCreate() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val locationStateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        val isNetworkAvailable = viewModel.isNetworkAvailable.value
        if (isNetworkAvailable != null && isNetworkAvailable) {
            LocationProvider.updateData(fusedLocationProviderClient, state = it.resultCode == RESULT_OK)
        }
    }

    val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val isNetworkAvailable = viewModel.isNetworkAvailable.value
        if (isNetworkAvailable != null && isNetworkAvailable) {
            LocationProvider.updateData(fusedLocationProviderClient, permission = isGranted)
        }
    }

    val requestGoogleSingInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadingDialog?.dismiss()
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
        }
    }

    val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { it1 ->
                viewModel.setCurrentImage(it1)
//                val cropOption = CropImageOptions().apply {
//                    fixAspectRatio = true
//                    aspectRatioX = 1
//                    aspectRatioY = 1
//                    cropShape = CropImageView.CropShape.OVAL
//                }
//                findNavController(R.id.nav_host_fragment).navigate(R.id.action_editProfileFragment_to_imageCropperFragment, bundleOf("image" to it1.toString(), "cropOptions" to cropOption))
//                findNavController(R.id.nav_host_fragment).navigate(R.id.action_editProfileFragment_to_cropFragment2, bundleOf("image" to it1.toString()))
            }
        }
    }

    val selectImageLauncher1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { it1 ->
                viewModel.setCurrentImage(it1)
                /*val cropOption = CropImageOptions().apply {
                    fixAspectRatio = true
                    aspectRatioX = 1
                    aspectRatioY = 1
                    cropShape = CropImageView.CropShape.OVAL
                }*/
//                findNavController(R.id.nav_host_fragment).navigate(R.id.action_profileImageFragment_to_cropFragment2, bundleOf("image" to it1.toString()))
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