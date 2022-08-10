package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.LikedBy

class LikedByComparator : DiffUtil.ItemCallback<LikedBy>() {

    init {
        Log.d("Something", "Simple: ")
    }

    override fun areItemsTheSame(oldItem: LikedBy, newItem: LikedBy): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LikedBy, newItem: LikedBy): Boolean {
        return oldItem == newItem
    }

}
