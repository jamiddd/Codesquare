package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.LikedByComparator
import com.jamid.codesquare.data.LikedBy

class LikedByAdapter: PagingDataAdapter<LikedBy, UserViewHolder>(LikedByComparator()) {
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {

        getItem(position)?.let {
            holder.bind(it.userMinimal.toUser())
/*
            FireUtility.getUser(it.id) { it1 ->
                val result = it1 ?: return@getUser
                when (result) {
                    is Result.Error -> {
                        Log.e(TAG, "onBindViewHolder: Couldn't get user")
                    }
                    is Result.Success -> {
                        holder.bind(result.data)
                    }
                }
            }*/
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.newInstance(parent, R.layout.user_vertical_item)
    }

    companion object {
        private const val TAG = "PostLikedByAdapter"
    }
}