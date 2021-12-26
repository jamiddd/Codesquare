package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.comparators.UserComparator
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.UserItemAltBinding
import com.jamid.codesquare.listeners.UserClickListener

class UserAdapter: ListAdapter<User, UserViewHolder>(UserComparator()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item_alt, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class UserViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val userClickListener = view.context as UserClickListener

    fun bind(user: User) {
        val binding = UserItemAltBinding.bind(view)
        binding.userFullName.text = user.name
        binding.userTagDesc.text = user.tag
        binding.userImg.setImageURI(user.photo)

        val currentUser = UserManager.currentUser
        binding.userLikeBtn.isSelected = currentUser.likedUsers.contains(user.id)

        binding.userLikeBtn.setOnClickListener {
            userClickListener.onUserLikeClick(user.copy())
            binding.userLikeBtn.isSelected = !binding.userLikeBtn.isSelected
        }

        view.setOnClickListener {
            userClickListener.onUserClick(user)
        }

        /*view.setOnLongClickListener {
            userClickListener.onUserOptionClick()
            true
        }*/

    }
}