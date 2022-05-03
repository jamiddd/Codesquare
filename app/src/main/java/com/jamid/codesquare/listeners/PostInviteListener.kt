package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.PostInvite

interface PostInviteListener {
    fun onPostInviteAccept(postInvite: PostInvite, onFailure: () -> Unit)
    fun onPostInviteCancel(postInvite: PostInvite)
    fun onPostInvitePostDeleted(postInvite: PostInvite)
    fun onCheckForStaleData(postInvite: PostInvite)
}