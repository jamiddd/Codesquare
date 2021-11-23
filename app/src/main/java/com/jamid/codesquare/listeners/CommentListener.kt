package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Comment

interface CommentListener {
    fun onCommentLike(comment: Comment)
    fun onCommentReply(comment: Comment)
    fun onClick(comment: Comment)
    fun onCommentDelete(comment: Comment)
    fun onCommentUpdate(comment: Comment)
    fun onNoUserFound(userId: String)
    fun onReportClick(comment: Comment)
}