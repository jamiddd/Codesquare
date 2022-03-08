package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.User

interface UserClickListener {
    fun onUserClick(user: User)
    fun onUserOptionClick(user: User)
    fun onUserLikeClick(user: User)
}