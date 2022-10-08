package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.InterestItem

class InterestItemComparator : DiffUtil.ItemCallback<InterestItem>() {

    override fun areItemsTheSame(
        oldItem: InterestItem,
        newItem: InterestItem
    ): Boolean {
        return oldItem.itemId == newItem.itemId
    }

    override fun areContentsTheSame(
        oldItem: InterestItem,
        newItem: InterestItem
    ): Boolean {
        return oldItem == newItem
    }
}
