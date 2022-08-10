package com.jamid.codesquare.ui

import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.CommentAdapter
import com.jamid.codesquare.adapter.recyclerview.CommentViewHolder
import com.jamid.codesquare.adapter.recyclerview.PostAdapter3
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.FragmentCommentsBinding
// something simple
class CommentsFragment :
    PagingDataFragment<FragmentCommentsBinding, Comment, CommentViewHolder>() {

    private var parentPost: Post? = null
    private var parentComment: Comment? = null
    private val currentUser = UserManager.currentUser
    private val staticList = mutableListOf<Post>()

    private fun setStaticPostRecycler(post: Post?) {
        val list = if (post != null) {
            listOf(post, Post().apply { isAd = true })
        } else {
            listOf(Post().apply { isAd = true })
        }

        staticList.clear()
        staticList.addAll(list)

        val postAdapter = PostAdapter3(viewLifecycleOwner, activity, activity).apply {
            shouldShowJoinButton = false
            allowContentClick = false
        }

        binding.commentPostRecycler.apply {
            adapter = postAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = null
        }

        postAdapter.submitList(list)

    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCommentsBinding.bind(view)

        val commentChannelId = arguments?.getString(COMMENT_CHANNEL_ID) ?: return

        binding.commentsRecycler.apply {
            adapter = myPagingAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = null
        }

        val query = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(commentChannelId)
            .collection(COMMENTS)

        getItems(viewLifecycleOwner) {
            viewModel.getPagedComments(commentChannelId, query)
        }

        binding.commentsRecycler.post {
            val post = arguments?.getParcelable<Post>(POST)
            if (post != null) {
                setStaticPostRecycler(post)
            } else {
                binding.commentPostRecycler.hide()
            }
        }


        /*   attachAd(binding.adView, null)
           binding.adView.show()*/

        binding.commentSendBtn.isEnabled = false
        binding.commentInputLayout.doAfterTextChanged {
            binding.commentSendBtn.isEnabled = !it.isNullOrBlank()
        }

        val parent = arguments?.getParcelable<Parcelable>(PARENT) ?: return

        if (parent is Post) {
            parentPost = parent
        } else if (parent is Comment) {
            parentComment = parent
        }

        setCommentInputUI()

        setSendButton()

        binding.replyingToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {

                binding.replyingToText.show()
                val sender = it.sender

                val rt = if (sender.userId == UserManager.currentUserId) {
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

                binding.replyingToText.text = rt

                setSendButton(it)

                val keyboardOpen = keyboardState.value
                if (keyboardOpen == false) {
                    showKeyboard()
                }

                runDelayed(200) {
                    binding.commentInputLayout.requestFocus()
                }

            } else {
                binding.replyingToText.hide()
                binding.replyingToText.text = getString(R.string.replying_to)

                setSendButton()
            }
        }
/*
        binding.commentFragmentScroll.post {
            binding.commentFragmentScroll.smoothScrollTo(0, binding.commentPostRecycler.measuredHeight)
        }*/

        keyboardState.observe(viewLifecycleOwner) { isOpened ->
            if (isOpened) {
                binding.commentFragmentScroll.fullScroll(View.FOCUS_DOWN)
                binding.commentInputLayout.requestFocus()
            }
        }

        if (parentPost != null) {
            binding.commentsHeader.text = "Comments (${parentPost?.commentsCount})"
        }

        if (parentComment != null) {
            binding.commentsHeader.text = "Comments (${parentComment?.repliesCount})"
        }

    }

    private fun setCommentInputUI() {
        val currentUser = UserManager.currentUser
        binding.senderImg.setImageURI(currentUser.photo)
        binding.commentInputLayout.requestFocus()

        runDelayed(600) {
            binding.constraintLayout3.slideReset()
        }
    }


    private fun setSendButton(replyComment: Comment? = null) {

        fun send(content: String, prevComment: Comment) {
            val comment1 = Comment(
                randomId(),
                content,
                currentUser.id,
                currentUser.minify(),
                prevComment.commentId,
                prevComment.postId,
                prevComment.threadChannelId,
                randomId(),
                prevComment.commentChannelId,
                0,
                0,
                prevComment.commentLevel + 1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                false,
                prevComment.postTitle
            )

            viewModel.sendComment(comment1, prevComment) {

            }

            binding.commentInputLayout.text.clear()

        }

        if (replyComment != null) {
            binding.commentSendBtn.setOnClickListener {
                if (binding.commentInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.commentInputLayout.text.trim().toString()
                send(content, replyComment)

                viewModel.replyToContent.postValue(null)
            }
        } else {
            binding.commentSendBtn.setOnClickListener {

                if (binding.commentInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.commentInputLayout.text.trim().toString()

                if (parentPost != null) {
                    val comment1 = Comment(
                        randomId(),
                        content,
                        currentUser.id,
                        currentUser.minify(),
                        parentPost!!.id,
                        parentPost!!.id,
                        parentPost!!.commentChannel,
                        randomId(),
                        null,
                        0,
                        0,
                        0,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        false,
                        parentPost!!.name
                    )
                    viewModel.sendComment(comment1, parentPost!!) {
                        //
                    }
                } else {
                    parentComment?.let {
                        send(content, it)
                    }
                }

                binding.commentInputLayout.text.clear()

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.replyToContent.postValue(null)
    }

    companion object {

        const val TAG = "CommentsFragment"

        fun newInstance(bundle: Bundle) =
            CommentsFragment().apply {
                arguments = bundle
            }

    }

    override fun getPagingAdapter(): PagingDataAdapter<Comment, CommentViewHolder> {
        return CommentAdapter(activity)
    }

    override fun onPagingDataChanged(itemCount: Int) {
        if (itemCount == 0) {
            binding.noCommentsText.text = "No comments yet. Be the first one to comment. \uD83D\uDC4B"
            binding.noCommentsText.show()
        } else {
            binding.noCommentsText.hide()
        }
    }

    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {

    }

    override fun onAdapterStateChanged(state: AdapterState, error: Throwable?) {
         setDefaultPagingLayoutBehavior(
             state,
             error,
             null,
             infoText = binding.noCommentsText,
             progress = null,
             recyclerView = binding.commentsRecycler
         )
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentCommentsBinding {
        return FragmentCommentsBinding.inflate(inflater)
    }

}