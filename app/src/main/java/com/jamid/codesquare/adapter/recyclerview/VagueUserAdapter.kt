package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User

class VagueUserAdapter(val currentUser: User, private val userIds: List<String>): RecyclerView.Adapter<UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item_alt, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userId = userIds[position]
        FireUtility.getUser(userId) {
            when (it) {
                is Result.Error -> Log.e(TAG, it.exception.localizedMessage.orEmpty())
                is Result.Success -> {
                    it.data.isCurrentUser = it.data.id == currentUser.id
                    holder.bind(it.data)
                }
                null -> Log.d(TAG, "Document doesn't exist. $userId")
            }
        }
    }

    override fun getItemCount(): Int {
        return userIds.size
    }

    companion object  {
        private val TAG = VagueUserAdapter::class.simpleName
    }

}