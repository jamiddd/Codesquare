package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.codesquare.FrescoImageControllerListener

class ImageAdapter(private val onClick: (v: View, controllerListener: BaseControllerListener<ImageInfo>) -> Unit): ListAdapter<String, ImageViewHolder>(comparator){

    companion object {
        val comparator = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem.length == newItem.length
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder.newInstance(parent, onClick)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}