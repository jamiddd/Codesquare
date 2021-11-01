package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
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
import com.jamid.codesquare.listeners.CommentListener

class CommentViewHolder(val view: View, private val commentListener: CommentListener): RecyclerView.ViewHolder(view) {

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

            setUserData(comment.sender)

            replyBtn.setOnClickListener {
                commentListener.onCommentReply(comment.copy())
            }

            // checking for stale user data, what if user data has been updated
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
            }

            content.text = comment.content

            time.text = getTextForTime(comment.createdAt)

            setLikeAndReplies(comment)

            likeBtn.isSelected = comment.isLiked

            likeBtn.setOnClickListener {
                commentListener.onCommentLike(comment.copy())
                likeBtn.isSelected = !likeBtn.isSelected
                setLikeAndReplies(comment)
            }

            view.setOnClickListener {
                commentListener.onClick(comment)
            }
        }
    }

    private fun setUserData(sender: UserMinimal) {
        userImage.setImageURI(sender.photo)
        userName.text = sender.name
    }

    private fun setLikeAndReplies(comment: Comment) {
        val likeRepliesText = "${comment.likes} Likes • ${comment.repliesCount} Replies"
        likeReplies.text = likeRepliesText
    }

    companion object {

        private const val TAG = "CommentViewHolder"

        fun newInstance(parent: ViewGroup, commentListener: CommentListener)
            = CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false), commentListener)

    }

}