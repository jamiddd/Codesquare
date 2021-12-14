package com.jamid.codesquare.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentImageCropperBinding
import com.theartofdev.edmodo.cropper.CropImageOptions
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCropperFragment: Fragment() {

    private lateinit var binding: FragmentImageCropperBinding
    private var options: CropImageOptions? = null
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.image_cropper_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.crop_image_done -> {
                val bitmap = binding.mainCropView.croppedImage

                val externalDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val name = "temp_" + System.currentTimeMillis().toString() + ".jpg"
                val file = File(externalDir, name)
                if (file.createNewFile()) {
                    if (bitmap != null) {
                        val byteArrayOutputStream = ByteArrayOutputStream()

                        runBlocking {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100 , byteArrayOutputStream)
                        }

                        val ba = byteArrayOutputStream.toByteArray()
                        val fos = FileOutputStream(file)
                        fos.write(ba)
                        fos.flush()
                        fos.close()
                        byteArrayOutputStream.flush()

                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.jamid.codesquare.fileprovider",
                            file
                        )

                        viewModel.setCurrentImage(uri)

                        findNavController().navigateUp()

                    }
                } else {
                    viewModel.setCurrentError(Exception("Something went wrong while creating a file."))
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
        binding = FragmentImageCropperBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = arguments?.getParcelable("cropOptions")

        val image = arguments?.getString("image") ?: return


        if (options != null) {
            binding.mainCropView.apply {
                setAspectRatio(options!!.aspectRatioX, options!!.aspectRatioY)
                setFixedAspectRatio(options!!.fixAspectRatio)
                cropShape = options!!.cropShape
            }
        }

        binding.mainCropView.setImageUriAsync(image.toUri())

        binding.cropSquare.setOnClickListener {
            binding.mainCropView.cropShape = CropImageView.CropShape.RECTANGLE
            binding.mainCropView.setFixedAspectRatio(true)
            binding.mainCropView.setAspectRatio(1, 1)
        }

        binding.cropOval.setOnClickListener {
            binding.mainCropView.cropShape = CropImageView.CropShape.OVAL
            binding.mainCropView.setFixedAspectRatio(true)
            binding.mainCropView.setAspectRatio(1, 1)
        }

        binding.cropFree.setOnClickListener {
            binding.mainCropView.setFixedAspectRatio(false)
            binding.mainCropView.cropShape = CropImageView.CropShape.RECTANGLE
        }

    }

}