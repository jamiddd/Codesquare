package com.jamid.codesquare.ui

import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentCommentsContainerBinding

class CommentsContainerFragment: Fragment() {

    private lateinit var binding: FragmentCommentsContainerBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var project: Project? = null
    private var comment: Comment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentsContainerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = arguments?.getParcelable<Parcelable>("parent") ?: return

        if (parent is Project) {
            project = parent
        } else if (parent is Comment) {
            comment = parent
        }

        val currentUser = viewModel.currentUser.value!!

        binding.senderImg.setImageURI(currentUser.photo)

        setSendButton(currentUser)

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {
                val sender = it.sender
                binding.replyingToText.show()
                val name = sender.name
                val replyToText = "Replying to $name"
                val sp = SpannableString(replyToText)
                sp.setSpan(StyleSpan(Typeface.BOLD), replyToText.length - name.length, replyToText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.replyingToText.text = sp

                setSendButton(currentUser, it)

                showKeyboard()

            } else {
                binding.replyingToText.hide()
                binding.replyingToText.text = "Replying to"

                hideKeyboard()

                setSendButton(currentUser)
            }
        }

        binding.replyingToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

    }

    private fun setSendButton(currentUser: User, replyComment: Comment? = null) {
        if (replyComment != null) {
            binding.commentSendBtn.setOnClickListener {
                if (binding.commentInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.commentInputLayout.text.toString()

                val comment = Comment(randomId(), content, currentUser.id, replyComment.commentId, replyComment.projectId, replyComment.threadChannelId, randomId(), 0, 0, replyComment.commentLevel + 1, System.currentTimeMillis(), currentUser, false, replyComment.postTitle)

                viewModel.sendComment(comment, replyComment.commentChannelId)

                binding.commentInputLayout.text.clear()

                viewModel.replyToContent.postValue(null)
            }
        } else {
            binding.commentSendBtn.setOnClickListener {

                if (binding.commentInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.commentInputLayout.text.toString()

                if (project != null) {
                    val comment1 = Comment(randomId(), content, currentUser.id, project!!.id, project!!.id, project!!.commentChannel, randomId(), 0, 0, 0, System.currentTimeMillis(), currentUser, false, project!!.title)
                    viewModel.sendComment(comment1)
                } else {
                    val comment1 = Comment(randomId(), content, currentUser.id, comment!!.commentId, project!!.id, comment!!.threadChannelId, randomId(), 0, 0, comment!!.commentLevel + 1, System.currentTimeMillis(), currentUser, false, comment!!.postTitle)
                    viewModel.sendComment(comment1, comment!!.commentChannelId)
                }


                binding.commentInputLayout.text.clear()

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.replyToContent.postValue(null)
        viewModel.currentCommentChannelIds.pop()
    }

    companion object {
        private const val TAG = "CommentsContainer"
    }

}