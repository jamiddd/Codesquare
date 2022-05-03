package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.UserMinimal

interface CommentListener {
    fun onCommentLikeClicked(comment: Comment, onChange: (newComment: Comment) -> Unit)
    fun onCommentReply(comment: Comment)
    fun onClick(comment: Comment)
    fun onCommentDelete(comment: Comment)
    fun onCommentUpdate(comment: Comment)
    fun onNoUserFound(userId: String)
    fun onReportClick(comment: Comment)
    fun onCommentUserClick(userMinimal: UserMinimal)
    fun onOptionClick(comment: Comment)
    fun onCheckForStaleData(comment: Comment, onUpdate: (newComment: Comment) -> Unit)
}

interface CommentMiniListener {
    fun onOptionClick(comment: Comment)
}