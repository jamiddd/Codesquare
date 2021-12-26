package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.widget.Button
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.data.Project
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.ScrollTouchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProjectViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val userImg: SimpleDraweeView = view.findViewById(R.id.project_user_img)
    private val userName: TextView = view.findViewById(R.id.project_user_name)
    private val location: TextView = view.findViewById(R.id.project_location)
    private val optionBtn: Button = view.findViewById(R.id.project_option)
    private val imagesRecycler: RecyclerView = view.findViewById(R.id.project_images_recycler)
    private val title: TextView = view.findViewById(R.id.project_title)
    private val content: TextView = view.findViewById(R.id.project_content)
    private val likeComment: TextView = view.findViewById(R.id.project_like_comment_text)
    private val likeBtn: Button = view.findViewById(R.id.project_like_btn)
    private val commentBtn: Button = view.findViewById(R.id.project_comment_btn)
    private val joinBtn: Button = view.findViewById(R.id.project_join_btn)
    private val saveBtn: Button = view.findViewById(R.id.project_save_btn)
    private val imagesCounter: TextView = view.findViewById(R.id.project_images_counter)
    private val time: TextView = view.findViewById(R.id.project_time)
    private val leftBtn: Button = view.findViewById(R.id.left_btn)
    private val rightBtn: Button = view.findViewById(R.id.right_btn)

    var currentImagePosition = 0
    private var totalImagesCount = 0

    private val projectClickListener = view.context as ProjectClickListener

    fun onSaveProjectClick(project: Project) {
        projectClickListener.onProjectSaveClick(project.copy())
        saveBtn.isSelected = !saveBtn.isSelected
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(project: Project?) {
        if (project != null) {

            Log.d(TAG, "${project.likes} -- ${project.isLiked}")

            val creator = project.creator
            val imagesCount = project.images.size
            totalImagesCount = imagesCount
            val ct = "1/$imagesCount"
            imagesCounter.text = ct

            userImg.setImageURI(creator.photo)
            userName.text = creator.name

            userImg.setOnClickListener {
                projectClickListener.onProjectCreatorClick(project)
            }

            userName.setOnClickListener {
                projectClickListener.onProjectCreatorClick(project)
            }

            if (project.location.address.isNotBlank()) {
                location.text = project.location.address
            } else {
                location.hide()
            }

            title.text = project.name
            content.text = project.content

            content.doOnLayout {
                if (content.lineCount > MAX_LINES) {
                    val lastCharShown = content.layout.getLineVisibleEnd(MAX_LINES - 1)
                    content.maxLines = MAX_LINES
                    val moreString = "Show more"
                    val suffix = "  $moreString"

                    val actionDisplayText: String = project.content.substring(0, lastCharShown - suffix.length - 3) + "..." + suffix
                    val truncatedSpannableString = SpannableString(actionDisplayText)
                    val startIndex = actionDisplayText.indexOf(moreString)

                    val cs1 = object : ClickableSpan() {
                        override fun onClick(p0: View) {
                            content.maxLines = Int.MAX_VALUE
                            content.text = project.content

                            view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                                delay(200)
                                content.setOnClickListener {
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

                    truncatedSpannableString.setSpan(cs1,
                        startIndex,
                        startIndex + moreString.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    content.movementMethod = LinkMovementMethod.getInstance()

                    content.text = truncatedSpannableString

                    view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                        delay(200)
                        content.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT)

                        content.setOnClickListener {
                            /*content.maxLines = Int.MAX_VALUE
                            content.text = project.content

                            view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                                delay(200)
                                content.setOnClickListener {

                                }
                            }*/
                            projectClickListener.onProjectClick(project.copy())
                        }

                    }
                }
            }

            val likeCommentText = "${project.likes} Likes • ${project.comments} Comments • ${project.contributors.size} Contributors"
            likeComment.text = likeCommentText

            val imageAdapter = ImageAdapter { _, _ ->
                projectClickListener.onProjectClick(project.copy())
            }
            val helper: SnapHelper = LinearSnapHelper()

            likeComment.setOnClickListener {
                projectClickListener.onProjectCommentClick(project.copy())
            }

            val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

            imagesRecycler.apply {
                adapter = imageAdapter
                layoutManager = manager
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

            val scrollInstance = ScrollTouchListener()

            imagesRecycler.addOnItemTouchListener(scrollInstance)

            imagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val pos = manager.findFirstCompletelyVisibleItemPosition()
                    if (pos != -1) {
                        currentImagePosition = pos
                        val counterText = "${pos + 1}/$imagesCount"
                        imagesCounter.text = counterText

                        rightBtn.setOnClickListener {
                            imagesRecycler.smoothScrollToPosition(pos + 1)
                        }

                        leftBtn.setOnClickListener {
                            imagesRecycler.smoothScrollToPosition(pos - 1)
                        }

                        if (imagesCount == 1) {
                            leftBtn.hide()
                            rightBtn.hide()
                            imagesCounter.hide()
                        } else {
                            if (pos == 0) {
                                leftBtn.hide()
                            } else {
                                leftBtn.show()
                            }

                            if (pos == imagesCount - 1) {
                                rightBtn.hide()
                            } else {
                                rightBtn.show()
                            }
                        }
                    }
                }
            })

            time.text = getTextForTime(project.createdAt)

            view.setOnClickListener {
                projectClickListener.onProjectClick(project.copy())
            }

            likeBtn.isSelected = project.isLiked

            likeBtn.setOnClickListener {
                projectClickListener.onProjectLikeClick(project.copy())
                if (project.isLiked) {
                    // dislike
                    project.likes = project.likes - 1
                    project.isLiked = false
                    likeBtn.isSelected = false
                    setLikeDislike(project)
                } else {
                    // like
                    project.likes = project.likes + 1
                    project.isLiked = true
                    likeBtn.isSelected = true
                    setLikeDislike(project)
                }
            }

            saveBtn.isSelected = project.isSaved

            saveBtn.setOnClickListener {
                onSaveProjectClick(project)
            }

            when {
                project.isMadeByMe -> {
                    joinBtn.hide()
                }
                project.isRequested -> {
                    joinBtn.show()
                    joinBtn.text = view.context.getString(R.string.undo)
                }
                project.isCollaboration -> {
                    joinBtn.hide()
                }
                else -> joinBtn.show()
            }

            commentBtn.setOnClickListener {
                projectClickListener.onProjectCommentClick(project)
            }

            optionBtn.setOnClickListener {
                projectClickListener.onProjectOptionClick(this, project)
            }

            setJoinButton(project)

        }
    }


    private fun setLikeDislike(project: Project) {
        val likeCommentText = "${getLikesString(project.likes.toInt())} • ${getCommentsString(project.comments.toInt())} • ${getContributorsString(project.contributors.size)}"
        likeComment.text = likeCommentText
    }

    private fun getCommentsString(size: Int): String {
        return if (size == 1) {
            "1 Comment"
        } else {
            "$size Comments"
        }
    }

    private fun getLikesString(size: Int): String{
        return if (size == 1) {
            "1 Like"
        } else {
            "$size Likes"
        }
    }

    private fun getContributorsString(size: Int): String{
        return if (size == 1) {
            "1 Contributor"
        } else {
            "$size Contributors"
        }
    }


    private fun setJoinButton(project: Project) {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            Firebase.firestore.collection("projectRequests")
                .whereEqualTo("projectId", project.id)
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, error.localizedMessage.orEmpty())
                        return@addSnapshotListener
                    }

                    if (value != null) {
                        if (value.isEmpty) {
                            // no requests have been made
                            joinBtn.text = view.context.getString(R.string.join)
                            joinBtn.setOnClickListener {
                                projectClickListener.onProjectJoinClick(project)
                            }
                        } else {
                            val projectRequest = value.toObjects(ProjectRequest::class.java).first()

                            // already requested
                            joinBtn.text = view.context.getString(R.string.undo)
                            joinBtn.setOnClickListener {
                                projectClickListener.onProjectUndoClick(project, projectRequest)
                            }
                        }
                    }
                }

        }
    }

    companion object {
        private const val TAG = "ProjectViewHolder"

        fun newInstance(parent: ViewGroup): ProjectViewHolder {
            return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
        }

    }

}