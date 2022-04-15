package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.*
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.ProjectItemBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.ProjectClickListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProjectViewHolder(val v: View): PostViewHolder(v), ImageClickListener {

    // just for external use cases
    private lateinit var mProject: Project
    private lateinit var binding: ProjectItemBinding

    // a click listener for all project related actions
    private val projectClickListener = view.context as ProjectClickListener

    /**
     * To save or un-save the project. This function saves or un-saves the project and also requests the viewHolder
     * to be laid out again with the updated project
     *
     * @param project The project to be saved or un-saved based on the current project state.
     * */
    fun saveUnSaveProject(project: Project) {
        projectClickListener.onProjectSaveClick(project.copy()) {
            bind(it)
        }
    }

    /**
     * To like or dislike the project. This function likes or dislikes the project and also requests the viewHolder
     * to be laid out again with the updated project
     *
     * @param project The project to be liked or disliked based on the current project state.
     * */
    private fun likeDislikeProject(project: Project) {
        projectClickListener.onProjectLikeClick(project.copy()) {
            bind(it)
        }
    }

    /**
     * To update the viewHolder views related to the creator of the project
     *
     * @param project The project to be used while updating user related views
     * */
    private fun setProjectCreatorInfo(project: Project) {
        binding.projectUserImg.setImageURI(project.creator.photo)
        binding.projectUserName.text = project.creator.name

        binding.projectUserImg.setOnClickListener {
            projectClickListener.onProjectCreatorClick(project)
        }

        binding.projectUserName.setOnClickListener {
            projectClickListener.onProjectCreatorClick(project)
        }

        if (project.location.address.isNotBlank()) {
            binding.projectLocation.text = project.location.address

            binding.projectLocation.setOnClickListener {
                projectClickListener.onProjectLocationClick(project)
            }
        } else {
            binding.projectLocation.hide()
        }
    }


    /**
     * To implement a recyclerView with project images
     *
     * @param project The project to be used to implement project images recycler
     * */
    private fun setImagesRecycler(project: Project) {

        val imageAdapter = ImageAdapter(this)
        val helper = LinearSnapHelper()

        val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = manager
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
            if (onFlingListener == null) {
                helper.attachToRecyclerView(this)
            }
        }

        val imagePipeline = Fresco.getImagePipeline()

        for (image in project.images) {
            val imageRequest = ImageRequest.fromUri(image)
            imagePipeline.prefetchToDiskCache(imageRequest, view.context)
        }

        imageAdapter.submitList(project.images)

        if (project.images.size == 1) {
            binding.projectImagesCounter.hide()
        } else {
            binding.projectImagesCounter.show()
        }

        binding.projectImagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = manager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                     val counterText = "${pos + 1}/${project.images.size}"
                    binding.projectImagesCounter.text = counterText

                    binding.rightBtn.setOnClickListener {
                        binding.projectImagesRecycler.smoothScrollToPosition(pos + 1)
                    }

                    binding.leftBtn.setOnClickListener {
                        binding.projectImagesRecycler.smoothScrollToPosition(pos - 1)
                    }

                    if (project.images.size == 1) {
                        binding.leftBtn.hide()
                        binding.rightBtn.hide()
                        binding.projectImagesCounter.hide()
                    } else {
                        if (pos == 0) {
                            binding.leftBtn.hide()
                        } else {
                            binding.leftBtn.show()
                        }

                        if (pos == project.images.size - 1) {
                            binding.rightBtn.hide()
                        } else {
                            binding.rightBtn.show()
                        }
                    }
                }
            }
        })
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun bind(project: Project?) {
        if (project != null) {
            binding = ProjectItemBinding.bind(view)

            mProject = project
            view.tag = project.id

            setProjectCreatorInfo(project)

            setImagesRecycler(project)

            setStaticContent(project)

            setMutableContent(project)

            checkForStaleData(project)

        }
    }

    private fun checkForStaleData(project: Project) {
        projectClickListener.onCheckForStaleData(project) {
            bind(it)
        }
    }

    /**
     * To update all the views that are mutable in nature, ie. the views that are actionable
     *
     * @param project The project used to update actionable views
     * */
    private fun setMutableContent(project: Project) {
        binding.projectLikeBtn.isSelected = project.isLiked

        binding.projectLikeBtn.setOnClickListener {
            likeDislikeProject(project)
        }

        binding.projectSaveBtn.isSelected = project.isSaved

        binding.projectSaveBtn.setOnClickListener {
            saveUnSaveProject(project)
        }

        setLikeDislike(project)

        setJoinButton(project)

    }


    /**
     * To update all the views that are immutable in nature, ie. the views that doesn't
     * have any action
     *
     * @param project The project used to update non-actionable views
     * */
    private fun setStaticContent(project: Project) {

        binding.projectTitle.text = project.name
        binding.projectContent.text = project.content

        /*binding.projectContent.setOnLongClickListener {
            binding.root.performLongClick()
        }*/

        binding.projectContent.doOnLayout {
            if (binding.projectContent.lineCount > MAX_LINES) {
                val lastCharShown = binding.projectContent.layout.getLineVisibleEnd(MAX_LINES - 1)
                binding.projectContent.maxLines = MAX_LINES
                val moreString = "Show more"
                val suffix = "  $moreString"

                val actionDisplayText: String = project.content.substring(0, lastCharShown - suffix.length - 3) + "..." + suffix
                val truncatedSpannableString = SpannableString(actionDisplayText)
                val startIndex = actionDisplayText.indexOf(moreString)

                val cs = object: ClickableSpan() {

                    override fun onClick(p0: View) {
                        projectClickListener.onProjectClick(project.copy())
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        val color = if (view.context.isNightMode()) {
                            Color.WHITE
                        } else {
                            Color.BLACK
                        }
                        ds.color = color
                    }

                }

                val cs1 = object : ClickableSpan() {
                    override fun onClick(p0: View) {
                        binding.projectContent.maxLines = Int.MAX_VALUE
                        binding.projectContent.text = project.content

                        view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                            delay(200)
                            binding.projectContent.setOnClickListener {
                                projectClickListener.onProjectClick(project.copy())
                            }
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = view.context.accentColor()
                    }

                }

                truncatedSpannableString.setSpan(cs,
                    0,
                    startIndex - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                truncatedSpannableString.setSpan(cs1,
                    startIndex,
                    startIndex + moreString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                truncatedSpannableString.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + moreString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

                binding.projectContent.movementMethod = LinkMovementMethod.getInstance()

                binding.projectContent.text = truncatedSpannableString

                view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                    delay(200)
                    binding.projectContent.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }

        binding.projectTime.text = getTextForTime(project.createdAt)

        binding.projectCommentBtn.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }

        binding.projectOption.setOnClickListener {
            projectClickListener.onProjectOptionClick(project)
        }

        binding.root.setOnClickListener {
            projectClickListener.onProjectClick(project.copy())
        }

        binding.root.setOnLongClickListener {
            projectClickListener.onProjectOptionClick(project.copy())
            true
        }

    }


    /**
     * To update likes, comment and contributors count
     *
     * @param project The project to be used to update likes, comment and contributors count
     * */
    private fun setLikeDislike(project: Project) {
        val likesString = getLikesString(project.likes.toInt())
        val commentsString = getCommentsString(project.comments.toInt())
        val contributorsString = getContributorsString(project.contributors.size)
        val likeCommentText = "$likesString • $commentsString • $contributorsString"

        val cs1 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                projectClickListener.onProjectSupportersClick(project)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val color = if (view.context.isNightMode()) {
                    Color.WHITE
                } else {
                    Color.GRAY
                }
                ds.color = color
            }
        }

        val cs2 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                projectClickListener.onProjectCommentClick(project)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val color = if (view.context.isNightMode()) {
                    Color.WHITE
                } else {
                    Color.GRAY
                }
                ds.color = color
            }
        }

        val cs3 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                projectClickListener.onProjectContributorsClick(project)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val color = if (view.context.isNightMode()) {
                    Color.WHITE
                } else {
                    Color.GRAY
                }
                ds.color = color
            }
        }

        val s1 = 0
        val e1 = likesString.length

        val s2 = e1 + 3
        val e2 = s2 + commentsString.length

        val s3 = e2 + 3
        val e3 = s3 + contributorsString.length

        val formattedString = SpannableString(likeCommentText)
        formattedString.setSpan(cs1, s1, e1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        formattedString.setSpan(cs2, s2, e2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        formattedString.setSpan(cs3, s3, e3, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.projectLikeCommentText.movementMethod = LinkMovementMethod.getInstance()

        binding.projectLikeCommentText.text = formattedString
    }

    private fun getCommentsString(size: Int): String {
        return if (size == 1) {
            "1 Comment"
        } else {
            "$size Comments"
        }
    }

    private fun getLikesString(size: Int): String {
        return if (size == 1) {
            "1 Like"
        } else {
            "$size Likes"
        }
    }

    private fun getContributorsString(size: Int): String {
        return if (size == 1) {
            "1 Contributor"
        } else {
            "$size Contributors"
        }
    }

    /**
     * Special function made for join button as it is very crucial for proper working
     *
     * @param project The project to be used to set up join button
     * */
    private fun setJoinButton(project: Project) {

        when {
            project.isMadeByMe -> {
                binding.projectJoinBtn.hide()
            }
            project.isCollaboration -> {
                binding.projectJoinBtn.hide()
            }
            project.isRequested -> {
                binding.projectJoinBtn.show()
                binding.projectJoinBtn.text = view.context.getString(R.string.undo)
            }
            else -> binding.projectJoinBtn.show()
        }

        if (project.isBlocked) {
            binding.projectJoinBtn.hide()
        } else {
            if (project.isRequested) {
                binding.projectJoinBtn.text = view.context.getString(R.string.undo)
                binding.projectJoinBtn.setOnClickListener {
                    projectClickListener.onProjectUndoClick(project) { newProject ->
                        bind(newProject)
                    }
                }
            } else {
                binding.projectJoinBtn.text = view.context.getString(R.string.join)
                binding.projectJoinBtn.setOnClickListener {
                    projectClickListener.onProjectJoinClick(project) { newProject ->
                        bind(newProject)
                    }
                }
            }
        }
    }

    companion object {

        fun newInstance(parent: ViewGroup): ProjectViewHolder {
            return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
        }

    }

    override fun onImageClick(view: View, image: Image) {
        projectClickListener.onProjectClick(mProject)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        //
    }

}