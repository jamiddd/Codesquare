package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.PostMinimal2

class PostMinimalComparator: DiffUtil.ItemCallback<PostMinimal2>() {
    override fun areItemsTheSame(oldItem: PostMinimal2, newItem: PostMinimal2)
            = oldItem.objectID == newItem.objectID

    override fun areContentsTheSame(oldItem: PostMinimal2, newItem: PostMinimal2)
            = oldItem == newItem
}