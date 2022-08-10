package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.OneTimeProduct

class SubscriptionComparator: DiffUtil.ItemCallback<OneTimeProduct>() {
    override fun areItemsTheSame(oldItem: OneTimeProduct, newItem: OneTimeProduct): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: OneTimeProduct, newItem: OneTimeProduct): Boolean {
        return oldItem == newItem
    }

    init {
        Log.d("Something", "Simple: ")
    }

}