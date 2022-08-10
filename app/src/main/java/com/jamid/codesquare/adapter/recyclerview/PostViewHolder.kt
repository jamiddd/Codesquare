package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
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
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.databinding.PostItemBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.listeners.PostClickListener
import com.jamid.codesquare.listeners.PostVideoListener
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
) : SuperPostViewHolder(v), ImageClickListener, PostVideoListener {
    init {
        Log.d("Something", "Simple: ")
    }
    // just for external use cases
    lateinit var post: Post
    private lateinit var binding: PostItemBinding
    var shouldShowJoinBtn = true
    var shouldAllowContentClick = true
    private var mediaAdapter: MediaAdapter? = null

    // a click listener for all post related actions
    private val postClickListener = listener ?: view.context as PostClickListener
    var hasAttachedOnce = false

    @Deprecated("All updates must come through database itself")
    var isPartialUpdate = false


    var likeListener: ListenerRegistration? = null
    var saveListener: ListenerRegistration? = null
    var joinListener: ListenerRegistration? = null

    private var counterHideJob: Job? = null



    /**
     * To save or un-save the post. This function saves or un-saves the post and also requests the viewHolder
     * to be laid out again with the updated post
     *
     * */
    fun onSaveBtnClick() {
        isPartialUpdate = true
        postClickListener.onPostSaveClick(post.copy())
    }

    /**
     * To like or dislike the post. This function likes or dislikes the post and also requests the viewHolder
     * to be laid out again with the updated post
     *
     * */
    private fun onLikeBtnClick() {
        isPartialUpdate = true
        postClickListener.onPostLikeClick(post.copy())
    }

    /**
     * To update the viewHolder views related to the creator of the post
     *
     * */
    private fun setPostCreatorInfo() {
        binding.postUserImg.setImageURI(post.creator.photo)
        binding.postUserName.text = post.creator.name

        binding.postUserImg.setOnClickListener {
            postClickListener.onPostCreatorClick(post.copy())
        }

        binding.postUserName.setOnClickListener {
            postClickListener.onPostCreatorClick(post.copy())
        }

        if (post.location.address.isNotBlank()) {
            binding.postLocation.show()

            binding.postLocation.text = post.location.address
            binding.postLocation.show()

            binding.userContainer.invalidate()

            binding.postLocation.setOnClickListener {
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


        /*recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            var totalScroll = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalScroll += abs(dx)

                if (totalScroll > recyclerView.measuredWidth/3) {

                    val pos = if (sign(dx.toFloat()) == 1f) {
                        (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    } else {
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    }

                    if (pos != -1) {
                        cText = "${pos + 1}/${post.mediaString.length}"
                        counterText.text = cText
                    } else {
                        Log.d(TAG, "onScrolled: Couldn't get position")
                    }
                } else {
                    Log.d(TAG, "onScrolled: $totalScroll - ${recyclerView.measuredWidth/5}")
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        counterText.fadeIn().doOnEnd {
                            fadeOutCounterText(counterText)
                        }
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        Log.d(TAG, "onScrollStateChanged: state idle")
                        totalScroll = 0

                        val pos = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                        if (pos != -1) {
                            cText = "${pos + 1}/${post.mediaString.length}"
                            counterText.text = cText
                        }
                    }
                }
            }

        })*/

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

        mediaAdapter = MediaAdapter(post.id, mediaClickListener =  mediaClickListener)
        set()

        setCountText(binding.postImagesCounter)

        val mediaItems = convertMediaListToMediaItemList(post.mediaList, post.mediaString)
        prefetchImages(mediaItems)

        mediaAdapter?.submitList(mediaItems)

    }

    private fun prefetchImages(mediaItems: List<MediaItem>) {
        val imagePipeline = Fresco.getImagePipeline()

        for (item in mediaItems) {
            if (item.type != image) {
                val imageRequest = ImageRequest.fromUri(item.url)
                imagePipeline.prefetchToDiskCache(imageRequest, view.context)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(mPost: Post2?) {
        if (isPartialUpdate) {
            isPartialUpdate = false
            return
        }

        if (mPost is Post2.Collab) {
            binding = PostItemBinding.bind(view)
            view.tag = mPost.post.id
            post = mPost.post
            hasAttachedOnce = true

            setPostCreatorInfo()
            setMediaRecycler()
            setStaticContent()
            setMutableContent()

        }

    }

    private fun setLikeBtn2() {
        setLikeCommentStats()
        likeListener?.remove()
        likeListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "setLikeButton2: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                post.isLiked = value != null && value.exists()

                binding.postLikeBtn.isSelected = post.isLiked

                binding.postLikeBtn.setOnClickListener {
                    onLikeBtnClick()
                }

                setLikeCommentStats()
            }
    }

    private fun setSaveBtn2() {
        saveListener?.remove()
        saveListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(SAVED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "setSaveBtn2: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                post.isSaved = value != null && value.exists()
                binding.postSaveBtn.isSelected = post.isSaved

                binding.postSaveBtn.setOnClickListener {
                    onSaveBtnClick()
                }
            }
    }

    private fun setMutableContent() {
        setLikeBtn2()
        setSaveBtn2()
        setJoinBtn()
    }


    /**
     * To update all the views that are immutable in nature, ie. the views that doesn't
     * have any action
     *
     * */
    private fun setStaticContent() {

        binding.postTitle.text = post.name
        binding.postContent.text = post.content

        binding.postContent.setOnClickListener {
            if (shouldAllowContentClick) {
                postClickListener.onPostClick(post.copy())
            }
        }

        val timeText = " • " + getTextForTime(post.createdAt)
        binding.postTime.text = timeText

        binding.postCommentBtn.setOnClickListener {
            postClickListener.onPostCommentClick(post.copy())
        }


        binding.root.setOnClickListener {
            if (shouldAllowContentClick) {
                postClickListener.onPostClick(post.copy())
            }
        }

        FireUtility.getUser(post.creator.userId) { creator ->
            if (creator != null) {
                binding.root.setOnLongClickListener {
                    if (shouldAllowContentClick) {
                        postClickListener.onPostOptionClick(post.copy(), creator)
                    }
                    true
                }

                binding.postOption.setOnClickListener {
                    postClickListener.onPostOptionClick(post.copy(), creator)
                }

            }
        }

        if (post.rank == -1L) {
            binding.rankedText.hide()
        } else {
            binding.rankedText.show()
            if (post.rank == -2L) {
                binding.rankedText.text = "IN REVIEW"
            } else {
                binding.rankedText.text = "Ranked #${post.rank}"
            }
        }

    }


    /**
     * To update likes, comment and contributors count
     *
     * */
    private fun setLikeCommentStats() {
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
                    isPartialUpdate = true
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

    override fun onVideoBeingPlayed() {
    }

    override fun onVideoPaused() {
    }

    /*fun pause() {
        if (!::binding.isInitialized){
            Log.d(TAG, "pause: Binding is not initialized")
            return
        }

        if (::binding.isInitialized) {
            for (child in binding.postMediaRecycler.children) {
                val vh = binding.postMediaRecycler.findContainingViewHolder(child)
                (vh as? MediaViewHolder)?.pause()
            }
        }
    }*/

    /*fun play() {
        *//*if (!::binding.isInitialized){
            Log.d(TAG, "play: Binding is not initialized")
            return
        }

        if (!binding.root.isVisibleOnScreen()) {
            Log.d(TAG, "play: The view itself is not visible on screen")
            return
        }

        if (::binding.isInitialized) {
            for (child in binding.postMediaRecycler.children) {
                val vh = binding.postMediaRecycler.findContainingViewHolder(child)
                (vh as? MediaViewHolder)?.play()
            }
        }*//*
    }*/

}