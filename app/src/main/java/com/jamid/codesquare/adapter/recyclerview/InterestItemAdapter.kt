package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.adapter.comparators.InterestItemComparator
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.listeners.InterestItemClickListener

class InterestItemAdapter(private val interestItemClickListener: InterestItemClickListener): PagingDataAdapter<InterestItem, InterestItemViewHolder>(InterestItemComparator()) {
    override fun onBindViewHolder(holder: InterestItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestItemViewHolder {
        return InterestItemViewHolder.newInstance(parent, interestItemClickListener)
    }
}