package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.listeners.CommentClickListener

class CommentViewHolder(val view: View, private val commentClickListener: CommentClickListener): RecyclerView.ViewHolder(view) {

    private val userImage: SimpleDraweeView = view.findViewById(R.id.comment_user_img)
    private val content: TextView = view.findViewById(R.id.comment_content)
    private val userName: TextView = view.findViewById(R.id.comment_user_name)
    private val time: TextView = view.findViewById(R.id.comment_time)
    private val likeReplies: TextView = view.findViewById(R.id.comment_likes_replies)
    private val likeBtn: Button = view.findViewById(R.id.comment_like_btn)
    private val replyBtn: Button = view.findViewById(R.id.comment_reply_btn)
    private val optionBtn: Button = view.findViewById(R.id.comment_option_btn)

    fun bind(comment: Comment?) {
        if (comment != null) {

            userImage.setImageURI(comment.sender.photo)
            userName.text = comment.sender.name

            replyBtn.setOnClickListener {
                commentClickListener.onCommentReply(comment.copy())
            }

            content.text = comment.content

            time.text = getTextForTime(comment.createdAt)

            setLikeAndReplies(comment)

            likeBtn.isSelected = comment.isLiked

            likeBtn.setOnClickListener {
                commentClickListener.onCommentLike(comment.copy())
                likeBtn.isSelected = !likeBtn.isSelected
                setLikeAndReplies(comment)
            }

            view.setOnClickListener {
                commentClickListener.onClick(comment)
            }

        }
    }

    private fun setLikeAndReplies(comment: Comment) {
        val likeRepliesText = "${comment.likes} Likes • ${comment.repliesCount} Replies"
        likeReplies.text = likeRepliesText
    }

    companion object {

        fun newInstance(parent: ViewGroup, commentClickListener: CommentClickListener)
            = CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false), commentClickListener)

    }

}