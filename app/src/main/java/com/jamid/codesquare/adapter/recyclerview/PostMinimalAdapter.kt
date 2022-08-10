package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.PostMinimalComparator
import com.jamid.codesquare.data.PostMinimal2

class PostMinimalAdapter: ListAdapter<PostMinimal2, PostMinimalViewHolder>(PostMinimalComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostMinimalViewHolder {
        return PostMinimalViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.post_mini_item, parent, false))
    }
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onBindViewHolder(holder: PostMinimalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

