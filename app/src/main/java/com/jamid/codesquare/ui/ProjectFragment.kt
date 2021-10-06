package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter2
import com.jamid.codesquare.data.CommentChannel
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProjectBinding
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.UserClickListener


class ProjectFragment: Fragment() {

    private lateinit var binding: FragmentProjectBinding
    private lateinit var project: Project
    private var totalImageCount = 1
    private lateinit var projectClickListener: ProjectClickListener
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userClickListener: UserClickListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProjectBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        projectClickListener = activity as ProjectClickListener
        userClickListener = activity as UserClickListener

        val joinBtn = activity.findViewById<MaterialButton>(R.id.main_primary_action)
        joinBtn.show()

        project = arguments?.getParcelable("project") ?: return

        val imageAdapter = ImageAdapter {}

        val manager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val helper: SnapHelper = LinearSnapHelper()

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = manager
            onFlingListener = null
            helper.attachToRecyclerView(this)
        }

        totalImageCount = project.images.size
        imageAdapter.submitList(project.images)

        binding.projectImagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = manager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                    binding.imagesCounter.text = "${pos + 1}/$totalImageCount"
                }
            }
        })

        binding.userImg.setImageURI(project.creator.photo)
        binding.userName.text = project.creator.name

        binding.userImg.setOnClickListener {
            onUserClick()
        }

        binding.userName.setOnClickListener {
            onUserClick()
        }

        binding.projectTime.text = getTextForTime(project.createdAt)

        binding.projectContent.text = project.content

        if (project.location.address.isNotBlank()) {
            binding.projectLocation.text = project.location.address
        } else {
            binding.projectLocation.hide()
        }

        setLikeDislike()

        setLikeButton()

        setSaveButton()

        setTags()

        setContributors()

        setCommentLayout()

        joinBtn.setOnClickListener {
            if (joinBtn.text == "Join") {
                joinBtn.text = "Undo"
                joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_undo_24)
            } else {
                joinBtn.text = "Join"
                joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)
            }
            projectClickListener.onProjectJoinClick(project.copy())
        }

        if (project.isMadeByMe) {
            joinBtn.slideDown(convertDpToPx(100).toFloat())
        }

        if (project.isRequested) {
            joinBtn.text = "Undo"
            joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_undo_24)
        }

        if (project.isCollaboration) {
            joinBtn.slideDown(convertDpToPx(100).toFloat())
        }

        if (project.isMadeByMe) {
            binding.userLikeBtn.hide()
        }

        val currentUser = viewModel.currentUser.value!!

        binding.userLikeBtn.isSelected = currentUser.likedUsers.contains(project.creator.userId)
        if (binding.userLikeBtn.isSelected) {
            binding.userLikeBtn.text = "Dislike"
        } else {
            binding.userLikeBtn.text = "Like"
        }

        binding.userLikeBtn.setOnClickListener {
            if (binding.userLikeBtn.isSelected) {
                binding.userLikeBtn.text = "Like"
                viewModel.dislikeUser(project.creator.userId)
            } else {
                binding.userLikeBtn.text = "Dislike"
                viewModel.likeUser(project.creator.userId)
            }
        }

        binding.projectCommentBtn.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }

        binding.projectLikeCommentText.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }

    }

    private fun onUserClick() {
        viewModel.getOtherUser(project.creator.userId) {
            if (it.isSuccessful && it.result.exists()) {
                val user = it.result.toObject(User::class.java)!!
                userClickListener.onUserClick(user)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun setTags() {
        if (project.tags.isNotEmpty()) {
            binding.tagsHeader.show()
            binding.projectTags.show()
            addTags(project.tags)
        } else {
            binding.tagsHeader.hide()
            binding.projectTags.hide()
        }
    }

    private fun addTags(tags: List<String>) {
        for (tag in tags) {
            addTag(tag)
        }
    }

    private fun addTag(tag: String) {
        tag.trim()
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCheckable = false
        chip.isCloseIconVisible = false
        binding.projectTags.addView(chip)
    }

    private fun setSaveButton() {
        binding.projectSaveBtn.isSelected = project.isSaved

        binding.projectSaveBtn.setOnClickListener {
            projectClickListener.onProjectSaveClick(project.copy())
            binding.projectSaveBtn.isSelected = !binding.projectSaveBtn.isSelected
        }

    }

    private fun setLikeButton() {
        binding.projectLikeBtn.isSelected = project.isLiked

        binding.projectLikeBtn.setOnClickListener {
            projectClickListener.onProjectLikeClick(project.copy())
            if (binding.projectLikeBtn.isSelected) {
                // dislike
                project.likes = project.likes - 1
                project.isLiked = false
                binding.projectLikeBtn.isSelected = false
                setLikeDislike()
            } else {
                // like
                project.likes = project.likes + 1
                project.isLiked = true
                binding.projectLikeBtn.isSelected = true
                setLikeDislike()
            }
        }
    }

    private fun setLikeDislike() {
        val likeCommentText = "${project.likes} Likes • ${project.comments} Comments"
        binding.projectLikeCommentText.text = likeCommentText
    }

    private fun setContributors() {
        binding.contributorsHeaderLayout.show()
        binding.divider6.show()
        binding.projectContributorsRecycler.show()

        val userAdapter2 = UserAdapter2()

        binding.projectContributorsRecycler.apply {
            adapter = userAdapter2
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.getProjectContributors(project) {
            if (it.isSuccessful) {
                if (!it.result.isEmpty) {
                    val contributors = it.result.toObjects(User::class.java)
                    userAdapter2.submitList(contributors)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }

    }

    private fun setCommentLayout() {
        viewModel.getCommentChannel(project) {
            if (it.isSuccessful && it.result.exists()) {
                val commentChannel = it.result.toObject(CommentChannel::class.java)!!
                if (commentChannel.lastComment == null) {
                    binding.projectsLastComment.root.hide()
                    binding.commentsHeaderLayout.hide()
                    binding.divider9.hide()
                } else {

                    binding.projectsLastComment.root.show()
                    binding.commentsHeaderLayout.show()
                    binding.divider9.show()

                    binding.projectsLastComment.commentLikeBtn.hide()
                    binding.projectsLastComment.commentReplyBtn.hide()
                    binding.projectsLastComment.commentLikesReplies.hide()
                    binding.projectsLastComment.commentOptionBtn.hide()

                    val comment = commentChannel.lastComment
                    viewModel.getOtherUser(comment.senderId) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val sender = it1.result.toObject(User::class.java)!!
                            binding.projectsLastComment.apply {
                                commentUserName.text = sender.name
                                commentUserImg.setImageURI(sender.photo)
                                commentTime.text = getTextForTime(comment.createdAt)

                                commentContent.text = comment.content

                                val likeRepliesText = "${comment.likes} Likes • ${comment.repliesCount} Replies"
                                commentLikesReplies.text = likeRepliesText
                            }
                        } else {
                            viewModel.setCurrentError(it1.exception)
                        }
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

}