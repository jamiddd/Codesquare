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
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentProjectBinding
import com.jamid.codesquare.listeners.*
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class ProjectFragment : Fragment(), ImageClickListener, CommentMiniListener {

    private lateinit var binding: FragmentProjectBinding
    private lateinit var project: Project
    private var lr: ListenerRegistration? = null
    private val projectClickListener: ProjectClickListener by lazy { requireActivity() as ProjectClickListener }
    private val userClickListener: UserClickListener by lazy { requireActivity() as UserClickListener }
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter

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

        project = arguments?.getParcelable(PROJECT) ?: return

        val currentUser = UserManager.currentUser

        setCreatorRelatedUi(project.creator)

        setProjectRelatedUi(project)

        binding.projectCommentBtn.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }

        if (currentUser.premiumState.toInt() == -1) {
            setAdView()
        } else {
            binding.adView.hide()
        }

        binding.projectRefresher.setOnRefreshListener {
            updateProject()
        }

        // to remove any issues related to saved project being
        // out of date and not matching with the original document
        if (project.isSaved) {
            updateProject()
        }

        // join btn slide for scroll change
        if (parentFragment is ProjectFragmentContainer) {
            (parentFragment as ProjectFragmentContainer).setJoinBtnForChildScroll(binding.projectFragmentScroll)
        }

    }

    private fun updateProject() {
        // getting new data of the project
        FireUtility.getProject(project.id) {
            val newProjectResult = it ?: return@getProject

            // finished process and loading
            binding.projectRefresher.isRefreshing = false

            // inserting project to local database
            when (newProjectResult) {
                is Result.Error -> viewModel.setCurrentError(newProjectResult.exception)
                is Result.Success -> {
                    project = processProjects(arrayOf(newProjectResult.data))[0]
                    viewModel.insertProjects(project)
                    viewModel.insertProjectToCache(project)
                }
            }
        }
    }

    private fun setCreatorRelatedUi(mCreator: UserMinimal) {

        // set immutable content
        binding.userImg.setImageURI(mCreator.photo)
        binding.userName.text = mCreator.name

        // checking for changes in the user data
        projectClickListener.onCheckForStaleData(project)

        // set mutable content
        viewModel.getReactiveUser(mCreator.userId).observe(viewLifecycleOwner) { creator ->
            if (creator != null) {

                binding.userImg.setOnClickListener {
                    userClickListener.onUserClick(creator)
                }

                binding.userName.setOnClickListener {
                    userClickListener.onUserClick(creator)
                }

                if (creator.isLiked) {
                    binding.userLikeBtn.text = getString(R.string.dislike)
                } else {
                    binding.userLikeBtn.text = getString(R.string.like)
                }

                binding.userLikeBtn.apply {
                    isVisible = !project.isMadeByMe

                    isSelected = creator.isLiked

                    setOnClickListener {
                        if (creator.isLiked) {
                            Snackbar.make(binding.root, "Disliked ${creator.name}", Snackbar.LENGTH_LONG).show()
                        } else {
                            Snackbar.make(binding.root, "Liked ${creator.name}", Snackbar.LENGTH_LONG).show()
                        }
                        userClickListener.onUserLikeClick(creator)
                    }
                }
            }
        }

    }

    private fun setProjectRelatedUi(project: Project) {

        // make static changes here
        setImmutableProperties(project)

        checkForExpiry(project)

        viewModel.getReactiveProject(project.id).observe(viewLifecycleOwner) { currentProject ->
            if (currentProject != null) {
                // make all the reactive changes here
                // * Like comment text
                // * Like and save button
                // * Join button

                setMutableProperties(currentProject)
            } else {
                Log.i(TAG, "Not possible for a project to be absent in local database " +
                        "because it was taken from local database.")
            }
        }
    }

    private fun setAdView() {
        val adView = requireActivity().findViewById<AdView>(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())
        adView.adListener = object: AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                Log.e(TAG, p0.message)
                adView.hide()
                binding.removeAdBtn.hide()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                adView.show()
                binding.removeAdBtn.show()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                adView.show()
                binding.removeAdBtn.show()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                adView.hide()
                binding.removeAdBtn.hide()
            }

        }

        binding.removeAdBtn.setOnClickListener {
            projectClickListener.onAdInfoClick()
        }

    }

    private fun addLinks(links: List<String>) {
        for (link in links) {
            addLink(link)
        }
    }

    private fun addLink(link: String) {
        val lContext = requireContext()
        val mActivity = activity as MainActivity

        val chip = Chip(lContext)

        val darkGreenColor = ContextCompat.getColor(lContext, R.color.green_dark)
        val darkGreenNightColor = ContextCompat.getColor(lContext, R.color.green_dark_night)
        val lightGreenColor = ContextCompat.getColor(lContext, R.color.green_light)
        val textColor = if (isNightMode()) darkGreenNightColor else lightGreenColor

        chip.apply {
            text = link
            chipIconTint = ColorStateList.valueOf(darkGreenColor)
            chipIcon = ContextCompat.getDrawable(lContext, R.drawable.forward_icon)
            chipIconSize = resources.getDimension(R.dimen.large_len)
            isCheckable = false
            isCloseIconVisible = false
            chipBackgroundColor = ColorStateList.valueOf(textColor)
            binding.projectLinks.addView(this)

            setTextColor(darkGreenColor)

            setOnClickListener {
                if (link.startsWith("https://") || link.startsWith("http://")) {
                    mActivity.onLinkClick(link)
                } else {
                    mActivity.onLinkClick("https://$link")
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
        val chip = Chip(lContext)

        val (backgroundColor, textColor) = if (isNightMode()) {
            val colorPair = colorPalettesNight.random()
            ContextCompat.getColor(lContext, colorPair.first) to
            ContextCompat.getColor(lContext, colorPair.second)
        } else {
            val colorPair = colorPalettesDay.random()
            ContextCompat.getColor(lContext, colorPair.first) to
            ContextCompat.getColor(lContext, colorPair.second)
        }

        chip.apply {
            text = tag
            isCheckable = false
            isCloseIconVisible = false
            binding.projectTags.addView(this)
            chipBackgroundColor = ColorStateList.valueOf(backgroundColor)

            setTextColor(textColor)

            setOnClickListener {
                (parentFragment as ProjectFragmentContainer).navigate(TagFragment.TAG, bundleOf(TITLE to "#$tag", "tag" to tag, SUB_TITLE to "Projects related to ${project.name.uppercase()}"))
            }

            setOnLongClickListener {
                val choices = arrayListOf(OPTION_28)
                val icons = arrayListOf(R.drawable.ic_round_add_24)


                (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(title = "\"$tag\"", options = choices, icons = icons, tag = tag)
                (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                true
            }
        }

    }

    private fun onContributorsFetched(contributors: List<User>) = requireActivity().runOnUiThread {
        userAdapter.submitList(contributors)

        Log.d(TAG, "onContributorsFetched: $contributors")

        val list = arrayListOf<User>()
        for (item in contributors) {
            list.add(item)
        }

        binding.seeAllContributorsBtn.isEnabled = true
        binding.seeAllContributorsBtn.setOnClickListener {
            (parentFragment as ProjectFragmentContainer).navigate(ProjectContributorsFragment.TAG, bundleOf(
                PROJECT to project, TITLE to "Contributors", SUB_TITLE to project.name.uppercase()))
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
            .whereArrayContains(COLLABORATIONS, project.id)
            .limit(5)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {
                    val contributors = mutableListOf<User>()
                    val users = it.toObjects(User::class.java)
                    contributors.addAll(users)

                    (activity as MainActivity).getUserImpulsive(project.creator.userId) { it1 ->
                        contributors.add(it1)
                        onContributorsFetched(contributors)
                    }
                } else {
                    (activity as MainActivity).getUserImpulsive(project.creator.userId) { it1 ->
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

            onCheckForStaleData(lastComment)

            binding.projectsLastComment.root.show()
            binding.commentsHeader.show()

            // show see all comments only if there are more than two comments
            if (project.comments.toInt() <= 1) {
                binding.seeAllComments.hide()
            } else {
                binding.seeAllComments.show()
            }

            binding.projectsLastComment.apply {

                commentUserName.text = lastComment.sender.name

                commentUserImg.setImageURI(lastComment.sender.photo)

                val timeText = " • " + getTextForTime(lastComment.createdAt)
                commentTime.text =  timeText

                commentContent.text = lastComment.content

                commentLikeBtn.isSelected = lastComment.isLiked

                commentLikeBtn.setOnClickListener {
                    onCommentLikeClicked(lastComment)
                }

                commentReplyBtn.setOnClickListener {
                    onCommentReply(lastComment)
                }

                val likeRepliesText = "${lastComment.likesCount} Likes • ${lastComment.repliesCount} Replies"
                commentLikesReplies.text = likeRepliesText

                commentOptionBtn.setOnClickListener {
                    onOptionClick(lastComment)
                }

                root.setOnClickListener {
                    projectClickListener.onProjectCommentClick(project)
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

    private fun setLikeText() {
        // setting like comment text

        val likesString = getLikesString(project.likes.toInt())
        val commentsString = getCommentsString(project.comments.toInt())

        val likeCommentText =
            "$likesString • $commentsString"

        val cs1 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                projectClickListener.onProjectSupportersClick(project)
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
                projectClickListener.onProjectCommentClick(project)
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

    private fun onCheckForStaleData(comment: Comment) {

        fun onChangeNeeded(lastComment: Comment, commentSender: User) {
            val changes = mapOf(SENDER to commentSender.minify(), UPDATED_AT to System.currentTimeMillis())

            lastComment.sender = commentSender.minify()
            lastComment.updatedAt = System.currentTimeMillis()

            updateLastComment(lastComment, changes)
        }


        (activity as MainActivity).getUserImpulsive(comment.senderId) { commentSender ->
            if (commentSender.updatedAt > comment.updatedAt) {
                onChangeNeeded(comment, commentSender)
            }
        }
    }

    private fun setCommentUi(project: Project) {
        Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(project.commentChannel)
            .addSnapshotListener { snap, err ->
                
                if (err != null) {
                    Log.e(TAG, "setCommentRelatedUi: ${err.localizedMessage}")
                    Snackbar.make(binding.root, "Something went wrong while trying to fetch comments", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                        .show()

                    onEmptyComments()
                }

                if (snap != null && snap.exists()) {
                    val commentChannel = snap.toObject(CommentChannel::class.java)
                    if (commentChannel != null) {
                        setCommentUi(commentChannel)
                    }
                } else {
                    onEmptyComments()
                }

            }
    }

    /**
     * Deletion of project on expiry should be done by the server and not client but for the time
     * being it is being checked every time a project is opened.
     *
     * @param project The project to be checked for expiry
     *
    * */
    private fun checkForExpiry(project: Project) {
        // check if the project is expired
        val now = System.currentTimeMillis()
        if (project.isArchived && project.expiredAt.toInt() != -1 && project.expiredAt < now) {
            // delete project
            FireUtility.deleteProject(project) {
                if (it.isSuccessful) {
                    viewModel.deleteLocalProject(project)
                    viewModel.deleteLocalChatChannelById(project.chatChannel)

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
    private fun setImmutableProperties(project: Project) {

        binding.projectTime.text = getTextForTime(project.createdAt)

        binding.projectContent.text = project.content

        if (project.location.address.isNotBlank()) {
            binding.projectLocation.text = project.location.address
        } else {
            binding.projectLocation.hide()
        }

        binding.projectLocation.setOnClickListener {
            findNavController().navigate(R.id.locationProjectsFragment, bundleOf(TITLE to "Showing projects near", SUB_TITLE to project.location.address, "location" to project.location), slideRightNavOptions())
        }

        setProjectImages()

        // Tags related
        if (project.tags.isNotEmpty()) {
            binding.tagsHeader.show()
            binding.projectTags.show()
            addTags(project.tags)
        } else {
            binding.tagsHeader.hide()
            binding.projectTags.hide()
        }

        // links related
        if (project.sources.isNotEmpty()) {
            binding.linksHeader.show()
            binding.projectLinks.show()
            addLinks(project.sources)
        } else {
            binding.linksHeader.hide()
            binding.projectLinks.hide()
        }

        // Contributors related
        setContributors()

        // Comments related
//        setCommentLayout()
        setCommentUi(project)
    }

    /**
     * Project image related code
     * */
    private fun setProjectImages() {
        val manager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImageAdapter(this)

        val helper = LinearSnapHelper()

        binding.projectImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = manager
            onFlingListener = null
            helper.attachToRecyclerView(this)
        }

        val totalImageCount = project.images.size
        if (totalImageCount == 1) {
            binding.imagesCounter.hide()
        } else {
            val imageCount = "1/$totalImageCount"
            binding.imagesCounter.text = imageCount
        }

        imageAdapter.submitList(project.images)

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

    /**
     * All mutable actions related to project that alters the project document
     * */
    private fun setMutableProperties(project: Project) {


        setLikeText()

        // Like button related code
        binding.projectLikeBtn.apply {
            isSelected = project.isLiked

            setOnClickListener {
                projectClickListener.onProjectLikeClick(project.copy()) {
                    setMutableProperties(it)
                }
            }
        }

        // Save button related code
        binding.projectSaveBtn.apply {
            isSelected = project.isSaved

            setOnClickListener {
                projectClickListener.onProjectSaveClick(project.copy()) {
                    setMutableProperties(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lr?.remove()
    }

    companion object {
        const val TAG = "ProjectFragment"

        fun newInstance(bundle: Bundle) = ProjectFragment().apply {
            arguments = bundle
        }

    }

    override fun onImageClick(view: View, image: Image) {
        (activity as MainActivity).showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {

    }

    override fun onCommentLikeClicked(comment: Comment) {

        val changes = if (!comment.isLiked) {

            comment.likesCount += 1
            val newList = comment.likes.addItemToList(UserManager.currentUserId)
            comment.likes = newList
            comment.updatedAt = System.currentTimeMillis()

            mapOf(
                "likesCount" to FieldValue.increment(1),
                "likes" to FieldValue.arrayUnion(UserManager.currentUserId),
                "updatedAt" to System.currentTimeMillis()
            )
        } else {

            comment.likesCount -= 1
            val newList = comment.likes.removeItemFromList(UserManager.currentUserId)
            comment.likes = newList
            comment.updatedAt = System.currentTimeMillis()

            mapOf(
                "likesCount" to FieldValue.increment(-1),
                "likes" to FieldValue.arrayRemove(UserManager.currentUserId),
                "updatedAt" to System.currentTimeMillis()
            )
        }

        updateLastComment(comment, changes)

    }

    private fun updateLastComment(comment: Comment, changes: Map<String, Any?>) {
        FireUtility.updateComment(comment.commentChannelId, comment.commentId, changes) {
            if (it.isSuccessful) {
                // since this is the last comment
                val changes1 = mapOf("lastComment" to comment, "updatedAt" to System.currentTimeMillis())

                viewModel.insertComment(comment)

                FireUtility.updateCommentChannel(comment.commentChannelId, changes1) { it1 ->
                    if (!it1.isSuccessful) {
                        Log.e(TAG, "onCommentLikeClicked: ${it.exception?.localizedMessage}")
                    }
                }

            } else {
                Log.e(TAG, "onCommentLikeClicked: ${it.exception?.localizedMessage}")
            }
        }
    }

    override fun onCommentReply(comment: Comment) {
        viewModel.replyToContent.postValue(comment)
        projectClickListener.onProjectCommentClick(project)
    }

    override fun onOptionClick(comment: Comment) {
        val isCommentByMe = comment.senderId == UserManager.currentUserId
        val name: String

        val (choices, icons) = if (isCommentByMe) {
            name = "You"
            arrayListOf(OPTION_29, OPTION_30) to arrayListOf(R.drawable.ic_report, R.drawable.ic_remove)
        } else {
            name = comment.sender.name
            arrayListOf(OPTION_29) to arrayListOf(R.drawable.ic_report)
        }

        (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(title = "Comment by $name", options = choices, icons = icons, comment = comment)
        (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

    }

}