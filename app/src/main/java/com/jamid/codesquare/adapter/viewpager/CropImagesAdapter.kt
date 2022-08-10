package com.jamid.codesquare.adapter.viewpager

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.canhub.cropper.CropImageOptions
import com.jamid.codesquare.CropFragment2

class CropImagesAdapter(val activity: FragmentActivity): FragmentStateAdapter(activity) {
    init {
        Log.d("Something", "Simple: ")
    }
    val images = arrayListOf<String>()
    var defaultCropOption: CropImageOptions? = null

    fun updateList(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun createFragment(position: Int): Fragment {
        return CropFragment2().apply {
            image = images[position]
            defaultCropOption?.let {
                options = it
            }
        }
    }

}