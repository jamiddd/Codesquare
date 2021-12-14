package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.User

interface UserClickListener {

    fun onUserClick(user: User)
    fun onUserOptionClick(projectId: String, chatChannelId: String, view: View, user: User, administrators: List<String>)

}