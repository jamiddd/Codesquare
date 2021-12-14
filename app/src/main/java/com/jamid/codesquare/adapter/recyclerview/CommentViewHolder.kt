package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
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
import com.jamid.codesquare.databinding.CommentItemBinding
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.listeners.CommentListener
import com.jamid.codesquare.toast

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

            /*// checking for stale user data, what if user data has been updated
            FireUtility.getOtherUser(comment.sender.userId) {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val newUser = it.result.toObject(User::class.java)!!.minify()
                        if (newUser != comment.sender) {
                            comment.sender = newUser
                            setUserData(comment.sender)

                            val changes = mapOf("sender" to comment.sender)
                            val ref = Firebase.firestore.collection("chatChannels")
                                .document(comment.commentChannelId)
                                .collection("comments")
                                .document(comment.commentId)

                            // if the user data is stale, update comment in firebase and make local changes
                            FireUtility.updateDocument(ref, changes) { it1 ->
                                if (it1.isSuccessful) {
                                    commentListener.onCommentUpdate(comment)
                                } else {
                                    Log.e(TAG, it1.exception?.localizedMessage.orEmpty())
                                }
                            }

                        }
                    } else {
                        // if somehow the document doesn't exists, probably the user was deleted
                        FireUtility.deleteUser(comment.sender.userId) { it1 ->
                            if (it1.isSuccessful) {

                                // delete the user locally, if there is any because stale data shouldn't exist in any local form
                                commentListener.onNoUserFound(comment.sender.userId)

                                // deleting the comment entirely because the sender doesn't exist anymore
                                FireUtility.deleteComment(comment.commentChannelId, comment.commentId) { it2 ->
                                    if (it2.isSuccessful) {
                                        commentListener.onCommentDelete(comment)
                                    } else {
                                        Log.e(TAG, it2.exception?.localizedMessage.orEmpty())
                                    }
                                }
                            } else {
                                Log.e(TAG, it1.exception?.localizedMessage.orEmpty())
                            }
                        }
                    }
                } else {
                    Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                }
            }*/

            binding.commentContent.text = comment.content

            binding.commentTime.text = "• " + getTextForTime(comment.createdAt)

            setLikeAndReplies(comment)

            binding.commentLikeBtn.isSelected = comment.isLiked

            binding.commentLikeBtn.setOnClickListener {
                commentListener.onCommentLike(comment.copy())
                binding.commentLikeBtn.isSelected = !binding.commentLikeBtn.isSelected
                setLikeAndReplies(comment)
            }

            view.setOnClickListener {
                commentListener.onClick(comment)
            }

            binding.commentOptionBtn.setOnClickListener {
                val popUpMenu = PopupMenu(view.context, it)
                popUpMenu.inflate(R.menu.comment_menu)
                popUpMenu.show()

                popUpMenu.setOnMenuItemClickListener { it1 ->
                    when (it1.itemId) {
                        R.id.comment_report -> {
                            commentListener.onReportClick(comment)
                        }
                    }
                    true
                }
            }
        }
    }

    private fun setLikeAndReplies(comment: Comment) {
        val likeRepliesText = "${comment.likesCount} Likes • ${comment.repliesCount} Replies"
        binding.commentLikesReplies.text = likeRepliesText
    }

    companion object {

        private const val TAG = "CommentViewHolder"

        fun newInstance(parent: ViewGroup, commentListener: CommentListener)
            = CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false), commentListener)

    }

}