package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
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
    private lateinit var bottomBinding: CommentBottomLayoutBinding
    private val currentUser = UserManager.currentUser

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val commentChannelId = arguments?.getString(COMMENT_CHANNEL_ID) ?: return
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
        initBottomLayout()
    }

    @SuppressLint("InflateParams")
    private fun initBottomLayout() {

        val bottomView = layoutInflater.inflate(R.layout.comment_bottom_layout, null, false)

        binding.pagerRoot.addView(bottomView)

        val parent = arguments?.getParcelable<Parcelable>(PARENT) ?: return

        if (parent is Project) {
            project = parent
        } else if (parent is Comment) {
            comment = parent
        }

        val params = bottomView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.pagerRoot.id
        params.endToEnd = binding.pagerRoot.id
        params.bottomToBottom = binding.pagerRoot.id
        bottomView.layoutParams = params

        bottomView.updateLayout(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        bottomBinding = CommentBottomLayoutBinding.bind(bottomView)

        bottomBinding.senderImg.setImageURI(currentUser.photo)

        setSendButton(bottomBinding.commentSendBtn, bottomBinding.commentInputLayout)

        bottomBinding.replyingToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        binding.pagerItemsRecycler.setPadding(0, 0, 0, convertDpToPx(56))

        bottomBinding.commentInputLayout.requestFocus()

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {
                val sender = it.sender
                bottomBinding.replyingToText.show()
                val name = sender.name
                val replyToText = "Replying to $name"
                val sp = SpannableString(replyToText)
                sp.setSpan(
                    StyleSpan(Typeface.BOLD),
                    replyToText.length - name.length,
                    replyToText.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                bottomBinding.replyingToText.text = sp

                setSendButton(
                    bottomBinding.commentSendBtn,
                    bottomBinding.commentInputLayout,
                    it
                )

                showKeyboard()

            } else {
                bottomBinding.replyingToText.hide()
                bottomBinding.replyingToText.text = getString(R.string.replying_to)

                hideKeyboard()

                setSendButton(
                    bottomBinding.commentSendBtn,
                    bottomBinding.commentInputLayout
                )
            }
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

        fun newInstance(commentChannelId: String, title: String, obj: Parcelable? = null) =
            CommentsFragment().apply {
                arguments = bundleOf(
                    TITLE to title,
                    COMMENT_CHANNEL_ID to commentChannelId,
                    PARENT to obj
                )
            }

    }

}