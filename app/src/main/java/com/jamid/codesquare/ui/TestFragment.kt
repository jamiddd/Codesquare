package com.jamid.codesquare.ui

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentTestBinding
import com.jamid.codesquare.toast
import java.io.File
// something simple
class TestFragment: Fragment(R.layout.fragment_test) {

    private val binding: FragmentTestBinding by lazy { FragmentTestBinding.bind(requireView()) }
    private val viewModel: MainViewModel by activityViewModels()
    private val imagesDir: File by lazy {
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.uploadAndCompress.setOnClickListener {
            toast("Hello")
        }

        /*binding.uploadAndCompress.setOnClickListener {
            (activity as MainActivity).selectImage(ImageSelectType.IMAGE_TEST)
        }

        viewModel.testImage.observe(viewLifecycleOwner) {
            if (it != null) {

                try {
                    val inputStream = requireActivity().contentResolver.openInputStream(it)
                    val image = BitmapFactory.decodeStream(inputStream)
                    val mWidth = image.width.toFloat()
                    val mHeight = image.height.toFloat()

                    val fWidth = minOf(mWidth, 600f).toInt()
                    val fHeight = ((mHeight / mWidth) * fWidth).toInt()

                    val scaledBitmap = Bitmap.createScaledBitmap(image, fWidth, fHeight, true)
                    val file = File(imagesDir, "${randomId()}.jpg")

                    file.createNewFile()

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

                    val data = byteArrayOutputStream.toByteArray()

                    val stream = FileOutputStream(file)
                    stream.write(data)
                    stream.flush()
                    stream.close()

                    val uri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)

                    val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)

                    try {
                        cursor?.moveToFirst()
                        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
                            ?: throw NullPointerException("Something")
                        val size = (cursor.getLong(sizeIndex))
                        cursor.close()

                        val sizeText = getTextForSizeInBytes(size)
                        binding.testSizeText.text = sizeText

                    } catch (e: Exception) {
                        toast(e.localizedMessage!!)
                    }

                    binding.testImageView.setImageURI(uri)

                } catch (e: Exception) {
                    toast(e.localizedMessage!!)
                }

            }
        }*/

    }

}