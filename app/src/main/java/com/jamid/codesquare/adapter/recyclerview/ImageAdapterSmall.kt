package com.jamid.codesquare.adapter.recyclerview

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class ImageAdapterSmall(private val onRemoveClick: (v: View) -> Unit): ListAdapter<Uri, ImageViewHolderSmall>(comparator) {

    companion object {

        val comparator = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolderSmall {
        return ImageViewHolderSmall.newInstance(parent, onRemoveClick)
    }

    override fun onBindViewHolder(holder: ImageViewHolderSmall, position: Int) {
        holder.bind(getItem(position))
    }

}