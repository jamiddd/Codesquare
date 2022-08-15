package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Color
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.databinding.PostItemBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.listeners.PostClickListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import kotlin.math.abs

class PostViewHolder(
    val v: View,
    private val lifecycleOwner: LifecycleOwner,
    private val mediaClickListener: MediaClickListener,
    listener: PostClickListener? = null
) : SuperPostViewHolder(v), ImageClickListener {

    // just for external use cases
    lateinit var post: Post
    lateinit var binding: PostItemBinding
    var shouldShowJoinBtn = true
    var shouldAllowContentClick = true
    private var mediaAdapter: MediaAdapter? = null

    // a click listener for all post related actions
    private val postClickListener = listener ?: view.context as PostClickListener
    private var counterHideJob: Job? = null

    /**
     * To save or un-save the post. This function saves or un-saves the post and also requests the viewHolder
     * to be laid out again with the updated post
     *
     * */
    fun onSaveBtnClick() {
        postClickListener.currentViewHolder = this

        // making sure that we are sending a copy and not the actual reference
        postClickListener.onPostSaveClick(post.copy())

        // this is just to update the ui
        if (post.isSaved) {
            post.isSaved = false
            binding.postSaveBtn.isSelected = false
        } else {
            post.isSaved = true
            binding.postSaveBtn.isSelected = true
        }
    }

    /**
     * To update the viewHolder views related to the creator of the post
     *
     * */
    private fun setPostCreatorInfo() {
        binding.postUserImg.setImageURI(post.creator.photo)
        binding.postUserName.text = post.creator.name

        binding.postUserImg.setOnClickListener {
            postClickListener.currentViewHolder = this
            postClickListener.onPostCreatorClick(post.copy())
        }

        binding.postUserName.setOnClickListener {
            postClickListener.currentViewHolder = this
            postClickListener.onPostCreatorClick(post.copy())
        }

        if (post.location.address.isNotBlank()) {
            binding.postLocation.show()

            binding.postLocation.text = post.location.address
            binding.postLocation.show()

            binding.userContainer.invalidate()

            binding.postLocation.setOnClickListener {
                postClickListener.currentViewHolder = this
                postClickListener.onPostLocationClick(post.copy())
            }
        } else {
            binding.postLocation.hide()
        }
    }


    private fun setCountText(counterText: TextView) {

        if (post.mediaString.length == 1) {
            counterText.hide()
            return
        }

        val cText = "1/${post.mediaString.length}"
        counterText.text = cText
        counterText.show()

        fadeOutCounterText(counterText)

    }


    private fun fadeOutCounterText(counterText: TextView) {
        counterHideJob?.cancel()
        counterHideJob = lifecycleOwner.lifecycleScope.launch {
            delay(5000)

            // after 5 seconds fadeout the text
            counterText.fadeOut()
        }
    }


   private var mScrollTouchListener: RecyclerView.OnItemTouchListener =
        object : RecyclerView.OnItemTouchListener {
            var x1 = 0f
            var x2 = 0f
            var y1 = 0f
            var y2 = 0f
            var dx = 0f
            var dy = 0f

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x1 = e.x
                        y1 = e.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        x2 = e.x
                        y2 = e.y
                        dx = x2-x1
                        dy = y2-y1

                        // Use dx and dy to determine the direction of the move
                        if(abs(dx) > abs(dy)) {
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        } else {
                            rv.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
//                    MotionEvent.ACTION_MOVE -> rv.parent.parent.requestDisallowInterceptTouchEvent(true)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }

    /**
     * To implement a recyclerView with post images
     *
     * */
    private fun setMediaRecycler() {
        fun set() {
            val helper = LinearSnapHelper()
            val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

            manager.recycleChildrenOnDetach = true

            binding.postMediaRecycler.apply {

                layoutManager = manager
                mLifecycleOwner = lifecycleOwner

                setMediaCounterText(binding.postImagesCounter)
                setMediaObjects(post.mediaList)

                adapter = mediaAdapter
                itemAnimator = null
                addOnItemTouchListener(mScrollTouchListener)

                OverScrollDecoratorHelper.setUpOverScroll(
                    this,
                    OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
                )

                if (onFlingListener == null) {
                    helper.attachToRecyclerView(this)
                }
            }
        }


        if (mediaAdapter == null) {
            mediaAdapter = MediaAdapter(post.id, mediaClickListener =  mediaClickListener)
            set()
        }

        setCountText(binding.postImagesCounter)

        val mediaItems = convertMediaListToMediaItemList(post.mediaList, post.mediaString)

        mediaAdapter?.submitList(mediaItems)

    }

    override fun bind(mPost: Post2?) {
        if (mPost is Post2.Collab) {
            binding = PostItemBinding.bind(view)
            view.tag = mPost.post.id

            post = mPost.post
            setPostCreatorInfo()
            setMediaRecycler()
            setStaticContent()
            setLikeBtn3()
            setMetadataText()
            setSaveBtn3()
            setJoinBtn()

            Log.d(TAG, "bind: ${mPost.post.name}")
        }
    }

    private fun setLikeBtn3() {
        FireUtility.checkIfPostLiked(post) { isLiked: Boolean ->
            post.isLiked = isLiked

            binding.postLikeBtn.isSelected = post.isLiked

            binding.postLikeBtn.setOnClickListener {

                postClickListener.currentViewHolder = this

                // making sure that we are sending a copy and not the actual reference
                postClickListener.onPostLikeClick(post.copy())

                // this is just to update the ui
                if (post.isLiked) {
                    post.likesCount--
                    post.isLiked = false
                    binding.postLikeBtn.isSelected = false
                } else {
                    post.likesCount++
                    post.isLiked = true
                    binding.postLikeBtn.isSelected = true
                }

                // update the like comment text
                setMetadataText()

            }
        }
    }

    /*private fun setLikeBtn2() {
        setLikeCommentStats()
        likeListener?.remove()
        likeListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                post.isLiked = value != null && value.exists()

                binding.postLikeBtn.isSelected = post.isLiked

                binding.postLikeBtn.setOnClickListener {
                    onLikeBtnClick()
                }

                setLikeCommentStats()
            }
    }*/

    private fun setSaveBtn3() {
        FireUtility.checkIfPostSaved(post) { isSaved: Boolean ->
            post.isSaved = isSaved

            binding.postSaveBtn.isSelected = post.isSaved

            binding.postSaveBtn.setOnClickListener {
                onSaveBtnClick()
            }
        }
    }

    /*private fun setSaveBtn2() {
        saveListener?.remove()
        saveListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(SAVED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                post.isSaved = value != null && value.exists()
                binding.postSaveBtn.isSelected = post.isSaved

                binding.postSaveBtn.setOnClickListener {
                    onSaveBtnClick()
                }
            }
    }*/

    /**
     * To update all the views that are immutable in nature, ie. the views that doesn't
     * have any action
     *
     * */
    private fun setStaticContent() {

        binding.postTitle.text = post.name
        binding.postContent.text = post.content

        val timeText = " • " + getTextForTime(post.createdAt)
        binding.postTime.text = timeText

        binding.postCommentBtn.setOnClickListener {
            postClickListener.currentViewHolder = this
            postClickListener.onPostCommentClick(post.copy())
        }


        if (shouldAllowContentClick) {
            binding.root.setOnClickListener {
                postClickListener.currentViewHolder = this
                postClickListener.onPostClick(post.copy())
            }
        } else {
            binding.root.background = null
        }


        FireUtility.getUser(post.creator.userId) { creator ->
            if (creator != null) {
                if (shouldAllowContentClick) {
                    binding.root.setOnLongClickListener {
                        postClickListener.currentViewHolder = this
                        postClickListener.onPostOptionClick(post.copy(), creator)
                        true
                    }
                }


                binding.postOption.setOnClickListener {
                    postClickListener.currentViewHolder = this
                    postClickListener.onPostOptionClick(post.copy(), creator)
                }

            }
        }

        if (post.rank == -1L) {
            binding.rankedText.hide()
        } else {
            binding.rankedText.show()
            if (post.rank == -2L) {
                binding.rankedText.text = view.context.getString(R.string.in_review)
            } else {
                val rt = "Ranked #${post.rank}"
                binding.rankedText.text = rt
            }
        }

    }


    private fun setMetadataText() {
        val likesString = getLikesString(post.likesCount.toInt())
        val commentsString = getCommentsString(post.commentsCount.toInt())
        val contributorsString = getContributorsString(post.contributors.size)
        val likeCommentText = "$likesString • $commentsString • $contributorsString"

        // this is done so that, it is visible even when the functionality is not done
        binding.postLikeCommentText.text = likeCommentText

        val cs1 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostSupportersClick(post)
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

        val cs2 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostCommentClick(post)
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

        val cs3 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostContributorsClick(post)
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

        binding.postLikeCommentText.movementMethod = LinkMovementMethod.getInstance()

        binding.postLikeCommentText.text = formattedString
    }

    /*private fun setLikeCommentStats() {
        val likesString = getLikesString(post.likesCount.toInt())
        val commentsString = getCommentsString(post.commentsCount.toInt())
        val contributorsString = getContributorsString(post.contributors.size)
        val likeCommentText = "$likesString • $commentsString • $contributorsString"

        val cs1 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostSupportersClick(post)
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

        val cs2 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostCommentClick(post)
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

        val cs3 = object : ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostContributorsClick(post)
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

        binding.postLikeCommentText.movementMethod = LinkMovementMethod.getInstance()

        binding.postLikeCommentText.text = formattedString
    }*/

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
            "1 Person"
        } else {
            "$size People"
        }
    }

    /**
     * Special function made for join button as it is very crucial for proper working
     *
     * */
    private fun setJoinBtn() {

        if (shouldShowJoinBtn) {
            when {
                post.isMadeByMe -> {
                    binding.postJoinBtn.hide()
                }
                post.isCollaboration -> {
                    binding.postJoinBtn.hide()
                }
                post.isRequested -> {
                    binding.postJoinBtn.show()
                    binding.postJoinBtn.text = view.context.getString(R.string.undo)
                }
                else -> binding.postJoinBtn.show()
            }

            if (post.isBlocked) {
                binding.postJoinBtn.hide()
            } else {
                if (post.isRequested) {
                    binding.postJoinBtn.text = view.context.getString(R.string.undo)
                } else {
                    binding.postJoinBtn.text = view.context.getString(R.string.join)
                }

                binding.postJoinBtn.setOnClickListener {
                    postClickListener.currentViewHolder = this
                    postClickListener.onPostJoinClick(post.copy())
                }
            }
        } else {
            binding.postJoinBtn.hide()
        }

    }

    companion object {

        const val TAG = "PostViewHolder"

        fun newInstance(
            parent: ViewGroup,
            lifecycleOwner: LifecycleOwner,
            mediaClickListener: MediaClickListener
        ): PostViewHolder {
            return PostViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false),
                lifecycleOwner,
                mediaClickListener
            )
        }

    }

    override fun onImageClick(view: View, image: Image) {
        postClickListener.onPostClick(post)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
    }


}