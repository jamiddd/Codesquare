package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.UserMinimal
import com.jamid.codesquare.data.UserMinimal2

class UserMinimalComparator2: DiffUtil.ItemCallback<UserMinimal2>() {
    override fun areItemsTheSame(oldItem: UserMinimal2, newItem: UserMinimal2): Boolean {
        return oldItem.objectID == newItem.objectID
    }

    init {
        Log.d("Something", "Simple: ")
    }

    override fun areContentsTheSame(oldItem: UserMinimal2, newItem: UserMinimal2): Boolean {
        return oldItem == newItem
    }

}

class UserMinimalComparator: DiffUtil.ItemCallback<UserMinimal>() {
    override fun areItemsTheSame(oldItem: UserMinimal, newItem: UserMinimal): Boolean {
        return oldItem.userId == newItem.userId
    }

    init {
        Log.d("Something", "Simple: ")
    }

    override fun areContentsTheSame(oldItem: UserMinimal, newItem: UserMinimal): Boolean {
        return oldItem == newItem
    }
}