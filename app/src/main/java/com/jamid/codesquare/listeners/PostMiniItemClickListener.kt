package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.PostInvite
// something simple
interface PostMiniItemClickListener {
    fun onInviteClick(post: Post, receiverId: String, onFailure: () -> Unit)
    fun onRevokeInviteClick(invite: PostInvite, onFailure: () -> Unit)
}