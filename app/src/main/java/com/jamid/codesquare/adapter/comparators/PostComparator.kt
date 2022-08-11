package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2

class PostComparator: DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post)
        = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post)
        = oldItem == newItem

    init {
        Log.d("Something", "Simple: ")
    }

}

class Post2Comparator: DiffUtil.ItemCallback<Post2>() {
    override fun areItemsTheSame(oldItem: Post2, newItem: Post2): Boolean {
        val x = when (oldItem) {
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
        Log.d(tag, "areItemsTheSame: $x")
        return x
    }

    init {
        Log.d("Something", "Simple: ")
    }

    override fun areContentsTheSame(oldItem: Post2, newItem: Post2): Boolean {
        val x = when (oldItem) {
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

        Log.d(tag, "areContentsTheSame: $x")
        return x
    }

    private val tag = "MyCheck"

}