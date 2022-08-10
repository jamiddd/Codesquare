package com.jamid.codesquare

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.jamid.codesquare.databinding.FragmentCrop2Binding

class CropFragment2: BaseBottomFragment<FragmentCrop2Binding>() {

    var options: CropImageOptions = CropImageOptions().apply {
        cropShape = CropImageView.CropShape.RECTANGLE
        fixAspectRatio = false
        aspectRatioX = 4
        aspectRatioY = 3
        outputRequestWidth = 400
        outputRequestHeight = 300
    }
    var image: String = ""

    override fun onCreateBinding(inflater: LayoutInflater): FragmentCrop2Binding {
        return FragmentCrop2Binding.inflate(inflater)
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_cropper_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.crop_image_done -> {
                try {
                    val bitmap = binding.cropView.getCroppedImage(options?.outputRequestWidth ?: 100, options?.outputRequestHeight ?: 100)
                    if (bitmap != null) {
                        val file = convertBitmapToFile(bitmap)
                        if (file != null) {
                            val destinationUri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)
                            viewModel.setCurrentImage(destinationUri)
                            findNavController().navigateUp()
                        } else
                            throw NullPointerException("File could not be created")
                    }

                } catch (e: Exception) {
                    viewModel.setCurrentError(e)
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }*/


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cropView.apply {
            setFixedAspectRatio(options.fixAspectRatio)
            setAspectRatio(options.aspectRatioX, options.aspectRatioY)
            cropShape = options.cropShape
        }

        binding.cropToolbarComp.bottomSheetToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.cropToolbarComp.bottomSheetToolbar.title = "Crop"

        binding.cropToolbarComp.bottomSheetDoneBtn.text = "Done"
        binding.cropToolbarComp.bottomSheetDoneBtn.setOnClickListener {
            try {
                val bitmap = binding.cropView.getCroppedImage(options.outputRequestWidth,
                    options.outputRequestHeight
                )
                if (bitmap != null) {
                    val file = convertBitmapToFile(bitmap)
                    if (file != null) {
                        val destinationUri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)
                        viewModel.setCurrentImage(destinationUri)
                        dismiss()
                    } else
                        throw NullPointerException("File could not be created")
                }

            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            }
        }

        binding.cropView.setImageUriAsync(image.toUri())

        /*
        * This is where the result comes asynchronously.
        * If the image was set through Uri then the result will also come in uri
        * If the image was set through bitmap then the result will also be a bitmap
        *
        * */
        binding.cropView.setOnCropImageCompleteListener { _, result ->
            if (result.isSuccessful) {
                if (result.uriContent != null) {
                    Log.d(TAG, "onViewCreated: We have uriContent after success: ${result.uriContent}")
                } else {
                    Log.d(TAG, "onViewCreated: No uriContent after success")
                }
            } else {
                Log.d(TAG, "onViewCreated: CropImageComplete unsuccessful")
            }
        }


        /*
        * this is called right after onOverlayMoved before calling onCropOverlayReleased
        * */
        binding.cropView.setOnCropWindowChangedListener {
            Log.d(TAG, "onViewCreated: The crop window has changed")
        }


        /*
        * this is called multiple times as the box is being moved
        * it returns the current position of the box in a rect
        * */
        binding.cropView.setOnSetCropOverlayMovedListener {
            it?.let { it1 ->
                Log.d(TAG, "onViewCreated: OnCropOverlayMoved $it1")
            }
        }


        /*
        * this is called as soon as the finger is released after moving
        * */
        binding.cropView.setOnSetCropOverlayReleasedListener {
            it?.let {
                /*val file = File.createTempFile(randomId(), ".jpg")
                binding.cropView.croppedImageAsync(customOutputUri = file.toUri())*/
               /* getNestedDir(requireActivity().filesDir, "images/thumbnails")?.let { it1 ->
                    getFile(it1, randomId() + ".jpg")?.let { f ->
                    }
                }*/
            }
        }

       /* binding.cropView.setOnSetImageUriCompleteListener { view1, uri, exc ->
            if (exc != null) {
                Log.e(TAG, "onViewCreated: ${exc.localizedMessage}")
            } else {
                *//*
                * This happens when the image is set, the resultant uri is
                *  the Uri of the image from local dir
                * *//*
                Log.d(TAG, "onViewCreated: The resultant uri = $uri")
            }
        }*/


    }

    companion object {
        const val TAG = "CropFragment2"
    }

}