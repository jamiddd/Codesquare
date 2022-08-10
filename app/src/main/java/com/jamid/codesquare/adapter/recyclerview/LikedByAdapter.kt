package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.UserMinimalComparator
import com.jamid.codesquare.data.UserMinimal

class LikedByAdapter: PagingDataAdapter<UserMinimal, UserViewHolder>(UserMinimalComparator()) {
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it.toUser())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.newInstance(parent, R.layout.user_vertical_item)
    }

    companion object {
        private const val TAG = "PostLikedByAdapter"
    }
}