package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.paging.ExperimentalPagingApi
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ImageSelectType.*
import com.jamid.codesquare.data.User
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@ExperimentalPagingApi
abstract class LauncherActivity : LocationAwareActivity(){

    var loadingDialog: AlertDialog? = null
    var imageSelectType = IMAGE_PROFILE

    private val imagesDir: File by lazy {
        getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }

    companion object {
        private const val TAG = "LauncherActivity"
    }

    val viewModel: MainViewModel by viewModels()


    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreate()
    }

    @SuppressLint("VisibleForTests")
    open fun onCreate() {}
*/



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
                viewModel.setGoogleSignInError(0)
            }
        } else {
            viewModel.setGoogleSignInError(1)
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


    @Throws(IOException::class)
    private fun compressAndSaveLocally(images: List<Uri>): List<Uri> {
        val compressedImages = mutableListOf<Uri>()

        for (image in images) {
            try {
                val inputStream = contentResolver.openInputStream(image)
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                val mWidth = imageBitmap.width.toFloat()
                val mHeight = imageBitmap.height.toFloat()

                val fWidth = minOf(mWidth, 600f).toInt()
                val fHeight = ((mHeight / mWidth) * fWidth).toInt()

                val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, fWidth, fHeight, true)
                val file = File(imagesDir, "${randomId()}.jpg")

                file.createNewFile()

                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

                val data = byteArrayOutputStream.toByteArray()

                val stream = FileOutputStream(file)
                stream.write(data)
                stream.flush()
                stream.close()

                val uri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, file)

                compressedImages.add(uri)
            } catch (e: Exception) {
                toast(e.localizedMessage!!)
            }
        }

       return compressedImages
    }

    val sil = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        var images: List<Uri> = emptyList()
        when (it.resultCode) {
            Activity.RESULT_OK -> {
                val data = it.data ?: return@registerForActivityResult
                val clipData = data.clipData

                when (imageSelectType) {
                    IMAGE_CHAT -> {
                        try {
                            if (clipData != null) {
                                images = getImagesFromClipData(clipData).toMutableList()
                                images = compressAndSaveLocally(images)
                            } else {
                                val singleImage = data.data
                                if (singleImage != null) {
                                    images = listOf(singleImage)
                                    images = compressAndSaveLocally(images)
                                }
                            }

                            if (images.size > 10) {
                                toast("Select only up to 10 images at once.")
                                return@registerForActivityResult
                            }

                            if (images.isEmpty())
                                return@registerForActivityResult

                            viewModel.setChatUploadImages(images)
                        } catch (e: Exception) {
                            toast(e.localizedMessage!!)
                        }

                    }
                    IMAGE_PROFILE -> {
                        val singleImage = data.data

                        if (singleImage != null) {
                            viewModel.setCurrentImage(singleImage)
                            val options = CropImageOptions().apply {
                                fixAspectRatio = true
                                aspectRatioX = 1
                                aspectRatioY = 1
                                cropShape = CropImageView.CropShape.OVAL
                                outputRequestHeight = 100
                                outputRequestWidth = 100
                            }
                            findNavController(R.id.nav_host_fragment).navigate(R.id.cropFragment2, bundleOf("image" to singleImage.toString(), "cropOptions" to options))
                        }
                    }
                    IMAGE_POST -> {
                        val currentPost = viewModel.currentPost.value
                        if (currentPost != null) {
                            val isPostImagesEmpty = currentPost.images.isNullOrEmpty()
                            try {
                                if (clipData != null) {
                                    images = getImagesFromClipData(clipData)
                                    images = compressAndSaveLocally(images)

                                    if (images.size > 10) {
                                        toast("Select only up to 10 images")
                                        return@registerForActivityResult
                                    }

                                    val formattedImages = images.map {it1 -> it1.toString() }

                                    if (isPostImagesEmpty) {
                                        viewModel.setCurrentPostImages(formattedImages)
                                    } else {

                                        if (currentPost.images.size + images.size > 10) {
                                            toast("Cannot add more images")
                                            return@registerForActivityResult
                                        }

                                        viewModel.addToExistingPostImages(formattedImages)
                                    }

                                } else {

                                    val singleImage = data.data
                                    if (singleImage != null) {
                                        images = listOf(singleImage)
                                        images = compressAndSaveLocally(images)

                                        val formattedImages = images.map {it1 -> it1.toString() }

                                        if (isPostImagesEmpty) {
                                            viewModel.setCurrentPostImages(formattedImages)
                                        } else {

                                            if (currentPost.images.size + images.size > 10) {
                                                toast("Cannot add more images")
                                                return@registerForActivityResult
                                            }

                                            viewModel.addToExistingPostImages(formattedImages)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, e.localizedMessage!!)
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
                    IMAGE_TEST -> {
                        viewModel.testImage.postValue(data.data)
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

    val selectChatDocumentsUploadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val documents = mutableListOf<Uri>()

            val clipData = it.data?.clipData

            if (clipData != null) {
                val d = getDocumentsFromClipData(clipData)
                val newD = checkDocumentsForSize(d)

                if (newD.size != d.size) {
                    if (newD.isEmpty()) {
                        if (d.size > 1) {
                            toast("Documents were not selected because some of the sizes were larger than 20 MB")
                        } else {
                            toast("Document was not selected because the size was larger than 20 MB")
                        }
                    } else {
                        toast("Some documents were not selected because the size was larger than 20 MB")
                    }
                }
                if (newD.isNotEmpty()) {
                    documents.addAll(newD)
                }
            } else {
                it.data?.data?.let { it1 ->
                    val d = checkDocumentsForSize(listOf(it1))

                    if (d.isEmpty()) {
                        toast("Document size must be less than or equal to 20 MB")
                    } else {
                        documents.addAll(d)
                    }
                }
            }

            if (documents.isEmpty())
                return@registerForActivityResult

            if (documents.size > 5) {
                toast("Select only up to 5 documents at once")
                return@registerForActivityResult
            }

            viewModel.setChatUploadDocuments(documents)

        }
    }

    private fun checkDocumentsForSize(documents: List<Uri>): List<Uri> {
        val goodDocuments = mutableListOf<Uri>()

        for (document in documents) {
            try {
                val cursor = contentResolver.query(document, null, null, null, null) ?: throw NullPointerException("Cursor is null")
                cursor.moveToFirst()
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val size = (cursor.getLong(sizeIndex))
                cursor.close()

                // if document size is greater than 20 mb
                if (size / (1024 * 1024) > 20) {
                    Log.d(TAG, "checkDocumentsForSize: ${size/(1024*1024)}")
                    continue
                } else {
                    Log.d(TAG, "checkDocumentsForSize: ${size/(1024*1024)}")
                    goodDocuments.add(document)
                }
            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            }

        }

        return goodDocuments
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
                            val localUser = User.newUser(user.uid, user.displayName!!, user.email!!, user.photoUrl)

                            FireUtility.uploadUser(localUser) { it2 ->
                                if (it2.isSuccessful) {
                                    UserManager.updateUser(localUser)
                                    viewModel.insertCurrentUser(localUser)
                                } else {
                                    viewModel.setCurrentError(it2.exception)
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