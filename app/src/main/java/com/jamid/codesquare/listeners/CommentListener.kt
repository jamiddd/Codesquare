package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.User

interface CommentListener {
    fun onCommentLikeClicked(comment: Comment)
    fun onCommentReply(comment: Comment)
    fun onClick(comment: Comment)
    fun onCommentDelete(comment: Comment)
    fun onCommentUpdate(comment: Comment)
    fun onNoUserFound(userId: String)
    fun onReportClick(comment: Comment)
    fun onCommentUserClick(user: User)
    fun onOptionClick(comment: Comment)
}