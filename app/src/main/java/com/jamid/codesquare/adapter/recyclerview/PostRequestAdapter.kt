package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.adapter.comparators.PostRequestComparator
import com.jamid.codesquare.data.PostRequest

class PostRequestAdapter(private val imr: Boolean = false) : PagingDataAdapter<PostRequest, PostRequestViewHolder>(PostRequestComparator()) {
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onBindViewHolder(holder: PostRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostRequestViewHolder {
        return PostRequestViewHolder.newInstance(parent, imr)
    }

}