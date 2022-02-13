package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.SearchQuery

class PreviousQueryComparator: DiffUtil.ItemCallback<SearchQuery>() {
    override fun areItemsTheSame(oldItem: SearchQuery, newItem: SearchQuery): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SearchQuery, newItem: SearchQuery): Boolean {
        return oldItem == newItem
    }
}