package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Post

class PostComparator: DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return when (oldItem) {
            is Post.Ad -> if (newItem is Post.Ad) {
                newItem.attachedProjectId == oldItem.attachedProjectId
            } else {
                return false
            }
            is Post.Project1 -> if (newItem is Post.Project1) {
                newItem.project.id == oldItem.project.id
            } else {
                return false
            }
        }
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return when (oldItem) {
            is Post.Ad -> if (newItem is Post.Ad) {
                newItem == oldItem
            } else {
                return false
            }
            is Post.Project1 -> if (newItem is Post.Project1) {
                newItem.project == oldItem.project
            } else {
                return false
            }
        }
    }

}