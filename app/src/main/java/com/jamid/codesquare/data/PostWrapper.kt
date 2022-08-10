package com.jamid.codesquare.data

import androidx.annotation.Keep
import androidx.recyclerview.widget.DiffUtil

@Keep
data class PostWrapper(
    val id: String,
    val post: Post,
    var isSelected: Boolean
) {

    companion object {

        val comparator = object: DiffUtil.ItemCallback<PostWrapper>() {
            override fun areItemsTheSame(oldItem: PostWrapper, newItem: PostWrapper): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PostWrapper, newItem: PostWrapper): Boolean {
                return oldItem == newItem
            }
        }

    }

}