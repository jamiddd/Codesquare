package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.MediaItem

class MediaItemComparator: DiffUtil.ItemCallback<MediaItem>() {

    init {
        Log.d("Something", "Simple: ")
    }

    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem == newItem
    }


}