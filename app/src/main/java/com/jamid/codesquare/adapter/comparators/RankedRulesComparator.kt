package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.RankedRule

class RankedRulesComparator: DiffUtil.ItemCallback<RankedRule>() {
    override fun areItemsTheSame(oldItem: RankedRule, newItem: RankedRule): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RankedRule, newItem: RankedRule): Boolean {
        return oldItem == newItem
    }

}