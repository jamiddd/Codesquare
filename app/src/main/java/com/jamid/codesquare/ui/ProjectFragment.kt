package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentProjectBinding
import com.jamid.codesquare.listeners.ProjectClickListener


class ProjectFragment: Fragment() {

    private lateinit var binding: FragmentProjectBinding
    private lateinit var project: Project
    private var totalImageCount = 1
    private lateinit var projectClickListener: ProjectClickListener

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
            binding.starBtn.hide()
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

}