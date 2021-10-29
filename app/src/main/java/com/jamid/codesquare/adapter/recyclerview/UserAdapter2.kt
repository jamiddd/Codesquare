package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.data.User

class UserAdapter2(private val administrators: List<String>): ListAdapter<User, UserSmallViewHolder>(comparator) {

    var isGrid = false

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSmallViewHolder {
        val vh = UserSmallViewHolder.newInstance(parent, administrators)
        vh.isGrid = isGrid
        return vh
    }

    override fun onBindViewHolder(holder: UserSmallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}