package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ListenerRegistration
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentProjectBinding
import com.jamid.codesquare.listeners.CommentListener
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.UserClickListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class ProjectFragment : Fragment() {

    private lateinit var binding: FragmentProjectBinding
    private lateinit var project: Project
    private var lr: ListenerRegistration? = null
    private val projectClickListener: ProjectClickListener by lazy { requireActivity() as ProjectClickListener }
    private val userClickListener: UserClickListener by lazy { requireActivity() as UserClickListener }
    private val commentClickListener: CommentListener by lazy { requireActivity() as CommentListener }
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

        binding.projectLikeCommentText.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }

        if (currentUser.premiumState.toInt() == -1) {
            setAdView()
        } else {
            binding.adView.hide()
        }

        binding.projectRefresher.setOnRefreshListener {

            // getting new data of the project
            FireUtility.getProject(project.id) { newProjectResult ->

                // finished process and loading
                binding.projectRefresher.isRefreshing = false

                // inserting project to local database
                when (newProjectResult) {
                    is Result.Error -> viewModel.setCurrentError(newProjectResult.exception)
                    is Result.Success -> {
                        project = processProjects(arrayOf(newProjectResult.data))[0]
                    }
                    null -> Log.w(TAG, "Something went wrong while trying to get project document with project id: ${project.id}")
                }
            }
        }

    }

    private fun setCreatorRelatedUi(mCreator: UserMinimal) {

        // set immutable content
        binding.userImg.setImageURI(mCreator.photo)
        binding.userName.text = mCreator.name

        // getting new user data always, because it can change or update any time
        FireUtility.getUser(mCreator.userId) { userResult ->
            when (userResult) {
                is Result.Error -> viewModel.setCurrentError(userResult.exception)
                is Result.Success -> viewModel.insertUsers(userResult.data)
                null -> Log.w(TAG,"Something went wrong while fetching user data")
            }
        }

        // set mutable content
        viewModel.getReactiveUser(mCreator.userId).observe(viewLifecycleOwner) { creator ->
            if (creator != null) {

                binding.userImg.setOnClickListener {
                    userClickListener.onUserClick(creator)
                }

                binding.userName.setOnClickListener {
                    userClickListener.onUserClick(creator)
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

                // setting archive related properties
//                setArchiveProperties(project)

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

        tag.trim() // TODO("Make sure the trimming happens at the root")

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
        }

    }

    private fun setContributors() {
        binding.seeAllContributorsBtn.setOnClickListener {
            (parentFragment as ProjectFragmentContainer).navigate(ProjectContributorsFragment.TAG, bundleOf(
                PROJECT to project, TITLE to "Contributors", SUB_TITLE to project.name.uppercase()))
        }

        getChatChannel()

    }

    private fun getChatChannel() {
        val contHeader = "Contributors (${project.contributors.size})"
        binding.contributorsHeader.text = contHeader

        val cachedChannel = viewModel.getCachedChatChannel(project.chatChannel)
        if (cachedChannel != null) {
            onReceiveChatChannel(cachedChannel)
        } else {
            FireUtility.getChatChannel(project.chatChannel) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        viewModel.putChatChannelToCache(it.data)
                        onReceiveChatChannel(it.data)
                    }
                    null -> Log.w(TAG, "Something went wrong while fetching chat channel with id: ${project.chatChannel}")
                }
            }
        }
    }

    private fun onContributorsFetched(contributors: List<User>) {
        showContributorsSection()

        viewModel.getLocalUser(project.creator.userId) { creator ->
            requireActivity().runOnUiThread {
                if (creator != null) {
                    val x = mutableListOf<User>()
                    x.addAll(contributors)
                    userAdapter.submitList(x)
                }
            }
        }
    }

    private fun getProjectContributors() {
        viewModel.getChannelContributorsLive("%${project.chatChannel}%").observe(viewLifecycleOwner) { contributors ->
            if (contributors.isNotEmpty()) {
                onContributorsFetched(contributors)
            } else {
                FireUtility.getProjectContributors(project, 4) {
                    if (it.isSuccessful) {
                        if (!it.result.isEmpty) {
                            val contributors1 = it.result.toObjects(User::class.java)
                            viewModel.insertUsers(contributors1)
                        } else {
                            hideContributorsSection()
                        }
                    } else {
                        hideContributorsSection()
                        viewModel.setCurrentError(it.exception)
                    }
                }
            }
        }
    }

    private fun onReceiveChatChannel(chatChannel: ChatChannel) {
        userAdapter = UserAdapter(small = true, associatedChatChannel = chatChannel)

        binding.projectContributorsRecycler.apply {
            adapter = userAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        getProjectContributors()
    }

    private fun showContributorsSection() {
        binding.projectContributorsRecycler.show()
        binding.contributorsHeader.show()
        binding.seeAllContributorsBtn.show()
    }

    private fun hideContributorsSection() {
        binding.projectContributorsRecycler.hide()
        binding.contributorsHeader.hide()
        binding.seeAllContributorsBtn.hide()
    }

    private fun onCommentReceived(comment: Comment) {
        viewModel.insertCommentToCache(comment)
        viewModel.insertComment(comment)

        setReactiveComment(project, comment)

        binding.seeAllComments.setOnClickListener {
            projectClickListener.onProjectCommentClick(project)
        }
    }

    private fun onCommentChannelReceived(commentChannel: CommentChannel) {
        val lastComment = commentChannel.lastComment
        if (lastComment != null) {

            val updatedLastComment = viewModel.getCachedComment(lastComment.commentId)

            if (updatedLastComment != null) {
                onCommentReceived(updatedLastComment)
            } else {
                FireUtility.getComment(lastComment.commentId) { commentResult ->
                    when (commentResult) {
                        is Result.Error -> viewModel.setCurrentError(commentResult.exception)
                        is Result.Success -> {
                            val comment = commentResult.data
                            onCommentReceived(comment)
                        }
                        null -> {
                            updateCommentUi(NO_COMMENT, project)
                            Log.w(TAG, "Something went wrong while trying to fetch comment " +
                                    "with id: ${lastComment.commentId} and commentChannelId: ${lastComment.commentChannelId}")
                        }
                    }
                }
            }

        } else {
            updateCommentUi(NO_COMMENT, project)
        }
    }

    private fun setCommentRelatedUi(project: Project) {
        val cachedCommentChannel = viewModel.getCachedCommentChannel(project.commentChannel)
        if (cachedCommentChannel != null) {
            onCommentChannelReceived(cachedCommentChannel)
        } else {
            FireUtility.getCommentChannel(project.commentChannel) {
                when (it) {
                    is Result.Error -> {
                        viewModel.setCurrentError(it.exception)
                        updateCommentUi(NO_COMMENT, project)
                    }
                    is Result.Success -> {
                        val commentChannel = it.data
                        viewModel.putCommentChannelToCache(commentChannel)
                        onCommentChannelReceived(commentChannel)
                    }
                    null -> {
                        updateCommentUi(NO_COMMENT, project)
                        Log.w(TAG, "Something went wrong while trying to fetch comment " +
                                "channel with id: ${project.commentChannel} and projectId: ${project.id}")
                    }
                }
            }
        }
    }


    private fun updateCommentUi(state: Int, project: Project, comment: Comment? = null) {

        binding.commentLayoutProgress.hide()

        when (state) {
            NO_COMMENT -> {
                binding.projectsLastComment.root.hide()
                 binding.commentsHeader.hide()
                 binding.seeAllComments.hide()
            }
            COMMENT_EXISTS -> {
                val c = comment!!

                binding.projectsLastComment.root.show()

                 binding.commentsHeader.show()
                 binding.seeAllComments.show()

                binding.projectsLastComment.apply {

                    commentUserName.text = c.sender.name
                    commentUserImg.setImageURI(c.sender.photo)
                    commentTime.text =  " • " + getTextForTime(c.createdAt)
                    commentContent.text = c.content

                    commentLikeBtn.isSelected = comment.isLiked

                    commentLikeBtn.setOnClickListener {
                        commentClickListener.onCommentLikeClicked(c)
                    }

                    commentReplyBtn.setOnClickListener {
                        commentClickListener.onCommentReply(c)
                        onReplyButtonClicked()
                    }

                    val likeRepliesText = "${c.likesCount} Likes • ${c.repliesCount} Replies"
                    commentLikesReplies.text = likeRepliesText

                    commentOptionBtn.setOnClickListener {
                        commentClickListener.onOptionClick(c)
                    }

                    root.setOnClickListener {
                        projectClickListener.onProjectCommentClick(project)
                    }
                }

                setCommentInputLayout()

            }
            COMMENT_NO_SENDER_ERROR -> {
                binding.projectsLastComment.root.hide()

                binding.commentsHeader.hide()
                binding.seeAllComments.hide()
            }
        }
    }

    private fun setCommentInputUI(senderImg: SimpleDraweeView, commentInputLayout: EditText) {
        val currentUser = UserManager.currentUser
        senderImg.setImageURI(currentUser.photo)
        commentInputLayout.requestFocus()
    }

    @SuppressLint("InflateParams")
    private fun setCommentInputLayout() {

        val commentBottomRoot = requireActivity().findViewById<MaterialCardView>(R.id.comment_bottom_root)
        val sendBtn = commentBottomRoot.findViewById<MaterialButton>(R.id.comment_send_btn)!!
        val commentInputLayout = commentBottomRoot.findViewById<EditText>(R.id.comment_input_layout)!!
        val replyToText = commentBottomRoot.findViewById<TextView>(R.id.replying_to_text)!!
        val senderImg = commentBottomRoot.findViewById<SimpleDraweeView>(R.id.sender_img)!!

        setSendButton(sendBtn, commentInputLayout)

        replyToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        setCommentInputUI(senderImg, commentInputLayout)

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {

                commentBottomRoot.slideReset()

                val sender = it.sender
                replyToText.show()

                val rt = if (sender.id == UserManager.currentUserId) {
                    "Replying to your comment"
                } else {
                    // setting styles to reply view
                    val s = "Replying to ${sender.name}"
                    val sp = SpannableString(s)
                    sp.setSpan(
                        StyleSpan(Typeface.BOLD),
                        s.length - sender.name.length,
                        s.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sp
                }

                replyToText.text = rt

                // updating send button function to include this comment as a reply comment
                setSendButton(
                    sendBtn,
                    commentInputLayout,
                    it
                )

            } else {

                hideKeyboard()

                // if reply comment is removed there's no point showing comment input
                val dy = resources.getDimension(R.dimen.comment_layout_translation)
                commentBottomRoot.slideDown(dy)

            }
        }

    }

    private fun onCommentWithSenderReceived(user: User, project: Project, comment: Comment) {
        comment.sender = user
        updateCommentUi(COMMENT_EXISTS, project, comment)
    }

    private fun setReactiveComment(project: Project, comment: Comment) {
        viewModel.getReactiveComment(comment.commentId).observe(viewLifecycleOwner) {
            if (it != null) {

                viewModel.insertCommentToCache(it)

                val commentSender = viewModel.getCachedUser(comment.senderId)
                if (commentSender != null) {
                    onCommentWithSenderReceived(commentSender, project, comment)
                } else {
                    FireUtility.getUser(comment.senderId) { commentSenderResult ->
                        when (commentSenderResult) {
                            is Result.Error -> viewModel.setCurrentError(commentSenderResult.exception)
                            is Result.Success -> {
                                onCommentWithSenderReceived(commentSenderResult.data, project, comment)
                            }
                            null -> updateCommentUi(COMMENT_NO_SENDER_ERROR, project)
                        }
                    }
                }
            }
        }
    }

    private fun onReplyButtonClicked() {

        showKeyboard()

        // scrolling the whole fragment only after the comment input has shown up
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            binding.projectFragmentScroll.fullScroll(View.FOCUS_DOWN)
        }

    }

    private fun setSendButton(
        sendBtn: Button,
        inputLayout: EditText,
        replyComment: Comment? = null
    ) {
        val currentUser = UserManager.currentUser
        if (replyComment != null) {
            sendBtn.setOnClickListener {
                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.trim().toString()

                val comment1 = Comment(
                    randomId(),
                    content,
                    currentUser.id,
                    replyComment.commentId,
                    replyComment.projectId,
                    replyComment.threadChannelId,
                    randomId(),
                    0,
                    0,
                    replyComment.commentLevel + 1,
                    System.currentTimeMillis(),
                    emptyList(),
                    currentUser,
                    false,
                    replyComment.postTitle
                )

                viewModel.sendComment(comment1, replyComment)

                inputLayout.text.clear()

                viewModel.replyToContent.postValue(null)
            }
        } else {
            sendBtn.setOnClickListener {

                if (inputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = inputLayout.text.trim().toString()

                val comment1 = Comment(
                    randomId(),
                    content,
                    currentUser.id,
                    project.id,
                    project.id,
                    project.commentChannel,
                    randomId(),
                    0,
                    0,
                    0,
                    System.currentTimeMillis(),
                    emptyList(),
                    currentUser,
                    false,
                    project.name
                )
                viewModel.sendComment(comment1, project)

                inputLayout.text.clear()
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
        setCommentRelatedUi(project)
    }

    /**
     * Project image related code
     * */
    private fun setProjectImages() {
        val manager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImageAdapter(requireActivity() as MainActivity)

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

        // setting like comment text
        val likeSuffix = if (project.likes.toInt() == 1) "Like" else "Likes"
        val commentSuffix = if (project.comments.toInt() == 1) "Comment" else "Comments"

        val likeCommentText =
            "${project.likes} $likeSuffix • ${project.comments} $commentSuffix"

        binding.projectLikeCommentText.text = likeCommentText

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
        viewModel.replyToContent.postValue(null)
    }

    companion object {
        const val TAG = "ProjectFragment"

        private const val COMMENT_EXISTS = 1
        private const val NO_COMMENT = 0
        private const val COMMENT_NO_SENDER_ERROR = -1

        fun newInstance(bundle: Bundle) = ProjectFragment().apply {
            arguments = bundle
        }

    }

}