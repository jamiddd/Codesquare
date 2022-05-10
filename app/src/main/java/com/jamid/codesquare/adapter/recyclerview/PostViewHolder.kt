package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.PostItemBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.PostClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class PostViewHolder(val v: View): SuperPostViewHolder(v), ImageClickListener {

    // just for external use cases
    lateinit var post: Post
    private lateinit var binding: PostItemBinding

    private var isTextExpanded = false

    // a click listener for all post related actions
    private val postClickListener = view.context as PostClickListener
    var hasAttachedOnce = false

    var isPartialUpdate = false


    var likeListener: ListenerRegistration? = null
    var saveListener: ListenerRegistration? = null
    var joinListener: ListenerRegistration? = null

    /**
     * To save or un-save the post. This function saves or un-saves the post and also requests the viewHolder
     * to be laid out again with the updated post
     *
     * */
    fun onSaveBtnClick() {
        isPartialUpdate = true
        postClickListener.onPostSaveClick(post.copy()) {
            //
        }
    }

    /**
     * To like or dislike the post. This function likes or dislikes the post and also requests the viewHolder
     * to be laid out again with the updated post
     *
     * */
    private fun onLikeBtnClick() {
        isPartialUpdate = true
        postClickListener.onPostLikeClick(post.copy()) {
            post = it
            setLikeCommentStats()
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
            postClickListener.onPostCreatorClick(post.copy())
        }

        binding.postUserName.setOnClickListener {
            postClickListener.onPostCreatorClick(post.copy())
        }

        if (post.location.address.isNotBlank()) {
            binding.postLocation.text = post.location.address

            binding.postLocation.setOnClickListener {
                postClickListener.onPostLocationClick(post.copy())
            }
        } else {
            binding.postLocation.hide()
        }
    }


    /**
     * To implement a recyclerView with post images
     *
     * */
    private fun setImagesRecycler() {

        val imageAdapter = ImageAdapter(this)
        val helper = LinearSnapHelper()

        val manager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

        binding.postImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = manager
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
            if (onFlingListener == null) {
                helper.attachToRecyclerView(this)
            }
        }

        val imagePipeline = Fresco.getImagePipeline()

        for (image in post.images) {
            val imageRequest = ImageRequest.fromUri(image)
            imagePipeline.prefetchToDiskCache(imageRequest, view.context)
        }

        imageAdapter.submitList(post.images)

        if (post.images.size == 1) {
            binding.postImagesCounter.hide()
        } else {
            binding.postImagesCounter.show()
        }

        binding.postImagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = manager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                     val counterText = "${pos + 1}/${post.images.size}"
                    binding.postImagesCounter.text = counterText

                    binding.rightBtn.setOnClickListener {
                        binding.postImagesRecycler.smoothScrollToPosition(pos + 1)
                    }

                    binding.leftBtn.setOnClickListener {
                        binding.postImagesRecycler.smoothScrollToPosition(pos - 1)
                    }

                    if (post.images.size == 1) {
                        binding.leftBtn.hide()
                        binding.rightBtn.hide()
                        binding.postImagesCounter.hide()
                    } else {
                        if (pos == 0) {
                            binding.leftBtn.hide()
                        } else {
                            binding.leftBtn.show()
                        }

                        if (pos == post.images.size - 1) {
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
    override fun bind(mPost: Post?) {
        if (isPartialUpdate) {
            isPartialUpdate = false
            return
        }

        if (mPost != null) {

            binding = PostItemBinding.bind(view)
            view.tag = mPost.id

            post = mPost
            hasAttachedOnce = true

            Log.d(TAG, "bind: Invoked ${post.isLiked}")


            setPostCreatorInfo()

            setImagesRecycler()

            setStaticContent()

            setMutableContent()

            checkForStaleData()

        }
    }

    private fun checkForStaleData() = view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch (Dispatchers.IO) {
        postClickListener.onCheckForStaleData(post.copy()) {
            bind(it)
        }
    }

    fun setLikeBtn2() {
        Log.d(TAG, "setLikeButton2: Setting like btn called")
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

                /*
                no need for this as this is handled by external functions

                postClickListener.onPostUpdate(post.copy())
                */

                binding.postLikeBtn.isSelected = post.isLiked

                binding.postLikeBtn.setOnClickListener {
                    onLikeBtnClick()
                }

                setLikeCommentStats()
            }
    }

    /*private fun setLikeButton() {
        Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_POSTS)
            .document(post.id)
            .get()
            .addOnSuccessListener {
                post.isLiked = it.exists()
                postClickListener.onPostUpdate(post)
                binding.postLikeBtn.isSelected = post.isLiked

                binding.postLikeBtn.setOnClickListener {
                    onLikeBtnClick()
                }

                setLikeCommentStats()
            }.addOnFailureListener {
                Log.e(TAG, "setMutableContent: ${it.localizedMessage}")
            }
    }*/

    fun setSaveBtn2() {
        saveListener?.remove()
        saveListener = Firebase.firestore.collection(USERS).document(UserManager.currentUserId)
            .collection(SAVED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "setSaveBtn2: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                post.isSaved = value != null && value.exists()

//                postClickListener.onPostUpdate(post.copy())

                binding.postSaveBtn.isSelected = post.isSaved

                binding.postSaveBtn.setOnClickListener {
                    onSaveBtnClick()
                }
            }
    }

    private fun setSaveButton() {
        Firebase.firestore.collection(USERS).document(UserManager.currentUserId)
            .collection(SAVED_POSTS)
            .document(post.id)
            .get()
            .addOnSuccessListener {
                post.isSaved = it.exists()
                postClickListener.onPostUpdate(post.copy())
                binding.postSaveBtn.isSelected = post.isSaved

                binding.postSaveBtn.setOnClickListener {
                    onSaveBtnClick()
                }
            }.addOnFailureListener {
                Log.e(TAG, "setMutableContent: ${it.localizedMessage}")
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

//        post.content = view.context.getString(R.string.large_text)

        binding.postTitle.text = post.name
        binding.postContent.text = post.content

        binding.postContent.setOnClickListener {
            postClickListener.onPostClick(post.copy())
        }

        /*binding.postContent.doOnLayout {
            if (binding.postContent.lineCount > MAX_LINES && !isTextExpanded) {
                val lastCharShown = binding.postContent.layout.getLineVisibleEnd(MAX_LINES - 1)
                binding.postContent.maxLines = MAX_LINES
                val moreString = "Show more"
                val suffix = "  $moreString"

                val actionDisplayText: String = post.content.substring(0, lastCharShown - suffix.length - 3) + "..." + suffix
                val truncatedSpannableString = SpannableString(actionDisplayText)
                val startIndex = actionDisplayText.indexOf(moreString)

                val cs = object: ClickableSpan() {

                    override fun onClick(p0: View) {
                        postClickListener.onPostClick(post.copy())
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

                        isTextExpanded = true

                        binding.postContent.maxLines = Int.MAX_VALUE
                        binding.postContent.text = post.content

                        view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {

                            delay(200)

                            binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }

                            binding.postContent.text = post.content

                            delay(200)

                            binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }

                            binding.postContent.setOnClickListener {
                                postClickListener.onPostClick(post.copy())
                            }
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        val greyColor = ContextCompat.getColor(view.context, R.color.darker_grey)
                        ds.color = greyColor
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

                binding.postContent.movementMethod = LinkMovementMethod.getInstance()

                binding.postContent.text = truncatedSpannableString

                view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                    delay(200)
                    binding.postContent.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }*/

        val timeText = " • " + getTextForTime(post.createdAt)
        binding.postTime.text = timeText

        binding.postCommentBtn.setOnClickListener {
            postClickListener.onPostCommentClick(post.copy())
        }

        binding.postOption.setOnClickListener {
            postClickListener.onPostOptionClick(post.copy())
        }

        binding.root.setOnClickListener {
            postClickListener.onPostClick(post.copy())
        }

        binding.root.setOnLongClickListener {
            postClickListener.onPostOptionClick(post.copy())
            true
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

        val cs1 = object: ClickableSpan() {
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

        val cs2 = object: ClickableSpan() {
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

        val cs3 = object: ClickableSpan() {
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
            "1 Contributor"
        } else {
            "$size Contributors"
        }
    }

    /**
     * Special function made for join button as it is very crucial for proper working
     *
     * */
    private fun setJoinBtn() {

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
                binding.postJoinBtn.setOnClickListener {
                    isPartialUpdate = true
                    postClickListener.onPostUndoClick(post.copy()) { newPost ->
                        post = newPost
                        setJoinBtn()
                    }
                }
            } else {
                binding.postJoinBtn.text = view.context.getString(R.string.join)
                binding.postJoinBtn.setOnClickListener {
                    isPartialUpdate = true
                    postClickListener.onPostJoinClick(post.copy()) { newPost ->
                        post = newPost
                        setJoinBtn()
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG = "PostViewHolder"

        fun newInstance(parent: ViewGroup): PostViewHolder {
            return PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false))
        }

    }

    override fun onImageClick(view: View, image: Image) {
        postClickListener.onPostClick(post)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        //
    }

}