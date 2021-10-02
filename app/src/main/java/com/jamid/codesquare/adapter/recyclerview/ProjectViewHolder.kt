package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.viewpager2.widget.ViewPager2
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.ScrollTouchListener


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

    private val projectClickListener = view.context as ProjectClickListener

    fun bind(project: Project?) {
        if (project != null) {

            Log.d(TAG, "${project.likes} -- ${project.isLiked}")

            val creator = project.creator
            val imagesCount = project.images.size
            val ct = "1/$imagesCount"
            imagesCounter.text = ct

            userImg.setImageURI(creator.photo)
            userName.text = creator.name

            if (project.location.address.isNotBlank()) {
                location.text = project.location.address
            } else {
                location.hide()
            }

            title.text = project.title
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
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = ContextCompat.getColor(view.context, R.color.purple_700)
                        }
                    }


                    truncatedSpannableString.setSpan(cs1,
                        startIndex,
                        startIndex + moreString.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    content.movementMethod = LinkMovementMethod.getInstance()

                    content.text = truncatedSpannableString

                    content.setOnClickListener {
                        projectClickListener.onProjectClick(project.copy())
                    }

                    content.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }

            val likeCommentText = "${project.likes} Likes • ${project.comments} Comments • ${project.contributors.size} Contributors"
            likeComment.text = likeCommentText

            val imageAdapter = ImageAdapter {
                projectClickListener.onProjectClick(project.copy())
            }
            val helper: SnapHelper = LinearSnapHelper()

            val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

            imagesRecycler.apply {
                adapter = imageAdapter
                layoutManager = manager
                if (onFlingListener == null) {
                    helper.attachToRecyclerView(this)
                }
            }

            imageAdapter.submitList(project.images)

            imagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val pos = manager.findFirstCompletelyVisibleItemPosition()
                    if (pos != -1) {
                        val counterText = "${pos + 1}/$imagesCount"
                        imagesCounter.text = counterText
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
                if (likeBtn.isSelected) {
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
                projectClickListener.onProjectSaveClick(project.copy())
                saveBtn.isSelected = !saveBtn.isSelected
            }

            joinBtn.setOnClickListener {
                projectClickListener.onProjectJoinClick(project.copy())
            }

            if (project.isMadeByMe) {
                joinBtn.hide()
            }

            if (project.isRequested) {
                joinBtn.show()
                joinBtn.text = "Undo"
            }

            if (project.isCollaboration) {
                joinBtn.hide()
            }

        }
    }

    private fun setLikeDislike(project: Project) {
        val likeCommentText = "${project.likes} Likes • ${project.comments} Comments • ${project.contributors.size} Contributors"
        likeComment.text = likeCommentText
    }

    companion object {
        private const val TAG = "ProjectViewHolder"

        fun newInstance(parent: ViewGroup): ProjectViewHolder {
            return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
        }

    }

}