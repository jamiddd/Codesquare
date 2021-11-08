package com.jamid.codesquare.ui

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.LocationItemClickListener
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity: AppCompatActivity(), LocationItemClickListener, ProjectClickListener, ProjectRequestListener, UserClickListener, CommentListener, ChatChannelClickListener, MessageListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    val requestGoogleSingInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                account?.idToken?.let { it1 ->
                    firebaseAuthWithGoogle(it1)
                }
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                viewModel.setCurrentError(e)
            }
        } else {
            toast("Activity result was not OK!")
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { it1 ->
                viewModel.setCurrentImage(it1)
            }
        }
    }

    private val selectProjectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.setCurrentProjectImages(images.map {it1 -> it1.toString() })

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setCurrentProjectImages(listOf(it1.toString()))
                }
            }
        }
    }

    private val selectChatDocumentsUploadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val documents = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { doc ->
                        documents.add(doc)
                    }
                }

                viewModel.setChatUploadDocuments(documents)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadDocuments(listOf(it1))
                }
            }
        }
    }

    private val selectChatImagesUploadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.setChatUploadImages(images)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadImages(listOf(it1))
                }
            }
        }
    }

    private val selectMoreProjectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {

            val clipData = it.data?.clipData

            if (clipData != null) {
                val count = clipData.itemCount

                val images = mutableListOf<Uri>()

                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i)?.uri
                    uri?.let { image ->
                        images.add(image)
                    }
                }

                viewModel.addToExistingProjectImages(images.map {it1 -> it1.toString() })

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.addToExistingProjectImages(listOf(it1.toString()))
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FireUtility.signInWithGoogle(credential) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val ref = Firebase.firestore.collection("users")
                        .document(user.uid)

                    FireUtility.getDocument(ref) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val oldUser = it1.result.toObject(User::class.java)!!
                            viewModel.insertCurrentUser(oldUser)
                        } else {
                            val localUser = User.newUser(user.uid, user.displayName!!, user.email!!)
                            FireUtility.uploadDocument(ref, localUser) { it2 ->
                                if (it2.isSuccessful) {
                                    viewModel.insertCurrentUser(localUser)
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                    Firebase.auth.signOut()
                                }
                            }
                        }
                    }


                } else {
                    Firebase.auth.signOut()
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.mainToolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        binding.mainToolbar.setupWithNavController(navController, appBarConfiguration)


        navController.addOnDestinationChangedListener { controller, destination, arguments ->

            val userInfoLayout = findViewById<View>(R.id.user_info)

            binding.mainToolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }

            binding.mainToolbar.isTitleCentered = destination.id != R.id.homeFragment

            when (destination.id) {
                R.id.loginFragment -> {
                    userInfoLayout?.hide()
                    userInfoLayout?.updateLayout(margin = 0)
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.hide()
                }
                R.id.homeFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.show()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.createProjectFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                }
                R.id.profileFragment -> {
                    userInfoLayout?.show()
                    userInfoLayout?.updateLayout(marginTop = convertDpToPx(56))
                    binding.mainToolbar.show()
                    binding.mainTabLayout.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.editProfileFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                }
                R.id.projectFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideReset()
                }
                R.id.projectRequestFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.savedProjectsFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.commentsFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.chatFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                    binding.mainAppbar.slideReset()

                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params

                }
                R.id.chatDetailFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())

                    binding.mainAppbar.slideReset()

                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params
                }
                R.id.chatMediaFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.show()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())

                    binding.mainAppbar.slideReset()

                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params
                }
                R.id.projectContributorsFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.tagFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.imageViewFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())

                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params

                }
            }
        }

        viewModel.currentUser.observe(this) {
            if (it != null) {
                Log.d(TAG, it.toString())
                if (!isInitialized) {
                    isInitialized = true
                    Log.d("MessageChecking", "Got User ")

                    // getting all the chat channels first
                    viewModel.getAllChatChannels(it.id) { it1 ->
                        if (it1.isSuccessful) {
                            val chatChannels = it1.result.toObjects(ChatChannel::class.java)

                            Log.d("MessageChecking", "Got channels ${chatChannels.size}")

                            for (channel in chatChannels) {
                                Log.d("MessageChecking", "Setting up ${channel.projectTitle}")
                                // getting all the contributors in a chat channel
                                getChannelData(it, channel)
                            }
                        } else {
                            viewModel.setCurrentError(it1.exception)
                        }
                    }
                }
            }
        }

        viewModel.currentError.observe(this) { exception ->
            if (exception != null) {
                Log.e(TAG, exception.localizedMessage.orEmpty())
                toast(exception.localizedMessage.orEmpty())
            }
        }

        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null) {

            // listener for users
            Firebase.firestore.collection("users")
                .document(firebaseUser.uid)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        viewModel.setCurrentError(error)
                    }

                    if (value != null && value.exists()) {

                        Log.d("MessageChecking", "Got new user")

                        val newUser = value.toObject(User::class.java)
                        if (newUser != null) {

                            val currentUser = viewModel.currentUser.value
                            if (currentUser != null) {
                                // this will look for any recent changes, so as to not download
                                    // everything, every time there's a change
                                if (currentUser.chatChannels.size < newUser.chatChannels.size) {
                                    for (channel in newUser.chatChannels) {
                                        if (!currentUser.chatChannels.contains(channel)) {
                                            setUpChatChannel(currentUser, channel)
                                        }
                                    }
                                }
                            }

                            viewModel.insertCurrentUser(newUser)
                        }
                    }
                }
        }
    }


    private fun setUpChatChannel(currentUser: User, chatChannelId: String) {
        // getting all the chat channels first
        viewModel.getChatChannel(chatChannelId) { it1 ->
            if (it1.isSuccessful) {
                if (it1.result != null && it1.result.exists()) {
                    val chatChannel = it1.result.toObject(ChatChannel::class.java)!!
                    getChannelData(currentUser, chatChannel)
                }
            } else {
                viewModel.setCurrentError(it1.exception)
            }
        }
    }

    private fun getChannelData(currentUser: User, chatChannel: ChatChannel) {
        // getting all the contributors in a chat channel
        viewModel.getChannelUsers(chatChannel.chatChannelId) { it2 ->
            if (it2.isSuccessful) {
                val users = it2.result.toObjects(User::class.java).filter {
                    it.id != currentUser.id
                }
                Log.d(TAG, chatChannel.projectTitle + " " +  users.map { it.name }.toString())
                viewModel.insertChannelWithContributors(chatChannel, users)
            } else {
                viewModel.setCurrentError(it2.exception)
            }
        }

        getLatestMessagesBaseOnLastMessage(chatChannel)

        // listening for new messages as soon as activity starts
        addChannelListener(currentUser, chatChannel)
    }

    // the criteria for checking time is 24 hour
    private fun isLastMessageReallyOld(message: Message): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - message.createdAt
        return diff > 12 * 60 * 60 * 1000
    }

    // the criteria for checking time is 12 hour
    private fun isLastMessageRelativelyNew(message: Message): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - message.createdAt
        return diff <= 6 * 60 * 60 * 1000
    }

    private fun getLatestMessagesBaseOnLastMessage(chatChannel: ChatChannel) = lifecycleScope.launch {

        Log.d("MessageChecking", "Getting latest Messages ${chatChannel.projectTitle}")

        val lastMessage = viewModel.getLastMessageForChannel(chatChannel.chatChannelId)
        if (lastMessage != null) {

            val externalImagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val externalDocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

            // getting limited new messages cause deleting old messages
            if (isLastMessageReallyOld(lastMessage)) {
                viewModel.deleteAllMessagesInChannel(chatChannel.chatChannelId)

                // getting 50 new messages
                viewModel.getLatestMessages(chatChannel.chatChannelId, 50) {
                    if (it.isSuccessful) {
                        if (externalImagesDir != null && externalDocumentsDir != null) {
                            val messages = it.result.toObjects(Message::class.java)
                            viewModel.insertChannelMessages(chatChannel, externalImagesDir, externalDocumentsDir, messages)
                        }
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            }

            // getting all messages since this message
            if (isLastMessageRelativelyNew(lastMessage)) {
                if (externalImagesDir != null && externalDocumentsDir != null) {
                    viewModel.getLatestMessagesAfter(externalImagesDir, externalDocumentsDir, lastMessage, chatChannel)
                }
            }
        }
    }

    private fun addChannelListener(currentUser: User, chatChannel: ChatChannel) = lifecycleScope.launch {

        val externalImagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val externalDocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        Firebase.firestore.collection("chatChannels")
            .document(chatChannel.chatChannelId)
            .collection("messages")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(chatChannel.contributorsCount)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null) {
                    val messages = value.toObjects(Message::class.java)
                    if (externalDocumentsDir != null && externalImagesDir != null) {

                        val nonUpdatedMessages = messages.filter { message ->
                            !message.deliveryList.contains(currentUser.id)
                        }

                        // update chat channel
                        // insert the new messages
                        viewModel.insertChatChannelsWithoutProcessing(listOf(chatChannel))
                        viewModel.insertChannelMessages(chatChannel, externalImagesDir, externalDocumentsDir, messages)

                        // update the delivery list
                        viewModel.updateDeliveryListOfMessages(chatChannel, currentUser.id, nonUpdatedMessages) { it1 ->
                            if (!it1.isSuccessful) {
                                viewModel.setCurrentError(it1.exception)
                            }
                        }
                    }
                }
            }

    }

    fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }

        selectImageLauncher.launch(intent)
    }

    fun selectProjectImages() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        selectProjectImageLauncher.launch(intent)
    }

    fun selectChatUploadDocuments() {
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        selectChatDocumentsUploadLauncher.launch(intent)

    }

    fun selectChatUploadImages() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        selectChatImagesUploadLauncher.launch(intent)
    }

    fun selectMoreProjectImages() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        selectMoreProjectImageLauncher.launch(intent)
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onLocationClick(address: Address) {
        val formattedAddress = address.getAddressLine(0).orEmpty()
        viewModel.setCurrentProjectLocation(formattedAddress)
        navController.navigateUp()
    }

    override fun onProjectClick(project: Project) {
        val bundle = bundleOf("title" to project.title, "project" to project)
        navController.navigate(R.id.projectFragment, bundle, slideRightNavOptions())
    }

    override fun onProjectLikeClick(project: Project) {
        viewModel.onLikePressed(project)
    }

    override fun onProjectSaveClick(project: Project) {
        viewModel.onSavePressed(project)
    }

    override fun onProjectJoinClick(project: Project) {
        viewModel.onJoinProject(project)
    }

    // project request must have project included inside it
    override fun onProjectRequestAccept(projectRequest: ProjectRequest) {
        viewModel.acceptRequest(projectRequest)
    }

    // project request must have project included inside it
    override fun onProjectRequestCancel(projectRequest: ProjectRequest) {
        viewModel.rejectRequest(projectRequest)
    }

    override fun onProjectCreatorClick(project: Project) {
        if (project.creator.userId == viewModel.currentUser.value?.id) {
            navController.navigate(R.id.action_homeFragment_to_profileFragment, null, slideRightNavOptions())
        } else {
            viewModel.getOtherUser(project.creator.userId) {
                if (it.isSuccessful && it.result.exists()) {
                    val user = it.result.toObject(User::class.java)!!
                    onUserClick(user)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    @ExperimentalPagingApi
    override fun onProjectCommentClick(project: Project) {
//        val bundle = bundleOf("parent" to project, "title" to project.title)

        showDialog(project)


    /*when (navController.currentDestination?.id) {
            R.id.homeFragment -> {
                navController.navigate(R.id.action_homeFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.projectFragment -> {
                navController.navigate(R.id.action_projectFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
        }*/
    }

    @ExperimentalPagingApi
    fun showDialog(project: Project) {
        val fragmentManager = supportFragmentManager
        val newFragment = CommentsFragment.newInstance(project.commentChannel, project.title, project)
        // The device is smaller, so show the fragment fullscreen
        val transaction = fragmentManager.beginTransaction()
        // For a little polish, specify a transition animation
        transaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction
            .add(android.R.id.content, newFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onProjectOptionClick(viewHolder: ProjectViewHolder, project: Project) {

        val currentUser = viewModel.currentUser.value!!
        val creatorId = project.creator.userId
        val isCurrentUser = creatorId == currentUser.id

        val name = project.creator.name
        val isCreatorLiked = currentUser.likedUsers.contains(creatorId)

        val likeDislikeUserText = if (isCreatorLiked) {
            "Dislike $name"
        } else {
            "Like $name"
        }

        val saveUnSaveText = if (project.isSaved) {
            "Unsave"
        } else {
            "Save"
        }

        val choices = if (isCurrentUser) {
            arrayOf(saveUnSaveText, "Delete")
        } else {
            arrayOf(likeDislikeUserText, saveUnSaveText)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(project.title)
            .setItems(choices) { _, index ->
                if (isCurrentUser) {
                    when (index) {
                        0 -> {
                            viewHolder.onSaveProjectClick(project)
                        }
                        1 -> {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Deleting project")
                                .setMessage("Are you sure you want to delete this project?")
                                .setPositiveButton("Delete") { _, _ ->
                                    viewModel.deleteProject(project) {
                                        if (it.isSuccessful) {
                                            toast("Project Deleted")
                                        } else {
                                            toast("Something went wrong. ${it.exception?.localizedMessage.orEmpty()}")
                                        }
                                    }
                                }.setNegativeButton("Cancel") { a, _ ->
                                    a.dismiss()
                                }.show()
                        }
                    }
                } else {
                    when (index) {
                        0 -> {
                            if (isCreatorLiked) {
                                viewModel.dislikeUser(creatorId)
                            } else {
                                viewModel.likeUser(creatorId)
                            }
                        }
                        1 -> {
                            viewHolder.onSaveProjectClick(project)
                        }
                    }
                }
            }
            .show()

    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> {
                navController.navigate(R.id.action_homeFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.projectFragment -> {
                navController.navigate(R.id.action_projectFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.chatDetailFragment -> {
                navController.navigate(R.id.action_chatDetailFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.chatFragment -> {
                navController.navigate(R.id.action_chatFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.projectContributorsFragment -> {
                navController.navigate(R.id.action_projectContributorsFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
        }
    }

    override fun onCommentLike(comment: Comment) {
        viewModel.onCommentLiked(comment)
    }

    override fun onCommentReply(comment: Comment) {
        viewModel.replyToContent.postValue(comment)
    }

    override fun onClick(comment: Comment) {
        val bundle = bundleOf("parent" to comment, "title" to "Comments")
        viewModel.currentCommentChannelIds.push(comment.threadChannelId)
        navController.navigate(R.id.action_commentsFragment_self, bundle)
    }

    override fun onCommentDelete(comment: Comment) {
        viewModel.deleteComment(comment)
    }

    override fun onCommentUpdate(comment: Comment) {
        viewModel.updateComment(comment)
    }

    override fun onNoUserFound(userId: String) {
        viewModel.deleteUserById(userId)
    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        val bundle = bundleOf("chatChannel" to chatChannel, "title" to chatChannel.projectTitle)
        navController.navigate(R.id.action_homeFragment_to_chatFragment, bundle, slideRightNavOptions())
    }

    override fun onChatChannelSelected(chatChannel: ChatChannel) {

    }

    override fun onChatChannelDeSelected(chatChannel: ChatChannel) {

    }

    override fun onStartDownload(message: Message, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit) {
        if (message.type == image) {
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                createNewFileAndDownload(it, message, onComplete)
            }
        } else if (message.type == document) {
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let {
                createNewFileAndDownload(it, message, onComplete)
            }
        }
    }

    override fun onDocumentClick(message: Message) {
        val externalDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val destination = File(externalDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file) 
    }

    override fun onImageClick(view: View, message: Message, pos: Int, id: String) {
        viewModel.chatScrollPositions[message.chatChannelId] = pos

        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val name = message.content + message.metadata!!.ext
        val destination = File(imagesDir, message.chatChannelId)
        val file = File(destination, name)
        val uri = Uri.fromFile(file)
        val bundle = bundleOf("fullscreenImage" to uri.toString(), "title" to message.sender.name, "transitionName" to id, "ext" to message.metadata!!.ext, "message" to message)
        val extras = FragmentNavigatorExtras(view to id)

        when (navController.currentDestination?.id) {
            R.id.chatFragment -> {
                navController.navigate(R.id.action_chatFragment_to_imageViewFragment, bundle, null, extras)
            }
            R.id.chatDetailFragment -> {
                navController.navigate(R.id.action_chatDetailFragment_to_imageViewFragment, bundle, null, extras)
            }
            R.id.chatMediaFragment -> {
                navController.navigate(R.id.action_chatMediaFragment_to_imageViewFragment, bundle, null, extras)
            }
        }
    }

    private fun createNewFileAndDownload(externalFilesDir: File, message: Message, onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit){

        val name = message.content + message.metadata!!.ext
        val destination = File(externalFilesDir, message.chatChannelId)

        fun download(des: File) {
            val file = File(des, name)
            try {
                if (file.createNewFile()) {
                    FireUtility.downloadMedia(file, message.content, message) {
                        onComplete(it, message)
                        if (it.isSuccessful) {
                            message.isDownloaded = true
                            viewModel.updateMessage(message)
                        } else {
                            file.delete()
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                } else {
                    if (file.exists()) {
                        FireUtility.downloadMedia(file, message.content, message) {
                            onComplete(it, message)
                            if (it.isSuccessful) {
                                message.isDownloaded = true
                                viewModel.updateMessage(message)
                            } else {
                                file.delete()
                                viewModel.setCurrentError(it.exception)
                            }
                        }
                    }
                    Log.d(TAG, "Probably file already exists. Or some other problem for which we are not being able to ")
                }
            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            } finally {
                Log.d(TAG, file.path)
            }
        }

        try {
            if (destination.mkdir()) {
                download(destination)
            } else {
                Log.d(TAG, "Probably directory already exists")
                if (destination.exists()) {
                    download(destination)
                } else {
                    throw Exception("Unknown error. Couldn't create file and download.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage.orEmpty())
        } finally {
            Log.d(TAG, destination.path)
        }
    }

    private fun openFile(file: File) {
        // Get URI and MIME type of file
        try {
            Log.d(TAG, file.path)
            val uri = FileProvider.getUriForFile(this, "com.jamid.codesquare.fileprovider", file)
            val mime = contentResolver.getType(uri)

            // Open file with user selected app
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, e.localizedMessage.orEmpty())
        }

    }

    // make sure to change this later on for null safety
    override fun onMessageRead(message: Message) {
        val a1 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val a2 = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

        val currentUser = viewModel.currentUser.value!!
        if (!message.readList.contains(currentUser.id)) {
            viewModel.updateReadList(currentUser, a1, a2, message)
        }
    }

    override fun onUserClick(message: Message) {
        onUserClick(message.sender)
    }

    override fun onForwardClick(view: View, message: Message) {
        val forwardFragment = ForwardFragment.newInstance(arrayListOf(message))
        forwardFragment.show(supportFragmentManager, "ForwardFragment")
    }

    override fun onMessageLongClick(message: Message) {
        viewModel.updateRestOfTheMessages(message.chatChannelId, 0)

        lifecycleScope.launch {
            delay(300)
            message.state = 1
            onMessageStateChanged(message)
        }

    }

    override fun onMessageStateChanged(message: Message) {
        viewModel.updateMessage(message)
    }

    override suspend fun onGetMessageReply(replyTo: String): Message? {
        return viewModel.getLocalMessage(replyTo)
    }

    override suspend fun onGetMessageReplyUser(senderId: String): User? {
        return viewModel.getLocalUser(senderId)
    }

    override fun onMessageDoubleClick(message: Message) {
        viewModel.setCurrentlySelectedMessage(message)
    }

    override fun onPause() {
        super.onPause()
        // TODO("Need to hold the last chat channel id value")
        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            for (channel in currentUser.chatChannels) {
                viewModel.updateRestOfTheMessages(channel, -1)
            }
        }
    }

}
