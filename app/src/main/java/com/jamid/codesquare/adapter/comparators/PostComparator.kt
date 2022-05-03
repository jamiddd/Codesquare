package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Post

class PostComparator: DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post)
        = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post)
        = oldItem == newItem
}