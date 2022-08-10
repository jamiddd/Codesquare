package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.ImageComparator
import com.jamid.codesquare.listeners.ImageClickListener

class SmallImagesAdapter(private val imageClickListener: ImageClickListener): ListAdapter<String, SmallImageViewHolder>(ImageComparator()) {
    init {
        Log.d("Something", "Simple: ")
    }
    var shouldShowCloseBtn = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallImageViewHolder {
        return SmallImageViewHolder(imageClickListener, LayoutInflater.from(parent.context).inflate(R.layout.small_image_item, parent, false), this.shouldShowCloseBtn)
    }

    override fun onBindViewHolder(holder: SmallImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}