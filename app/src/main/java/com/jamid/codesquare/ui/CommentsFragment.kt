package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.CommentAdapter
import com.jamid.codesquare.adapter.recyclerview.CommentViewHolder
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.CommentBottomLayoutBinding
import com.jamid.codesquare.listeners.CommentListener

@ExperimentalPagingApi
class CommentsFragment : PagerListFragment<Comment, CommentViewHolder>() {

    private var project: Project? = null
    private var comment: Comment? = null
    private val currentUser = UserManager.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val commentChannelId = arguments?.getString(COMMENT_CHANNEL_ID) ?: return

        Log.d(TAG, commentChannelId)

        shouldShowImage = false

        val query = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(commentChannelId)
            .collection(COMMENTS)

        getItems {
            viewModel.getPagedComments(commentChannelId, query)
        }

        binding.pagerItemsRecycler.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.pagerItemsRecycler.itemAnimator = null
        binding.pagerNoItemsText.text = getString(R.string.empty_comments_greet)

        showKeyboard()
        initAlt()
    }

    private fun setCommentInputUI(senderImg: SimpleDraweeView, commentInputLayout: EditText) {
        val currentUser = UserManager.currentUser
        senderImg.setImageURI(currentUser.photo)
        commentInputLayout.requestFocus()
    }

    private fun initAlt() {
        val commentBottomRoot = requireActivity().findViewById<MaterialCardView>(R.id.comment_bottom_root)
        val sendBtn = commentBottomRoot.findViewById<MaterialButton>(R.id.comment_send_btn)!!
        val commentInputLayout = commentBottomRoot.findViewById<EditText>(R.id.comment_input_layout)!!
        val replyToText = commentBottomRoot.findViewById<TextView>(R.id.replying_to_text)!!
        val senderImg = commentBottomRoot.findViewById<SimpleDraweeView>(R.id.sender_img)!!
        val adView = commentBottomRoot.findViewById<AdView>(R.id.adView)!!

        commentBottomRoot.slideReset()

        val parent = arguments?.getParcelable<Parcelable>(PARENT) ?: return

        if (parent is Project) {
            project = parent
        } else if (parent is Comment) {
            comment = parent
        }

        setCommentInputUI(senderImg, commentInputLayout)

        setSendButton(sendBtn, commentInputLayout)

        replyToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        val actionLength = resources.getDimension(R.dimen.action_height)
        binding.pagerItemsRecycler.setPadding(0, 0, 0, actionLength.toInt())
        commentInputLayout.requestFocus()

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {
                replyToText.show()
                val sender = it.sender

                val rt = if (sender.id == UserManager.currentUserId) {
                    "Replying to your comment"
                } else {
                    // setting styles to reply view
                    val s = "Replying to ${sender.name}"
                    val sp = SpannableString(s)
                    sp.setSpan(
                        StyleSpan(Typeface.BOLD),
                        s.length - sender.name.length,
                        s.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sp
                }

                replyToText.text = rt

                setSendButton(
                    sendBtn,
                    commentInputLayout,
                    it
                )

                showKeyboard()
            } else {
                replyToText.hide()
                replyToText.text = getString(R.string.replying_to)

                setSendButton(
                    sendBtn,
                    commentInputLayout
                )
            }
        }

        if (currentUser.premiumState.toInt() == -1) {
            adView.loadAd(AdRequest.Builder().build())
            adView.show()
        } else {
            adView.hide()
        }

    }

    private fun setSendButton(
        sendBtn: Button,
        inputLayout: EditText,
        replyComment: Comment? = null
    ) {
        if (replyComment != null) {
            sendBtn.setOnClickListener {
                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.trim().toString()

                val comment1 = Comment(
                    randomId(),
                    content,
                    currentUser.id,
                    replyComment.commentId,
                    replyComment.projectId,
                    replyComment.threadChannelId,
                    randomId(),
                    0,
                    0,
                    replyComment.commentLevel + 1,
                    System.currentTimeMillis(),
                    emptyList(),
                    currentUser,
                    false,
                    replyComment.postTitle
                )

                viewModel.sendComment(comment1, replyComment)

                inputLayout.text.clear()

                viewModel.replyToContent.postValue(null)
            }
        } else {
            sendBtn.setOnClickListener {

                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.trim().toString()

                if (project != null) {
                    val comment1 = Comment(
                        randomId(),
                        content,
                        currentUser.id,
                        project!!.id,
                        project!!.id,
                        project!!.commentChannel,
                        randomId(),
                        0,
                        0,
                        0,
                        System.currentTimeMillis(),
                        emptyList(),
                        currentUser,
                        false,
                        project!!.name
                    )
                    viewModel.sendComment(comment1, project!!)
                } else {
                    if (comment != null) {
                        val comment1 = Comment(
                            randomId(),
                            content,
                            currentUser.id,
                            comment!!.commentId,
                            comment!!.projectId,
                            comment!!.threadChannelId,
                            randomId(),
                            0,
                            0,
                            comment!!.commentLevel + 1,
                            System.currentTimeMillis(),
                            emptyList(),
                            currentUser,
                            false,
                            comment!!.postTitle
                        )
                        viewModel.sendComment(comment1, comment!!)
                    }
                }

                inputLayout.text.clear()

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.replyToContent.postValue(null)
    }

    override fun getAdapter(): PagingDataAdapter<Comment, CommentViewHolder> {
        return CommentAdapter(requireActivity() as CommentListener)
    }

    companion object {

        const val TAG = "CommentsFragment"

        fun newInstance(bundle: Bundle) =
            CommentsFragment().apply {
                arguments = bundle
            }

    }

}