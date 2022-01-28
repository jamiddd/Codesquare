package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.UserComparator
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.UserItemAltBinding
import com.jamid.codesquare.listeners.UserClickListener

class UserAdapter: ListAdapter<User, UserViewHolder>(UserComparator()){

    var shouldShowLikeButton = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item_alt, parent, false)).apply {
            shouldShowLikeButton = this@UserAdapter.shouldShowLikeButton
        }
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class UserViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val userClickListener = view.context as UserClickListener
    var shouldShowLikeButton = true

    fun bind(user: User) {

        val binding = UserItemAltBinding.bind(view)
        binding.userFullName.text = user.name
        binding.userTagDesc.text = user.tag

        binding.userImg.setImageURI(user.photo)

        binding.userLikeBtn.isSelected = user.isLiked

        binding.userLikeBtn.setOnClickListener {
            userClickListener.onUserLikeClick(user.copy())
            user.isLiked = !user.isLiked
            binding.userLikeBtn.isSelected = !binding.userLikeBtn.isSelected
        }

        view.setOnClickListener {
            userClickListener.onUserClick(user)
        }

        binding.userLikeBtn.isVisible = shouldShowLikeButton
    }
}