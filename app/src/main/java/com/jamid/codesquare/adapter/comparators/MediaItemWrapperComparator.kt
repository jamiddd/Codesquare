package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.MediaItemWrapper

class MediaItemWrapperComparator: DiffUtil.ItemCallback<MediaItemWrapper>() {

    override fun areItemsTheSame(oldItem: MediaItemWrapper, newItem: MediaItemWrapper): Boolean {
        return oldItem.mediaItem.url == newItem.mediaItem.url
    }

    override fun areContentsTheSame(oldItem: MediaItemWrapper, newItem: MediaItemWrapper): Boolean {
        return oldItem == newItem
    }
}
