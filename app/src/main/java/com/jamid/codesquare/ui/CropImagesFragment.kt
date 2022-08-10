package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.adapter.viewpager.CropImagesAdapter
import com.jamid.codesquare.databinding.FragmentCropImagesBinding

/*
* 1. input - receives a list of images
* 2. process - all the images can be cropped
* 3. output - all images but cropped
* */
class CropImagesFragment: Fragment(){

    private val images = arrayListOf<String>()
    private lateinit var binding: FragmentCropImagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getStringArrayList(ARG_IMAGES)?.let {
            images.addAll(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCropImagesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cropImagesPager.adapter = CropImagesAdapter(requireActivity()).apply {
            updateList(images)
        }

        TabLayoutMediator(binding.imagePositionTabs, binding.cropImagesPager) { a, b ->
            //
        }.attach()


    }

    companion object {
        private const val TAG = "CropImagesFragment"
        const val ARG_IMAGES = "ARG_IMAGES"

        fun newInstance(images: ArrayList<String>)
            = CropImagesFragment()
            .apply {
                arguments = bundleOf()
            }

    }

}