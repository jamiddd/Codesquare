package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.listeners.ImageClickListener

class ImageAdapter(private val imageClickListener: ImageClickListener? = null): ListAdapter<String, ImageViewHolder>(comparator){
    init {
        Log.d("Something", "Simple: ")
    }
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
        return ImageViewHolder.newInstance(parent, imageClickListener)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}