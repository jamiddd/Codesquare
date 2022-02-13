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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.google.android.gms.ads.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ImageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter2
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.CommentBottomLayoutBinding
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

    private var commentInputLayout: View? = null
    private var mainRoot: CoordinatorLayout? = null

    private val projectClickListener: ProjectClickListener by lazy { requireActivity() as ProjectClickListener }
    private val userClickListener: UserClickListener by lazy { requireActivity() as UserClickListener }
    private val commentClickListener: CommentListener by lazy { requireActivity() as CommentListener }
    private val joinBtn: MaterialButton by lazy { requireActivity().findViewById(R.id.main_primary_action) }
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val project = arguments?.getParcelable<Project>(PROJECT)
        if (project?.isMadeByMe == true) {
            inflater.inflate(R.menu.project_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_project -> {
                val project = arguments?.getParcelable<Project>(PROJECT)
                findNavController().navigate(
                    R.id.action_projectFragment_to_updateProjectFragment,
                    bundleOf("project" to project),
                    slideRightNavOptions()
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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

                // setting listener for join button
                listenForContributorChanges(project)

                // setting archive related properties
                setArchiveProperties(project)

                if (!currentProject.isMadeByMe) {
                    if (!currentProject.isCollaboration) {
                        if (currentProject.isRequested) {
                            updateJoinButtonUi(currentProject, REQUESTED_UNKNOWN)
                        }
                    } else {
                        updateJoinButtonUi(project, REQUESTED_ACCEPTED)
                    }
                } else {
                    updateJoinButtonUi(project, REQUESTED_ACCEPTED)
                }

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
                adView.hide()
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
            chipIconSize = convertDpToPx(18).toFloat()
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
            chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            isCheckable = false
            isCloseIconVisible = false
            binding.projectTags.addView(this)

            setTextColor(textColor)

            setOnClickListener {
                val bundle = bundleOf("title" to "#$tag", "tag" to tag)
                findNavController().navigate(
                    R.id.action_projectFragment_to_tagFragment,
                    bundle,
                    slideRightNavOptions()
                )
            }
        }

    }

    private fun setContributors() {
        binding.projectContributorsRecycler.show()

        binding.seeAllContributorsBtn.setOnClickListener {

            val bundle = bundleOf(
                ProjectContributorsFragment.ARG_PROJECT to project,
                ProjectContributorsFragment.ARG_ADMINISTRATORS to arrayListOf(project.creator.userId)
            )

            findNavController().navigate(
                R.id.action_projectFragment_to_projectContributorsFragment,
                bundle,
                slideRightNavOptions()
            )
        }

        val userAdapter2 =
            UserAdapter2(project.id, project.chatChannel, listOf(project.creator.userId))

        binding.projectContributorsRecycler.apply {
            adapter = userAdapter2
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.getProjectContributors(project, 7) {
            if (it.isSuccessful) {
                if (!it.result.isEmpty) {
                    binding.contributorsHeaderLayout.show()
                    val contributors = it.result.toObjects(User::class.java)
                    userAdapter2.submitList(contributors)
                } else {
                    binding.contributorsHeaderLayout.hide()
                }
            } else {
                binding.contributorsHeaderLayout.hide()
                viewModel.setCurrentError(it.exception)
            }
        }

    }

    private fun setCommentRelatedUi(project: Project) {
        FireUtility.getCommentChannel(project.commentChannel) {
            when (it) {
                is Result.Error -> viewModel.setCurrentError(it.exception)
                is Result.Success -> {
                    val commentChannel = it.data
                    val lastComment = commentChannel.lastComment
                    if (lastComment != null) {
                        FireUtility.getComment(lastComment.commentId) { commentResult ->
                            when (commentResult) {
                                is Result.Error -> viewModel.setCurrentError(commentResult.exception)
                                is Result.Success -> {
                                    val comment = commentResult.data
                                    viewModel.insertComment(comment)
                                    setReactiveComment(project, comment)

                                    binding.seeAllComments.setOnClickListener {
                                        projectClickListener.onProjectCommentClick(project)
                                    }

                                }
                                null -> {
                                    updateCommentUi(NO_COMMENT, project)
                                    Log.w(TAG, "Something went wrong while trying to fetch comment " +
                                            "with id: ${lastComment.commentId} and commentChannelId: ${lastComment.commentChannelId}")
                                }
                            }
                        }
                    } else {
                        updateCommentUi(NO_COMMENT, project)
                    }
                }
                null -> {
                    updateCommentUi(NO_COMMENT, project)
                    Log.w(TAG, "Something went wrong while trying to fetch comment " +
                            "channel with id: ${project.commentChannel} and projectId: ${project.id}")
                }
            }
        }
    }


    private fun updateCommentUi(state: Int, project: Project, comment: Comment? = null) {

        binding.commentLayoutProgress.hide()

        when (state) {
            NO_COMMENT -> {
                binding.projectsLastComment.root.hide()
                binding.commentsHeaderLayout.hide()
            }
            COMMENT_EXISTS -> {
                val c = comment!!

                binding.projectsLastComment.root.show()
                binding.commentsHeaderLayout.show()

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

                setCommentInputLayout(c)

            }
            COMMENT_NO_SENDER_ERROR -> {
                binding.projectsLastComment.root.hide()
                binding.commentsHeaderLayout.hide()
            }
        }
    }

    private fun setCommentInputUI(mBinding: CommentBottomLayoutBinding) {
        val currentUser = UserManager.currentUser
        mBinding.senderImg.setImageURI(currentUser.photo)
        mBinding.commentInputLayout.requestFocus()
    }

    @SuppressLint("InflateParams")
    private fun setCommentInputLayout(c: Comment) {
        val commentBottomLayout = layoutInflater.inflate(R.layout.comment_bottom_layout, null, false)
        val commentBottomBinding = CommentBottomLayoutBinding.bind(commentBottomLayout)

        // removing any existing views
        mainRoot?.removeView(commentInputLayout)

        // adding option to comment on project or reply to a comment
        mainRoot = requireActivity().findViewById(R.id.main_container_root)
        mainRoot?.addView(commentBottomBinding.root)

        // saving the whole bottom layout to be removed later in [onDestroy]
        commentInputLayout = commentBottomBinding.root

        val params = commentBottomBinding.root.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        commentBottomBinding.root.layoutParams = params

        setSendButton(commentBottomBinding.commentSendBtn, commentBottomBinding.commentInputLayout)

        commentBottomBinding.replyingToText.setOnClickListener {
            viewModel.replyToContent.postValue(null)
        }

        setCommentInputUI(commentBottomBinding)

        viewModel.replyToContent.observe(viewLifecycleOwner) {
            if (it != null) {

                val sender = it.sender
                commentBottomBinding.replyingToText.show()

                // setting styles to reply view
                val name = sender.name
                val replyToText = "Replying to $name"
                val sp = SpannableString(replyToText)
                sp.setSpan(
                    StyleSpan(Typeface.BOLD),
                    replyToText.length - name.length,
                    replyToText.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                commentBottomBinding.replyingToText.text = sp

                // updating send button function to include this comment as a reply comment
                setSendButton(
                    commentBottomBinding.commentSendBtn,
                    commentBottomBinding.commentInputLayout,
                    it
                )

                // hiding the join button as it comes over the bottom input
                joinBtn.slideDown(convertDpToPx(100f))
            } else {
                // if reply comment is removed there's no point showing comment input
                commentInputLayout?.slideDown(convertDpToPx(300f))

                joinBtn.slideReset()
            }
        }

        commentBottomBinding.adView.hide()
        commentInputLayout?.slideDown(convertDpToPx(300f))
    }

    private fun setReactiveComment(project: Project, comment: Comment) {
        viewModel.getReactiveComment(comment.commentId).observe(viewLifecycleOwner) {
            if (it != null) {
                FireUtility.getUser(comment.senderId) { commentSenderResult ->
                    when (commentSenderResult) {
                        is Result.Error -> viewModel.setCurrentError(commentSenderResult.exception)
                        is Result.Success -> {
                            val sender = commentSenderResult.data
                            comment.sender = sender

                            updateCommentUi(COMMENT_EXISTS, project, comment)
                        }
                        null -> updateCommentUi(COMMENT_NO_SENDER_ERROR, project)
                    }
                }

            }
        }
    }

    private fun onReplyButtonClicked() {
        commentInputLayout?.slideReset()

        showKeyboard()

        // scrolling the whole fragment only after the comment input has shown up
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            binding.projectFragmentScroll.fullScroll(View.FOCUS_DOWN)

            delay(100)
            commentInputLayout?.findViewById<EditText>(R.id.comment_input_layout)?.requestFocus()

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
     * To solve the issue of knowing, when the creator of the project will accept project request
     * from the current user, we are setting this listener on the chat channel because if the
     * listener was set on project document, it can reflect lot of changes that might not be
     * important. If the current has not sent any request this function will not listen for changes.
     * If the current user has created the project, then there's no point in calling the function
     *
     * @param project The project to listen changes for
     * */
    private fun listenForContributorChanges(project: Project) {
        if (project.isMadeByMe) {
            updateJoinButtonUi(project, REQUESTED_ACCEPTED)
            return
        }

        if (project.isBlocked) {
            updateJoinButtonUi(project, REQUEST_BLOCKED)
            return
        }

        // check if the current user has requested to join the current project
        if (project.isRequested) {
            Firebase.firestore.collection(CHAT_CHANNELS).document(project.chatChannel)
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        viewModel.setCurrentError(error)
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val chatChannel = documentSnapshot.toObject(ChatChannel::class.java)!!
                        val currentUserId = UserManager.currentUserId
                        if (chatChannel.contributors.contains(currentUserId)) {
                            // that means the user has been accepted to this project
                            updateJoinButtonUi(project, REQUESTED_ACCEPTED)
                        } else {
                            // the user is not yet accepted to this project
                            updateJoinButtonUi(project, REQUESTED_UNKNOWN)
                        }
                    }
                }
        } else {
            updateJoinButtonUi(project, NOT_REQUESTED)
            Log.i(TAG, "Not listening to project document, as the current user has not requested to join the project.")
        }
    }

    /**
     * Update the join button on every changes related to it
     *
     * @param project The current project in reference to the join button
     * @param state The state of the join button. Possible states [NOT_REQUESTED], [REQUESTED_ACCEPTED], [REQUESTED_UNKNOWN]
     * (-1: The current user has requested to join the project but not yet accepted.
     * 0: The current user has not requested to join this project.
     * 1: The current user has requested and he's been accepted)
     *
    * */
    private fun updateJoinButtonUi(project: Project, state: Int) {
        when (state) {
            NOT_REQUESTED -> {
                joinBtn.text = getString(R.string.join)
                joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)
                joinBtn.slideReset()
            }
            REQUESTED_UNKNOWN -> {
                joinBtn.text = getString(R.string.undo)
                joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_undo_20)
                joinBtn.slideReset()
            }
            REQUESTED_ACCEPTED -> {
                joinBtn.slideDown(convertDpToPx(100f))
            }
            REQUEST_BLOCKED -> {
                joinBtn.text = getString(R.string.join)
                joinBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)
                joinBtn.slideReset()
                joinBtn.disable()
            }
        }

        joinBtn.setOnClickListener {
            projectClickListener.onProjectJoinClick(project.copy())
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
        val imageAdapter = ImageAdapter { v, controllerListener ->
            val pos = manager.findFirstCompletelyVisibleItemPosition()
            (activity as MainActivity).showImageViewFragment(
                v,
                project.images[pos].toUri(),
                ".jpg",
                controllerListener
            )
        }

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
                projectClickListener.onProjectLikeClick(project.copy())
            }
        }

        // Save button related code
        binding.projectSaveBtn.apply {
            isSelected = project.isSaved

            setOnClickListener {
                projectClickListener.onProjectSaveClick(project.copy())
            }
        }
    }

    private fun archiveProject(project: Project) {

        val currentUser = UserManager.currentUser

        FireUtility.archiveProject(project) {
            if (it.isSuccessful) {
                Snackbar.make(
                    binding.root,
                    "Archived project successfully",
                    Snackbar.LENGTH_LONG
                ).show()
                // notify the other contributors that the project has been archived
                val content = "${project.name} has been archived."
                val notification = Notification.createNotification(
                    content,
                    currentUser.id,
                    project.chatChannel
                )
                FireUtility.sendNotificationToChannel(notification) { it1 ->
                    if (it1.isSuccessful) {
                        // updating project locally
                        project.isArchived = true
                        viewModel.updateLocalProject(project)
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun unArchiveProject(project: Project) {
        FireUtility.unArchiveProject(project) {
            if (it.isSuccessful) {
                Snackbar.make(
                    binding.root,
                    "Un-archived project successfully",
                    Snackbar.LENGTH_LONG
                ).show()
                project.isArchived = false
                viewModel.updateLocalProject(project)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun setArchiveProperties(project: Project) {
        binding.archiveProjectBtn.setOnClickListener {
            if (project.isArchived) {
                binding.archiveProjectBtn.text = getString(R.string.un_archive)
                binding.archiveProjectInfo.text = getString(R.string.archive_summary)

                showDialog("Are you sure you want to un-archive this project?", "Un-archiving project ... ") {
                    unArchiveProject(project)
                }
            } else {
                binding.archiveProjectBtn.text = getString(R.string.archive)
                binding.archiveProjectInfo.text = getString(R.string.un_archive_summary)

                showDialog("Are you sure you want to archive this project?", "Archiving project ...", posLabel = "Archive") {
                    archiveProject(project)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lr?.remove()
        viewModel.replyToContent.postValue(null)
        mainRoot?.removeView(commentInputLayout)
    }

    companion object {
        private const val TAG = "ProjectFragment"
        private const val REQUESTED_UNKNOWN = -1
        private const val REQUESTED_ACCEPTED = 1
        private const val NOT_REQUESTED = 0
        private const val REQUEST_BLOCKED = -2

        private const val COMMENT_EXISTS = 1
        private const val NO_COMMENT = 0
        private const val COMMENT_NO_SENDER_ERROR = -1
    }

}