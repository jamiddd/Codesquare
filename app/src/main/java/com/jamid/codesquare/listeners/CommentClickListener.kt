package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Comment

interface CommentClickListener {
    fun onCommentLike(comment: Comment)
    fun onCommentReply(comment: Comment)
    fun onClick(comment: Comment)
}