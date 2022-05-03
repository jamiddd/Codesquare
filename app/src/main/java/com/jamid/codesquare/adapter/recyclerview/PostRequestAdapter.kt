package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.PostRequest

class PostRequestAdapter(private val imr: Boolean = false) : PagingDataAdapter<PostRequest, PostRequestViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<PostRequest>() {
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
    }

    override fun onBindViewHolder(holder: PostRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostRequestViewHolder {
        return PostRequestViewHolder.newInstance(parent, imr)
    }

}