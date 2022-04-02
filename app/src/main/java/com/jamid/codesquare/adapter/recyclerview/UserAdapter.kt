package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.UserComparator
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.User
import com.jamid.codesquare.listeners.UserClickListener

class UserAdapter(
    private val like: Boolean = false,
    private val small: Boolean = false,
    private val min: Boolean = false,
    private val vague: Boolean = false,
    private val grid: Boolean = false,
    private val associatedChatChannel: ChatChannel? = null,
    private val userClickListener: UserClickListener? = null
): ListAdapter<User, UserViewHolder>(UserComparator()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layout = if (small) {
            R.layout.user_grid_item
        } else {
            R.layout.user_vertical_item
        }

        return UserViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false), like, small, min, vague, grid, associatedChatChannel, userClickListener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}