package com.jamid.codesquare.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentPostBinding
import com.jamid.codesquare.listeners.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*

@ExperimentalPagingApi
class PostFragment : BaseFragment<FragmentPostBinding, MainViewModel>(), ImageClickListener, CommentMiniListener {

    private lateinit var post: Post
    private lateinit var creator: User

    private val postClickListener: PostClickListener by lazy { activity }
    private val userClickListener: UserClickListener by lazy { activity }
    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter

    private var likeListener: ListenerRegistration? = null
    private var saveListener: ListenerRegistration? = null

    private var userLikeListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        post = arguments?.getParcelable(POST) ?: return
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if ((parentFragment as PostFragmentContainer).getCurrentFragmentTag() == TAG) {
            activity.binding.mainToolbar.menu.clear()
            inflater.inflate(R.menu.post_fragment_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.post_menu_option -> {
                val (a, b) = if (post.isMadeByMe) {
                    val option = if (post.archived) {
                        OPTION_13
                    } else {
                        OPTION_12
                    }

                    arrayListOf(OPTION_15, option) to arrayListOf(R.drawable.ic_round_edit_note_24, R.drawable.ic_round_archive_24)
                } else {
                    arrayListOf(OPTION_14) to arrayListOf(R.drawable.ic_round_report_24)
                }

                activity.optionsFragment = OptionsFragment.newInstance(options = a, title = post.name, icons = b, post = post)
                activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post = arguments?.getParcelable(POST) ?: return
        val currentUser = UserManager.currentUser

        // initially not visible until user data is not downloaded
        binding.userLikeBtn.isVisible = false
        getUserData()

        setPostRelatedUi()

        binding.projectCommentBtn.setOnClickListener {
            postClickListener.onPostCommentClick(post)
        }

        if (currentUser.premiumState.toInt() == -1) {
            setCustomAd()
        } else {
            binding.adContainer.root.hide()
        }

        binding.projectRefresher.setDefaultSwipeRefreshLayoutUi()

        binding.projectRefresher.setOnRefreshListener {
            updatePost()
        }

        // join btn slide for scroll change
        if (parentFragment is PostFragmentContainer) {
            (parentFragment as PostFragmentContainer).setJoinBtnForChildScroll(binding.projectFragmentScroll)
        }

//        TODO("Create utility function")
        viewLifecycleOwner.lifecycleScope.launch {

            // waiting for 4 s to update views count for this project
            delay(4000)

            val db = Firebase.firestore
            val projectRef = db.collection(POSTS).document(post.id)
            val ref = projectRef.collection("views").document(UserManager.currentUserId)

            ref.get()
                .addOnSuccessListener {
                    if (!it.exists()) {
                        val batch = Firebase.firestore.batch()

                        batch.set(ref, mapOf("id" to UserManager.currentUserId, "time" to System.currentTimeMillis()))
                        batch.update(projectRef, mapOf("viewsCount" to FieldValue.increment(1), UPDATED_AT to  System.currentTimeMillis()))

                        batch.commit()
                            .addOnSuccessListener {
                                // made required changes
                            }.addOnFailureListener { it1 ->
                                Log.e(TAG, "onViewCreated: ${it1.localizedMessage}")
                            }

                    }
                }.addOnFailureListener {
                    Log.e(TAG, "onViewCreated: ${it.localizedMessage}")
                }
        }

    }

    private fun setCustomAd() {
        val adBinding = binding.adContainer

        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT)
            .build()

        val nativeAdView = adBinding.root

        adBinding.adInfoIcon.setOnClickListener {
            postClickListener.onAdInfoClick()
        }

        val adLoader = AdLoader.Builder(requireContext(), "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { nativeAd ->

                nativeAdView.headlineView = adBinding.adHeadline
                nativeAdView.bodyView = adBinding.adSecondaryText
                nativeAdView.mediaView = adBinding.adMediaView
                nativeAdView.callToActionView = adBinding.adPrimaryAction
                nativeAdView.iconView = adBinding.adAppIcon
                nativeAdView.priceView = adBinding.adPriceText
                nativeAdView.starRatingView = adBinding.adRating
                nativeAdView.advertiserView = adBinding.adAdvertiser

                (nativeAdView.headlineView as TextView).text = nativeAd.headline
                nativeAd.mediaContent?.let {
                    nativeAdView.mediaView?.setMediaContent(it)
                    nativeAdView.mediaView?.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
                }

                if (nativeAd.icon != null) {
                    (nativeAdView.iconView as SimpleDraweeView).setImageURI(nativeAd.icon?.uri.toString())
                }

                if (nativeAd.body == null) {
                    nativeAdView.bodyView?.hide()
                } else {
                    nativeAdView.bodyView?.show()
                    (nativeAdView.bodyView as TextView).text = nativeAd.body
                }

                if (nativeAd.callToAction == null) {
                    nativeAdView.callToActionView?.hide()
                } else {
                    nativeAdView.callToActionView?.show()
                    (nativeAdView.callToActionView as Button).text = nativeAd.callToAction
                }

                if (nativeAd.price == null) {
                    nativeAdView.priceView?.hide()
                } else {
                    nativeAdView.priceView?.show()
                    (nativeAdView.priceView as TextView).text = nativeAd.price
                }

                if (nativeAd.starRating == null) {
                    nativeAdView.starRatingView?.hide()
                } else {
                    nativeAdView.starRatingView?.show()
                    (nativeAdView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                }

                if (nativeAd.advertiser == null) {
                    nativeAdView.advertiserView?.hide()
                } else {
                    (nativeAdView.advertiserView as TextView).text = nativeAd.advertiser
                    nativeAdView.advertiserView?.show()
                }

                nativeAdView.setNativeAd(nativeAd)

                val newText = adBinding.adPrimaryAction.text.toString().lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                adBinding.adPrimaryAction.text = newText

            }
            .withAdListener(object: AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    postClickListener.onAdError(post)
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adBinding.loadingAdText.hide()
                    adBinding.adPrimaryAction.show()
                }
            })
            .withNativeAdOptions(adOptions)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun updatePost() {
        // getting new data of the project
        FireUtility.getPost(post.id) {
            val newPostResult = it ?: return@getPost

            // finished process and loading
            binding.projectRefresher.isRefreshing = false

            // inserting project to local database
            when (newPostResult) {
                is Result.Error -> viewModel.setCurrentError(newPostResult.exception)
                is Result.Success -> {
                    post = processPosts(arrayOf(newPostResult.data))[0]
                    viewModel.insertPosts(post)
                    viewModel.insertPostToCache(post)

                    // Contributors related
                    setContributors()

                    // Comments related
                    setCommentUi()
                }
            }
        }
    }


    /*
    *
    * On a background thread get user who created this project if it's not current user
    * and update the ui accordingly
    *
    * */
    private fun getUserData() = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        getUserImpulsive(post.creator.userId) {
            // now this is main thread
            creator = it
            post.creator = creator.minify()
            updateUserUi()
        }
    }

    private fun updateUserUi() {
        binding.userImg.setImageURI(creator.photo)
        binding.userName.text = creator.name

        binding.userImg.setOnClickListener {
            userClickListener.onUserClick(creator)
        }

        binding.userName.setOnClickListener {
            userClickListener.onUserClick(creator)
        }

        setLikeListener()

    }

    private fun setUserLikeBtn() {
        if (creator.isLiked) {
            binding.userLikeBtn.text = getString(R.string.dislike)
        } else {
            binding.userLikeBtn.text = getString(R.string.like)
        }

        binding.userLikeBtn.isVisible = !post.isMadeByMe
        binding.userLikeBtn.isSelected = creator.isLiked

        binding.userLikeBtn.setOnClickListener {
            userClickListener.onUserLikeClick(creator.copy())

            if (creator.isLiked) {
                creator.isLiked = false
                creator.likesCount -= 1
            } else {
                creator.isLiked = true
                creator.likesCount += 1
            }

            setUserLikeBtn()
        }
    }


    private fun setLikeListener() {
        userLikeListenerRegistration?.remove()
        userLikeListenerRegistration = Firebase
            .firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_USERS)
            .document(creator.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    binding.userLikeBtn.hide()
                    return@addSnapshotListener
                }

                creator.isLiked = value != null && value.exists()
                setUserLikeBtn()
            }
    }


    private fun setPostRelatedUi() {

        // make static changes here
        setImmutableProperties()

        checkForExpiry()

        setLikeCommentStats()

        likeListener?.remove()
        likeListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "setPostRelatedUi: ${error.localizedMessage}")
                }

                post.isLiked = value != null && value.exists()

                setPostLikeBtn()
            }

        saveListener?.remove()
        saveListener = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(SAVED_POSTS)
            .document(post.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "setPostRelatedUi: ${error.localizedMessage}")
                }

                post.isSaved = value != null && value.exists()

                setPostSaveBtn()
            }

    }

    private fun setPostLikeBtn() {

        // Like button related code
        binding.projectLikeBtn.apply {

            isSelected = post.isLiked

            setOnClickListener {
                postClickListener.onPostLikeClick(post.copy()) {
                    post = it
                    setLikeCommentStats()
                    setPostLikeBtn()
                }
            }
        }
    }

    private fun setPostSaveBtn() {
        // Save button related code
        binding.projectSaveBtn.apply {
            isSelected = post.isSaved

            setOnClickListener {
                postClickListener.onPostSaveClick(post.copy()) {
                    post = it

                    setPostSaveBtn()
                }
            }
        }
    }

    private fun addLinks(links: List<String>) {
        for (link in links) {
            addLink(link)
        }
    }

    private fun addLink(link: String) {
        val lContext = requireContext()

        val chip = View.inflate(lContext, R.layout.action_chip, null) as Chip

        val (requireDots, len) = if (link.length > 16) {
            true to 16
        } else {
            false to link.length
        }

        val shortLink = link.take(len) + if (requireDots) "..." else ""
/*
        val darkGreenColor = ContextCompat.getColor(lContext, R.color.green_dark)
        val darkGreenNightColor = ContextCompat.getColor(lContext, R.color.green_dark_night)
        val lightGreenColor = ContextCompat.getColor(lContext, R.color.green_light)
        val textColor = if (isNightMode()) darkGreenNightColor else lightGreenColor
*/

        chip.apply {
            text = shortLink
//            chipIconTint = ColorStateList.valueOf(darkGreenColor)
            chipIcon = ContextCompat.getDrawable(lContext, R.drawable.forward_icon)
            chipIconSize = resources.getDimension(R.dimen.large_len)
            isChipIconVisible = true
            isCheckable = false
            chipStrokeWidth = 0f
            isCloseIconVisible = false
//            chipBackgroundColor = ColorStateList.valueOf(textColor)
            binding.projectLinks.addView(this)

//            setTextColor(darkGreenColor)

            setOnClickListener {
                if (link.startsWith("https://") || link.startsWith("http://")) {
                    activity.onLinkClick(link)
                } else {
                    activity.onLinkClick("https://$link")
                }
            }
        }

    }

    private fun addTags(tags: List<String>) {
        for (tag in tags) {
            addTag(tag)
        }
    }

    private fun addTag(tag: String) {

        tag.trim()

        // local context
        val lContext = requireContext()
        val chip = View.inflate(lContext, R.layout.action_chip, null) as Chip

        /*val (backgroundColor, textColor) = if (isNightMode()) {
            val colorPair = colorPalettesNight.random()
            ContextCompat.getColor(lContext, colorPair.first) to
            ContextCompat.getColor(lContext, colorPair.second)
        } else {
            val colorPair = colorPalettesDay.random()
            ContextCompat.getColor(lContext, colorPair.first) to
            ContextCompat.getColor(lContext, colorPair.second)
        }*/

        chip.apply {
            text = tag
            isCheckable = false
            isCloseIconVisible = false
            binding.projectTags.addView(this)
//            chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            chipStrokeWidth = 0f
//            setTextColor(textColor)
            setOnClickListener {
                (parentFragment as PostFragmentContainer).navigate(TagFragment.TAG, bundleOf(TITLE to "#$tag", "tag" to tag, SUB_TITLE to "Posts related to ${post.name.uppercase()}"))
            }

            setOnLongClickListener {
                val choices = arrayListOf(OPTION_28)
                val icons = arrayListOf(R.drawable.ic_round_add_24)

                activity.optionsFragment = OptionsFragment.newInstance(title = "\"$tag\"", options = choices, icons = icons, tag = tag)
                activity.optionsFragment?.show(activity.supportFragmentManager, OptionsFragment.TAG)

                true
            }
        }

    }

    private fun onContributorsFetched(contributors: List<User>) = activity.runOnUiThread {
        userAdapter.submitList(contributors)

        Log.d(TAG, "onContributorsFetched: $contributors")

        val list = arrayListOf<User>()
        for (item in contributors) {
            list.add(item)
        }

        binding.seeAllContributorsBtn.isEnabled = true
        binding.seeAllContributorsBtn.setOnClickListener {
            (parentFragment as PostFragmentContainer).navigate(PostContributorsFragment.TAG, bundleOf(
                POST to post, TITLE to "Contributors", SUB_TITLE to post.name.uppercase()))
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    private fun setContributors() {

        binding.seeAllContributorsBtn.isEnabled = false

        userAdapter = UserAdapter(small = true)

        binding.projectContributorsRecycler.apply {
            adapter = userAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
        }

        Firebase.firestore.collection(USERS)
            .whereArrayContains(COLLABORATIONS, post.id)
            .limit(5)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {
                    val contributors = mutableListOf<User>()
                    val users = it.toObjects(User::class.java)
                    contributors.addAll(users)

                    activity.getUserImpulsive(post.creator.userId) { it1 ->
                        contributors.add(it1)
                        onContributorsFetched(contributors)
                    }
                } else {
                    activity.getUserImpulsive(post.creator.userId) { it1 ->
                        onContributorsFetched(listOf(it1))
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "setContributors: ${it.localizedMessage}")
                Snackbar.make(binding.root, "Something went wrong while trying to fetch contributors. Try again later ..", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                    .show()
            }
    }

    private fun setCommentUi(commentChannel: CommentChannel) {

        binding.commentLayoutProgress.hide()

        val lastComment = commentChannel.lastComment
        if (lastComment != null) {

            Firebase.firestore.collection(COMMENT_CHANNELS)
                .document(commentChannel.commentChannelId)
                .collection(COMMENTS)
                .document(lastComment.commentId)
                .collection("likedBy")
                .document(UserManager.currentUserId)
                .get()
                .addOnSuccessListener {
                    lastComment.isLiked = it.exists()
                }.addOnFailureListener {
                    Log.e(TAG, "setCommentUi: ${it.localizedMessage}")
                }

//            onCheckForStaleData(commentChannel, lastComment)

            binding.projectsLastComment.root.show()
            binding.commentsHeader.show()

            // show see all comments only if there are more than two comments
            if (post.commentsCount.toInt() <= 1) {
                binding.seeAllComments.hide()
            } else {
                binding.seeAllComments.show()
            }

            binding.projectsLastComment.commentLikeBtn.hide()
            binding.projectsLastComment.commentReplyBtn.hide()
            binding.projectsLastComment.commentLikesReplies.hide()

            binding.seeAllComments.setOnClickListener {
                postClickListener.onPostCommentClick(post)
            }

            binding.projectsLastComment.apply {

                commentUserName.text = lastComment.sender.name

                commentUserImg.setImageURI(lastComment.sender.photo)

                val timeText = " • " + getTextForTime(lastComment.createdAt)
                commentTime.text =  timeText

                commentContent.text = lastComment.content

                commentLikeBtn.isSelected = lastComment.isLiked

                val likeRepliesText = "${lastComment.likesCount} Likes • ${lastComment.repliesCount} Replies"
                commentLikesReplies.text = likeRepliesText

                val isCommentByMe = lastComment.senderId == UserManager.currentUserId
                if (isCommentByMe) {
                    commentOptionBtn.hide()
                }

                commentOptionBtn.setOnClickListener {
                    onOptionClick(lastComment)
                }

                root.setOnClickListener {
                    postClickListener.onPostCommentClick(post)
                }
            }

        } else {
            onEmptyComments()
        }
    }

    private fun getLikesString(size: Int): String {
        return if (size == 1) {
            "1 Like"
        } else {
            "$size Likes"
        }
    }

    private fun getCommentsString(size: Int): String {
        return if (size == 1) {
            "1 Comment"
        } else {
            "$size Comments"
        }
    }

    private fun setLikeCommentStats() {
        // setting like comment text

        val likesString = getLikesString(post.likesCount.toInt())
        val commentsString = getCommentsString(post.commentsCount.toInt())

        val likeCommentText =
            "$likesString • $commentsString"

        val cs1 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                postClickListener.onPostSupportersClick(post)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val color = if (isNightMode()) {
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
                val color = if (isNightMode()) {
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

        val formattedString = SpannableString(likeCommentText)
        formattedString.setSpan(cs1, s1, e1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        formattedString.setSpan(cs2, s2, e2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.projectLikeCommentText.movementMethod = LinkMovementMethod.getInstance()

        binding.projectLikeCommentText.text = formattedString
    }

    private fun onEmptyComments() {
        binding.projectsLastComment.root.hide()
        binding.commentsHeader.hide()
        binding.seeAllComments.hide()
        binding.commentLayoutProgress.hide()
    }

    private fun setCommentUi() {
        FireUtility.getCommentChannel(post.commentChannel) {
            val result = it ?: return@getCommentChannel

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "setCommentUi: ${result.exception.localizedMessage}")
                    onEmptyComments()
                }
                is Result.Success -> {
                    val commentChannel = result.data
                    setCommentUi(commentChannel)
                }
            }
        }
    }

    /**
     * Deletion of project on expiry should be done by the server and not client but for the time
     * being it is being checked every time a project is opened.
     *
     *
    * */
    private fun checkForExpiry() {
        // check if the project is expired
        val now = System.currentTimeMillis()
        if (post.archived && post.expiredAt.toInt() != -1 && post.expiredAt < now) {
            // delete project
            FireUtility.deletePost(post) {
                if (it.isSuccessful) {
                    viewModel.deleteLocalPost(post)
                    viewModel.deleteLocalChatChannelById(post.chatChannel)

                    findNavController().navigateUp()

                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }


    /**
     * All immutable UI changes
    * */
    private fun setImmutableProperties() {

        val timeText = " • " + getTextForTime(post.createdAt)
        binding.projectTime.text = timeText
        binding.projectName.text = post.name
        binding.projectContent.text = post.content

        if (post.location.address.isNotBlank()) {
            binding.projectLocation.text = post.location.address
        } else {
            binding.projectLocation.hide()
        }

        binding.projectLocation.setOnClickListener {
            findNavController().navigate(R.id.locationPostsFragment, bundleOf(TITLE to "Showing projects near", SUB_TITLE to post.location.address, "location" to post.location), slideRightNavOptions())
        }

        setPostImages()

        // Tags related
        if (post.tags.isNotEmpty()) {
            binding.tagsHeader.show()
            binding.projectTags.show()
            addTags(post.tags)
        } else {
            binding.tagsHeader.hide()
            binding.projectTags.hide()
        }

        // links related
        if (post.sources.isNotEmpty()) {
            binding.linksHeader.show()
            binding.projectLinks.show()
            addLinks(post.sources)
        } else {
            binding.linksHeader.hide()
            binding.projectLinks.hide()
        }

        // Contributors related
        setContributors()

        // Comments related
        setCommentUi()
    }

    /**
     * Post image related code
     * */
    private fun setPostImages() {
        val manager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImageAdapter(this)

        val helper = LinearSnapHelper()

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = manager
            onFlingListener = null
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

            helper.attachToRecyclerView(this)
        }

        val totalImageCount = post.images.size
        if (totalImageCount == 1) {
            binding.imagesCounter.hide()
        } else {
            val imageCount = "1/$totalImageCount"
            binding.imagesCounter.text = imageCount
        }

        imageAdapter.submitList(post.images)

        binding.projectImagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val pos = manager.findFirstCompletelyVisibleItemPosition()
                if (pos != -1) {
                    val imageCount = "${pos + 1}/$totalImageCount"
                    binding.imagesCounter.text = imageCount
                }
            }
        })

    }

    companion object {
        const val TAG = "PostFragment"

        fun newInstance(bundle: Bundle) = PostFragment().apply {
            arguments = bundle
        }

    }

    override fun onImageClick(view: View, image: Image) {
        activity.showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {

    }

    override fun onOptionClick(comment: Comment) {
        val isCommentByMe = comment.senderId == UserManager.currentUserId
        val name = comment.sender.name

        if (isCommentByMe)
            return

        val choices = arrayListOf(OPTION_29)
        val icons = arrayListOf(R.drawable.ic_round_report_24)

        activity.optionsFragment = OptionsFragment.newInstance(title = "Comment by $name", options = choices, icons = icons, comment = comment)
        activity.optionsFragment?.show(activity.supportFragmentManager, OptionsFragment.TAG)

    }

    override fun getViewBinding(): FragmentPostBinding {
        return FragmentPostBinding.inflate(layoutInflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // saving the changes made to the user
        viewModel.insertUser(creator)

        // saving the changes made to the post
        viewModel.insertPost(post)

        userLikeListenerRegistration?.remove()
        likeListener?.remove()
        saveListener?.remove()
    }

}