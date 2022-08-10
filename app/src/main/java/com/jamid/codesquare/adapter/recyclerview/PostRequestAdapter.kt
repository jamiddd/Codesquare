package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.adapter.comparators.PostRequestComparator
import com.jamid.codesquare.data.PostRequest

class PostRequestAdapter(private val imr: Boolean = false) : PagingDataAdapter<PostRequest, PostRequestViewHolder>(PostRequestComparator()) {

    override fun onBindViewHolder(holder: PostRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostRequestViewHolder {
        return PostRequestViewHolder.newInstance(parent, imr)
    }

}