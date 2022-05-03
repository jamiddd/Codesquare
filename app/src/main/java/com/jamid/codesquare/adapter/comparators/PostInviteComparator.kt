package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.PostInvite

class PostInviteComparator: DiffUtil.ItemCallback<PostInvite>() {
    override fun areItemsTheSame(oldItem: PostInvite, newItem: PostInvite): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PostInvite, newItem: PostInvite): Boolean {
        return oldItem == newItem
    }
}