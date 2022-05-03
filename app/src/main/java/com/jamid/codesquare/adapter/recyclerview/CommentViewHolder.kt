package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.databinding.CommentItemBinding
import com.jamid.codesquare.listeners.CommentListener

class CommentViewHolder(val view: View, private val commentListener: CommentListener): RecyclerView.ViewHolder(view) {

    private lateinit var binding: CommentItemBinding

    fun bind(comment: Comment?) {
        if (comment != null) {
            binding = CommentItemBinding.bind(view)

            binding.commentUserImg.setImageURI(comment.sender.photo)
            binding.commentUserName.text = comment.sender.name

            binding.commentUserName.setOnClickListener {
                commentListener.onCommentUserClick(comment.sender)
            }

            binding.commentUserImg.setOnClickListener {
                commentListener.onCommentUserClick(comment.sender)
            }

            binding.commentReplyBtn.setOnClickListener {
                commentListener.onCommentReply(comment.copy())
            }

            binding.commentContent.text = comment.content

            val timeText = "• " + getTextForTime(comment.createdAt)
            binding.commentTime.text = timeText

            setLikeAndReplies(comment)

            setCommentLikeBtn(comment)


            view.setOnClickListener {
                commentListener.onClick(comment)
            }

            binding.commentOptionBtn.setOnClickListener {
                commentListener.onOptionClick(comment)
            }

            commentListener.onCheckForStaleData(comment) {
                bind(it)
            }

        }
    }

    private fun setCommentLikeBtn(comment: Comment) {
        Firebase.firestore.collection(USERS).document(UserManager.currentUserId)
            .collection(LIKED_COMMENTS).document(comment.commentId)
            .get()
            .addOnSuccessListener {
                comment.isLiked = it != null && it.exists()

                binding.commentLikeBtn.isSelected = comment.isLiked

                binding.commentLikeBtn.setOnClickListener {
                    commentListener.onCommentLikeClicked(comment.copy()) { comment ->
                        bind(comment)
                    }
                    binding.commentLikeBtn.isSelected = !binding.commentLikeBtn.isSelected
                    setLikeAndReplies(comment)
                }
            }.addOnFailureListener {
                Log.e(TAG, "setCommentLikeBtn: ${it.localizedMessage}")
            }
    }

    private fun setLikeAndReplies(comment: Comment) {
        val likeRepliesText = "${comment.likesCount} Likes • ${comment.repliesCount} Replies"
        binding.commentLikesReplies.text = likeRepliesText
    }

    companion object {
        fun newInstance(parent: ViewGroup, commentListener: CommentListener)
            = CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false), commentListener)
    }

}