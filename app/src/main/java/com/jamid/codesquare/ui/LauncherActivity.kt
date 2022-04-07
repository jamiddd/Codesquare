package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
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
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ImageSelectType.*
import com.jamid.codesquare.data.User

@ExperimentalPagingApi
abstract class LauncherActivity : AppCompatActivity(){

    var loadingDialog: AlertDialog? = null
    var imageSelectType = IMAGE_PROFILE


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

    val requestGoogleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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


    private fun getImagesFromClipData(clipData: ClipData): List<Uri> {
        return getItemsFromClipData(clipData)
    }

    private fun getDocumentsFromClipData(clipData: ClipData): List<Uri> {
        return getItemsFromClipData(clipData)
    }

    private fun getItemsFromClipData(clipData: ClipData): List<Uri> {
        val items = mutableListOf<Uri>()
        val count = clipData.itemCount
        for (i in 0 until count) {
            val uri = clipData.getItemAt(i)?.uri
            uri?.let { item ->
                items.add(item)
            }
        }
        return items
    }

    val sil = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        var images: List<Uri> = emptyList()
        when (it.resultCode) {
            Activity.RESULT_OK -> {
                val data = it.data ?: return@registerForActivityResult
                val clipData = data.clipData

                when (imageSelectType) {
                    IMAGE_CHAT -> {

                        if (clipData != null) {
                            images = getImagesFromClipData(clipData).toMutableList()
                        } else {
                            val singleImage = data.data
                            if (singleImage != null) {
                                images = listOf(singleImage)
                            }
                        }

                        viewModel.setChatUploadImages(images)
                    }
                    IMAGE_PROFILE -> {
                        val singleImage = data.data

                        if (singleImage != null) {
//                            val fileData = getFileData(singleImage)
//
//                            // if the profile picture
//                            val sizeInMB: Float = fileData!!.size.toFloat() / (1024 * 1024)
//                            if (sizeInMB > 1f) {
//
//
//
//                                toast("The size of the image is larger than 1 MB. Please select an image of size lower than 1 MB.", Toast.LENGTH_LONG)
//                                return@registerForActivityResult
//                            }

                            viewModel.setCurrentImage(singleImage)
                            val options = CropImageOptions().apply {
                                fixAspectRatio = true
                                aspectRatioX = 1
                                aspectRatioY = 1
                                cropShape = CropImageView.CropShape.OVAL
                                outputRequestHeight = 100
                                outputRequestWidth = 100
                            }
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_editProfileFragment_to_cropFragment2, bundleOf("image" to singleImage.toString(), "cropOptions" to options))
                        }
                    }
                    IMAGE_PROJECT -> {

                        val isProjectImagesEmpty = viewModel.currentProject.value?.images?.isNullOrEmpty() == true

                        if (clipData != null) {
                            images = getImagesFromClipData(clipData)

                            val formattedImages = images.map {it1 -> it1.toString() }

                            if (isProjectImagesEmpty) {
                                viewModel.setCurrentProjectImages(formattedImages)
                            } else {
                                viewModel.addToExistingProjectImages(formattedImages)
                            }

                        } else {
                            val singleImage = data.data
                            if (singleImage != null) {

                                val formattedImages = listOf(singleImage.toString())

                                if (isProjectImagesEmpty) {
                                    viewModel.setCurrentProjectImages(formattedImages)
                                } else {
                                    viewModel.addToExistingProjectImages(formattedImages)
                                }

                            }
                        }
                    }
                    IMAGE_REPORT -> {

                        if (clipData != null) {
                            images = getImagesFromClipData(clipData)
                            viewModel.setReportUploadImages(images)
                        } else {
                            val singleImage = data.data
                            if (singleImage != null) {
                                viewModel.setChatUploadImages(listOf(singleImage))
                            }
                        }

                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Activity result was cancelled")
            }
            else -> {
                Log.d(TAG, "Something unexpected happened")
            }
        }
    }

//    data class FileData(val name: String, val size: Long, val ext: String)

    /*private fun getFileData(item: Uri) : FileData? {
        val cursor = contentResolver.query(item, null, null, null, null)
        return try {
            cursor?.moveToFirst()
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

            val name = cursor?.getString(nameIndex ?: 0) ?: throw NullPointerException("Name of $item is null")
            val size = (cursor.getLong(sizeIndex ?: 0))
            cursor.close()
            val ext = "." + name.split('.').last()
            FileData(name, size, ext)
        } catch (e: Exception) {
            Log.e(TAG, "getFileData: ${e.localizedMessage}")
            null
        }
    }*/

    val selectChatDocumentsUploadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val documents = mutableListOf<Uri>()

            val clipData = it.data?.clipData

            if (clipData != null) {
               documents.addAll(getDocumentsFromClipData(clipData))
            } else {
                it.data?.data?.let { it1 ->
                    documents.add(it1)
                }
            }

            viewModel.setChatUploadDocuments(documents)

        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FireUtility.signInWithGoogle(credential) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val ref = Firebase.firestore.collection(USERS)
                        .document(user.uid)

                    FireUtility.getDocument(ref) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val oldUser = it1.result.toObject(User::class.java)!!
                            UserManager.updateUser(oldUser)
                            viewModel.insertCurrentUser(oldUser)
                        } else {
                            val localUser = User.newUser(user.uid, user.displayName!!, user.email!!)

                            FireUtility.uploadDocument(ref, localUser) { it2 ->
                                if (it2.isSuccessful) {
                                    UserManager.updateUser(localUser)
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