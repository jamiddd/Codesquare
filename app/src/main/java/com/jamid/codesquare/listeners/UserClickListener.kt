package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal2
// something simple
interface UserClickListener {
    fun onUserClick(user: User)
    fun onUserClick(userId: String)
    fun onUserClick(userMinimal: UserMinimal2)
    fun onUserOptionClick(user: User)
    fun onUserOptionClick(userMinimal: UserMinimal2)
    fun onUserLikeClick(user: User)
    fun onUserLikeClick(userId: String)
    fun onUserLikeClick(userMinimal: UserMinimal2)
}