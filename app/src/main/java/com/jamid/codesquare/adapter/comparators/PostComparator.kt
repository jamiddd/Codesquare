package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2

class PostComparator: DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post)
        = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post)
        = oldItem == newItem

}

class Post2Comparator: DiffUtil.ItemCallback<Post2>() {
    override fun areItemsTheSame(oldItem: Post2, newItem: Post2): Boolean {
        return when (oldItem) {
            is Post2.Advertise -> {
                when (newItem) {
                    is Post2.Advertise -> oldItem.id == newItem.id
                    is Post2.Collab -> oldItem.id == newItem.post.id
                }
            }
            is Post2.Collab -> {
                when (newItem) {
                    is Post2.Advertise -> oldItem.post.id == newItem.id
                    is Post2.Collab -> oldItem.post.id == newItem.post.id
                }
            }
        }
    }

    override fun areContentsTheSame(oldItem: Post2, newItem: Post2): Boolean {
        return when (oldItem) {
            is Post2.Advertise -> {
                when (newItem) {
                    is Post2.Advertise -> oldItem.id == newItem.id
                    is Post2.Collab -> oldItem.id == newItem.post.id
                }
            }
            is Post2.Collab -> {
                when (newItem) {
                    is Post2.Advertise -> oldItem.post.id == newItem.id
                    is Post2.Collab -> oldItem.post.id == newItem.post.id
                }
            }
        }
    }

    private val tag = "MyCheck"

}