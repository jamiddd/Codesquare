package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.UserMinimal2

class UserMinimalComparator: DiffUtil.ItemCallback<UserMinimal2>() {
    override fun areItemsTheSame(oldItem: UserMinimal2, newItem: UserMinimal2): Boolean {
        return oldItem.objectID == newItem.objectID
    }

    override fun areContentsTheSame(oldItem: UserMinimal2, newItem: UserMinimal2): Boolean {
        return oldItem == newItem
    }
}