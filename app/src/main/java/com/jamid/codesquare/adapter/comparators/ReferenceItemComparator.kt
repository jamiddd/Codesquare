package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.ReferenceItem

class ReferenceItemComparator : DiffUtil.ItemCallback<ReferenceItem>() {
    override fun areItemsTheSame(oldItem: ReferenceItem, newItem: ReferenceItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ReferenceItem, newItem: ReferenceItem): Boolean {
        return oldItem == newItem
    }

}
