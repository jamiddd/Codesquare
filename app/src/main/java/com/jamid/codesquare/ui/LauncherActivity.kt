package com.jamid.codesquare.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.jamid.codesquare.*
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.Metadata
// something simple
abstract class LauncherActivity : LocationAwareActivity() {

    var loadingDialog: AlertDialog? = null
    /*var imageSelectType = IMAGE_PROFILE

    private val imagesDir: File by lazy {
        getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }*/

    companion object {
        private const val TAG = "LauncherActivity"
    }

    val viewModel: MainViewModel by viewModels()

    val fileSaverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val externalUri = it.data?.data
            viewModel.setExternallyCreatedDocumentUri(externalUri)
        } else {
            Log.d(TAG, "LOL")
        }
    }

    /*var writePermissionGranted = false*/

   /* val requestWriteStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        writePermissionGranted = it
    }*/

    val requestGoogleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
                googleSignInListener?.onError(e)
            }
        } else {
            googleSignInListener?.onError(Exception("Activity result was not OK"))
        }
    }


    var cameraPhotoUri: Uri? = null

    val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.setCameraImage(cameraPhotoUri)
        }
    }

    /*private fun getImagesFromClipData(clipData: ClipData): List<Uri> {
        return getItemsFromClipData(clipData)
    }

    private fun getDocumentsFromClipData(clipData: ClipData): List<Uri> {
        return getItemsFromClipData(clipData)
    }*/

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


    private fun getMetadataForFile(uri: Uri): Metadata? {
        val cursor = contentResolver.query(uri, null, null, null, null)

        return try {
            cursor?.moveToFirst()
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
            val name = cursor?.getString(nameIndex ?: 0) ?: throw NullPointerException("Name of $uri is null")

            val size = (cursor.getLong(sizeIndex ?: 0))
            cursor.close()
            val ext = "." + name.split('.').last()

            Metadata(size, name, uri.toString(), ext, 0, 0)
        } catch (e: Exception) {
            viewModel.setCurrentError(e)
            null
        }
    }

    /*fun compressVideo(item: Uri): Uri? {
        return try {
            TODO()
        } catch (e: Exception) {
            Log.e(TAG, "compressVideo: ${e.localizedMessage}")
            null
        }
    }*/

    /*private fun compressImage(item: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(item)
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
            FileProvider.getUriForFile(this, FILE_PROV_AUTH, file)
        } catch (e: Exception) {
            Log.e(TAG, "compressImage: ${e.localizedMessage}")
            null
        }
    }*/

   /* @Throws(IOException::class)
    private fun compressAndSaveLocally(items: List<Uri>): List<Uri> {
        val compressedItems = mutableListOf<Uri>()

        for (item in items) {
            try {

                val metadata = getMetadataForFile(item)
                if (metadata != null) {
                    if (metadata.ext != ".mp4") {
                        compressImage(item)?.let {
                            compressedItems.add(it)
                        }
                    } else {
                        // check if size is more than 15 MB, omit files larger than 15 MB
                        if (metadata.size > 15728640) {
                            //
                        } else {
                            compressedItems.add(item)
                        }
                    }
                } else {
                    throw NullPointerException("The metadata for the file couldn't be generated.")
                }
            } catch (e: Exception) {
                toast(e.localizedMessage!!)
            }
        }

       return compressedItems
    }*/

   /* val chatVideosLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val finalList = mutableListOf<Uri>()

        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data ?: return@registerForActivityResult
            val singleItem = data.data

            if (singleItem != null) {

                finalList.add(singleItem)

            *//*val metadata = getMetadataForFile(singleItem)

                if (metadata != null) {
                    val image = compressImage(singleItem)
                    if (image != null) {
                        finalList.add(MediaItem(image.toString(), "image", sizeInBytes = metadata.size, ext = metadata.ext))
                    } else {
                        Log.e(TAG, "ActivityResult: Couldn't get image after compressing")
                    }
                } else {
                    Log.e(TAG, "ActivityResult: Metadata of the $singleItem is null")
                }*//*

            } else {
                val clipData = data.clipData ?: return@registerForActivityResult

                val items = getItemsFromClipData(clipData)

                if (items.isNullOrEmpty()) {
                    return@registerForActivityResult
                }

                for (item in items) {
                    val metadata = getMetadataForFile(item)
                    if (metadata != null) {
                        if (metadata.size > VIDEO_SIZE_LIMIT) {
                            // notify that some files have not been selected
                            toast("Some of the video files have been omitted because the size of the video was larger than 15 MB.")
                        } else {
                            finalList.add(item)
                        }
                    } else {
                        Log.e(TAG, "ActivityResult: Metadata of the $item is null")
                    }
                }
            }
            viewModel.setChatUploadVideos(finalList)
        }
    }*/

    /*val sml = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data ?: return@registerForActivityResult
            val singleItem = data.data

            val finalList = mutableListOf<MediaItem>()

            if (singleItem != null) {
                val metadata = getMetadataForFile(singleItem)

                if (metadata != null) {
                    val mime = getMimeType(metadata.url.toUri())
                    if (mime != null) {

                        val type = if (mime.contains(video)) {
                            video
                        } else {
                            image
                        }

                        if (type == image) {
                            val img = compressImage(singleItem)
                            if (img != null) {
                                finalList.add(MediaItem(img.toString(), image, sizeInBytes = metadata.size, ext = metadata.ext))
                            } else {
                                Log.e(TAG, "Something went wrong while compressing image.")
                            }
                        } else {
                            if (metadata.size < VIDEO_SIZE_LIMIT) {
                                finalList.add(MediaItem(singleItem.toString(), video, sizeInBytes = metadata.size, ext = metadata.ext))
                            } else {
                                Log.e(TAG, "Video selection omitted because video size is too large.")
                            }
                        }
                    } else {
                        Log.e(TAG, "Mime couldn't be generated for the given file: $singleItem")
                    }
                } else {
                    Log.e(TAG, "ActivityResult: Metadata of the $singleItem is null")
                }
            } else {
                val clipData = data.clipData ?: return@registerForActivityResult

                val items = getItemsFromClipData(clipData)

                if (items.isNullOrEmpty()) {
                    return@registerForActivityResult
                }

                for (item in items) {
                    val metadata = getMetadataForFile(item)
                    if (metadata != null) {

                        val mime = getMimeType(metadata.url.toUri())

                        if (mime != null) {
                            val type = if (mime.contains(video)) {
                                video
                            } else {
                                image
                            }

                            if (type == image) {
                                val img = compressImage(item)
                                if (img != null) {
                                    finalList.add(MediaItem(img.toString(), metadata.name, image, mime, sizeInBytes = metadata.size, ext = metadata.ext))
                                } else {
                                    Log.e(TAG, "Something went wrong while compressing image.")
                                }
                            } else {
                                if (metadata.size < VIDEO_SIZE_LIMIT) {
                                    finalList.add(MediaItem(it.toString(), metadata.name, video, mime, sizeInBytes = metadata.size, ext = metadata.ext))
                                } else {
                                    Log.e(TAG, "Video selection omitted because video size is too large.")
                                }
                            }
                        } else {
                            Log.e(TAG, "Mime couldn't be generated for the given file: $item")
                        }
                    } else {
                        Log.e(TAG, "ActivityResult: Metadata of the $item is null")
                    }
                }
            }

            viewModel.setCreatePostMediaList(finalList)

        } else {
            Log.d(TAG, "ActivityResult : NOT_OK")
        }

    }*/

    /*val chatMediaLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        val items = mutableListOf<MediaItem>()
        if (!it.isNullOrEmpty()) {
            for (uri in it) {
                val metadata = getMetadataForFile(uri)
                if (metadata != null) {
                    items.add(MediaItem(uri.toString(), metadata.name, ext = metadata.ext, sizeInBytes = metadata.size))
                }
            }
            viewModel.setChatUploadMedia(items)
        }
    }*/

    /*val chatMediaLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {

            val items = mutableListOf<MediaItem>()

            val data = it.data
            if (data != null) {
                val single = data.data
                if (single != null) {
                    val metadata = getMetadataForFile(single)
                    if (metadata != null) {
                        if (metadata.size < VIDEO_SIZE_LIMIT) {
                            items.add(MediaItem(single.toString(), metadata.name, ext = metadata.ext, sizeInBytes = metadata.size))
                        } else {
                            toast("Some files are being omitted because the size is larger than 15 MB")
                        }
                    }
                } else {
                    val clipData = data.clipData
                    if (clipData != null) {
                        val clipItems = getItemsFromClipData(clipData)
                        for (item in clipItems) {
                            val metadata = getMetadataForFile(item)
                            if (metadata != null) {
                                if (metadata.size < VIDEO_SIZE_LIMIT) {
                                    items.add(MediaItem(item.toString(), metadata.name, ext = metadata.ext, sizeInBytes = metadata.size))
                                } else {
                                    toast("Some files are being omitted because the size is larger than 15 MB")
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Found nothing")
                    }
                }
            } else {
                Log.e(TAG, "Single item null")
            }

            viewModel.setChatUploadMedia(items)

        }
    }*/


    val gallerySelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {

            val items = mutableListOf<MediaItem>()

            val data = it.data
            if (data != null) {
                val single = data.data
                if (single != null) {
                    val metadata = getMetadataForFile(single)
                    val mime = getMimeType(single)
                    if (metadata != null && mime != null) {
                        val type = if (mime.contains(video)) {
                            video
                        } else {
                            image
                        }

                        if (metadata.size < VIDEO_SIZE_LIMIT) {
                            items.add(MediaItem(single.toString(), metadata.name, type, mime, metadata.size, metadata.ext))
                        } else {
                            toast("Some files are being omitted because the size is larger than 15 MB")
                        }
                    }
                } else {
                    val clipData = data.clipData
                    if (clipData != null) {
                        val clipItems = getItemsFromClipData(clipData)
                        for (item in clipItems) {
                            val metadata = getMetadataForFile(item)
                            val mime = getMimeType(item)
                            if (metadata != null && mime != null) {
                                val type = if (mime.contains(video)) {
                                    video
                                } else {
                                    image
                                }
                                if (metadata.size < VIDEO_SIZE_LIMIT) {
                                    items.add(MediaItem(item.toString(), metadata.name, type, mime, metadata.size, metadata.ext))
                                } else {
                                    toast("Some files are being omitted because the size is larger than 15 MB")
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Found nothing")
                    }
                }
            } else {
                Log.e(TAG, "Single item null")
            }

            viewModel.setPreUploadMediaItems(items)

        }
    }

    /*val sil = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
                       *//* val singleImage = data.data

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
                            findNavController(R.id.nav_host_fragment).navigate(R.id.cropFragment2, bundleOf(ARG_CROP_IMAGE to singleImage.toString(), ARG_CROP_OPTIONS to options))
                        }*//*
                        TODO("Whatever happened here ...??? Jeez")
                    }
                    IMAGE_POST -> {
                        val currentPost = viewModel.currentPost.value
                        if (currentPost != null) {
                            val isPostImagesEmpty = currentPost.mediaList.isNullOrEmpty()
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

                                        if (currentPost.mediaList.size + images.size > 10) {
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

                                            if (currentPost.mediaList.size + images.size > 10) {
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
    }*/

    /*val selectChatDocumentsUploadLauncher = registerForActivityResult(
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
    }*/

    /*private fun checkDocumentsForSize(documents: List<Uri>): List<Uri> {
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
    }*/

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FireUtility.signInWithGoogle(credential) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    googleSignInListener?.onSignedIn(user)
                } else {
                    googleSignInListener?.onError(Exception("Firebase user is null"))
                }
            } else {
                it.exception?.let { it1 -> googleSignInListener?.onError(it1) }
            }
        }
    }

    var currentRequest = ""

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            when (currentRequest) {
                Manifest.permission.READ_EXTERNAL_STORAGE -> viewModel.setReadPermission(true)
                Manifest.permission.ACCESS_FINE_LOCATION -> viewModel.setLocationPermission(true)
            }
        }
    }


}