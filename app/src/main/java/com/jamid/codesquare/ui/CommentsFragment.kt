package com.jamid.codesquare.ui

import android.graphics.Typeface
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
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
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.CommentBottomLayoutBinding
import com.jamid.codesquare.listeners.CommentListener

@ExperimentalPagingApi
class CommentsFragment: PagerListFragment<Comment, CommentViewHolder>(), CommentListener {

    private var project: Project? = null
    private var comment: Comment? = null

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val commentChannelId = arguments?.getString("commentChannelId") ?: return
        shouldShowImage = false
        val query = Firebase.firestore.collection("commentChannels")
            .document(commentChannelId)
            .collection("comments")

        getItems {
            viewModel.getPagedComments(commentChannelId, query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView?.itemAnimator = null
        noItemsText?.text = "No comments yet. Be the first one to comment."

//        initTopLayout(title)

        initBottomLayout()
    }

    /*private fun initTopLayout(title: String) {
        val topView = layoutInflater.inflate(R.layout.comment_top_layout, null, false)
        binding.pagerRoot.addView(topView)

        val params = topView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.pagerRoot.id
        params.endToEnd = binding.pagerRoot.id
        params.topToTop = binding.pagerRoot.id
        topView.layoutParams = params

        binding.pagerRefresher.setPadding(0, convertDpToPx(56), 0, 0)

        topView.updateLayout(convertDpToPx(56), ConstraintLayout.LayoutParams.MATCH_PARENT)

        val topViewBinding = CommentTopLayoutBinding.bind(topView)

        topViewBinding.topToolbar.title = title

        topViewBinding.topToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@CommentsFragment).commit()
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@CommentsFragment).commit()
        }

    }*/

    private fun initBottomLayout() {

        val bottomView = layoutInflater.inflate(R.layout.comment_bottom_layout, null, false)

        binding.pagerRoot.addView(bottomView)

        val parent = arguments?.getParcelable<Parcelable>("parent") ?: return

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

        bottomView.updateLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        val bottomBinding = CommentBottomLayoutBinding.bind(bottomView)

        val currentUser = viewModel.currentUser.value!!
        bottomBinding.senderImg.setImageURI(currentUser.photo)

        setSendButton(bottomBinding.commentSendBtn, bottomBinding.commentInputLayout, currentUser)

        bottomBinding.replyingToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {
                val sender = it.sender
                bottomBinding.replyingToText.show()
                val name = sender.name
                val replyToText = "Replying to $name"
                val sp = SpannableString(replyToText)
                sp.setSpan(StyleSpan(Typeface.BOLD), replyToText.length - name.length, replyToText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                bottomBinding.replyingToText.text = sp

                setSendButton(bottomBinding.commentSendBtn, bottomBinding.commentInputLayout, currentUser, it)

                showKeyboard()

            } else {
                bottomBinding.replyingToText.hide()
                bottomBinding.replyingToText.text = "Replying to"

                hideKeyboard()

                setSendButton(bottomBinding.commentSendBtn, bottomBinding.commentInputLayout, currentUser)
            }
        }


    }

    private fun setSendButton(sendBtn: Button, inputLayout: EditText, currentUser: User, replyComment: Comment? = null) {
        if (replyComment != null) {
            sendBtn.setOnClickListener {
                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.toString()

                val comment1 = Comment(randomId(), content, currentUser.id, replyComment.commentId, replyComment.projectId, replyComment.threadChannelId, randomId(), 0, 0, replyComment.commentLevel + 1, System.currentTimeMillis(), emptyList(), currentUser.minify(), false, replyComment.postTitle)

                viewModel.sendComment(comment1, replyComment)

                inputLayout.text.clear()

                viewModel.replyToContent.postValue(null)
            }
        } else {
            sendBtn.setOnClickListener {

                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.toString()

                if (project != null) {
                    val comment1 = Comment(randomId(), content, currentUser.id, project!!.id, project!!.id, project!!.commentChannel, randomId(), 0, 0, 0, System.currentTimeMillis(), emptyList(), currentUser.minify(), false, project!!.title)
                    viewModel.sendComment(comment1, project!!)
                } else {
                    if (comment != null){
                        val comment1 = Comment(randomId(), content, currentUser.id, comment!!.commentId, comment!!.projectId, comment!!.threadChannelId, randomId(), 0, 0, comment!!.commentLevel + 1, System.currentTimeMillis(), emptyList(), currentUser.minify(), false, comment!!.postTitle)
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
        return CommentAdapter(this)
    }

    companion object {
        private const val TAG = "CommentsFragment"

        fun newInstance(commentChannelId: String, title: String, obj: Parcelable? = null) = CommentsFragment().apply {
            arguments = bundleOf("title" to title, "commentChannelId" to commentChannelId, "parent" to obj)
        }

    }

    override fun onCommentLike(comment: Comment) {
        viewModel.onCommentLiked(comment)
    }

    override fun onCommentReply(comment: Comment) {
        viewModel.replyToContent.postValue(comment)
    }

    override fun onClick(comment: Comment) {
//        showDialog(comment)
        val bundle = bundleOf("parent" to comment, "commentChannelId" to comment.threadChannelId)
        findNavController().navigate(R.id.action_commentsFragment_self, bundle)

    }

    override fun onCommentDelete(comment: Comment) {
        viewModel.deleteComment(comment)
    }

    override fun onCommentUpdate(comment: Comment) {
        viewModel.updateComment(comment)
    }

    override fun onNoUserFound(userId: String) {
        viewModel.deleteUserById(userId)
    }

    override fun onReportClick(comment: Comment) {
        findNavController().navigate(R.id.action_commentsFragment_to_reportFragment, bundleOf("contextObject" to comment))
    }

    @ExperimentalPagingApi
    fun showDialog(comment: Comment) {
        val fragmentManager = activity?.supportFragmentManager
        val newFragment = newInstance(comment.threadChannelId, "Comments", comment)
        // The device is smaller, so show the fragment fullscreen
        val transaction = fragmentManager?.beginTransaction()
        // For a little polish, specify a transition animation
        transaction?.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction
            ?.add(android.R.id.content, newFragment)
            ?.addToBackStack(null)
            ?.commit()
    }

}