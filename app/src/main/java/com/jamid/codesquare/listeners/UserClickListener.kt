package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal2

interface UserClickListener {
    fun onUserClick(user: User)
    fun onUserClick(userMinimal: UserMinimal2)
    fun onUserOptionClick(user: User)
    fun onUserOptionClick(userMinimal: UserMinimal2)
    fun onUserLikeClick(user: User)
    fun onUserLikeClick(userMinimal: UserMinimal2)
}