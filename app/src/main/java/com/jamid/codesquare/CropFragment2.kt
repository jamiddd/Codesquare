package com.jamid.codesquare

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.canhub.cropper.CropImageOptions
import com.jamid.codesquare.databinding.FragmentCrop2Binding
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@ExperimentalPagingApi
class CropFragment2: Fragment() {

    private lateinit var binding: FragmentCrop2Binding
    private val viewModel: MainViewModel by activityViewModels()
    private var options: CropImageOptions? = null

    private val imagesDir: File by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw NullPointerException("Couldn't get images directory.") }

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    val file = File(imagesDir, "${randomId()}.jpg")

                    val bitmap = binding.cropView.getCroppedImage(options?.outputRequestWidth ?: 100, options?.outputRequestHeight ?: 100)
                    if (bitmap != null) {
                        if (file.createNewFile()) {
                            val byteArrayOutputStream = ByteArrayOutputStream()
                            val destinationUri = FileProvider.getUriForFile(requireContext(), "com.jamid.codesquare.fileprovider", file)

                            runBlocking {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                            }

                            val ba = byteArrayOutputStream.toByteArray()

                            val fos = FileOutputStream(file)
                            fos.write(ba)
                            fos.flush()
                            fos.close()
                            byteArrayOutputStream.flush()

                            viewModel.setCurrentImage(destinationUri)
                            findNavController().navigateUp()
                        } else {
                            throw Exception("Could not create file.")
                        }
                    }

                } catch (e: Exception) {
                    viewModel.setCurrentError(e)
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCrop2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = arguments?.getParcelable("cropOptions")
        val image = arguments?.getString("image")

        binding.cropView.apply {
            if (options != null) {
                setFixedAspectRatio(options!!.fixAspectRatio)
                setAspectRatio(options!!.aspectRatioX, options!!.aspectRatioY)
                cropShape = options!!.cropShape
            }
        }

        binding.cropView.setImageUriAsync(image?.toUri())

    }

}