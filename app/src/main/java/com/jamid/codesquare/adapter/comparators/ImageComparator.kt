package com.jamid.codesquare.adapter.comparators

import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil

class ImageComparator: DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem.toUri().lastPathSegment == newItem.toUri().lastPathSegment
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

}