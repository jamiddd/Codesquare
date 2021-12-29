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
import com.jamid.codesquare.databinding.FragmentCrop2Binding
//import com.steelkiwi.cropiwa.config.CropIwaSaveConfig
import java.io.File

@ExperimentalPagingApi
class CropFragment2: Fragment() {

    private lateinit var binding: FragmentCrop2Binding
    private val viewModel: MainViewModel by activityViewModels()
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

             /*   try {
                    val file = File(imagesDir, "${randomId()}.jpg")
                    val destinationUri = FileProvider.getUriForFile(requireContext(), "com.jamid.codesquare.fileprovider", file)
                    binding.cropView.crop(
                        CropIwaSaveConfig.Builder(destinationUri)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setQuality(80) //Hint for lossy compression formats
                            .build()
                    )

                } catch (e: Exception) {
                    viewModel.setCurrentError(e)
                }*/
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



       /* binding.cropView.configureOverlay()
            .setDynamicCrop(true)
            .apply()

        binding.cropView.configureOverlay()
            .setShouldDrawGrid(true)
            .apply()

        binding.cropView.configureImage()
            .setImageTranslationEnabled(true)
            .apply()

        binding.cropView.configureImage()
            .setScale(1f)
            .apply()

        binding.cropView.configureImage()
            .setImageScaleEnabled(true)
            .apply()

        val image = arguments?.getString("image") ?: return

        binding.cropView.setImageUri(image.toUri())

        binding.cropView.setCropSaveCompleteListener {
            viewModel.setCurrentImage(it)
            findNavController().navigateUp()
        }*/


    }

}