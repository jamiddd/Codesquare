package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.UserMinimalComparator2
import com.jamid.codesquare.data.UserMinimal2
import com.jamid.codesquare.listeners.UserClickListener

class UserMinimalAdapter(private val listener: UserClickListener? = null): ListAdapter<UserMinimal2, UserViewHolder>(UserMinimalComparator2()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_vertical_item, parent, false), listener = listener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}