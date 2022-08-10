package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.UserComparator
import com.jamid.codesquare.data.User

class PagingUserAdapter(
    private val like: Boolean = false,
    private val small: Boolean = false,
    private val min: Boolean = false,
) : PagingDataAdapter<User, UserViewHolder>(UserComparator()){
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layout = if (small) {
            R.layout.user_grid_item
        } else {
            R.layout.user_vertical_item
        }

        return UserViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false), like, small, min)
    }


}