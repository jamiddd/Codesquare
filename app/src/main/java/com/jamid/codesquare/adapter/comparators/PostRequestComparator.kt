package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.PostRequest

class PostRequestComparator: DiffUtil.ItemCallback<PostRequest>() {
    override fun areItemsTheSame(
        oldItem: PostRequest,
        newItem: PostRequest
    ): Boolean {
        return oldItem.requestId == newItem.requestId
    }

    override fun areContentsTheSame(
        oldItem: PostRequest,
        newItem: PostRequest
    ): Boolean {
        return oldItem == newItem
    }

}