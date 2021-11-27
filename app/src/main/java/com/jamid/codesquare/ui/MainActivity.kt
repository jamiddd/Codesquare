package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.net.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.LocationItemClickListener
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.home.chat.ChatInterface
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URL

class MainActivity: LauncherActivity(), LocationItemClickListener, ProjectClickListener, ProjectRequestListener, UserClickListener, CommentListener, ChatChannelClickListener, MessageListener, NotificationItemClickListener {

    private var networkFlag = false
    private var loadingDialog: AlertDialog? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var networkCallback: MyNetworkCallback

    private fun setCurrentUserPhotoAsDrawable(photo: String) = lifecycleScope.launch (Dispatchers.IO) {
        val currentSavedBitmap = viewModel.currentUserBitmap
        if (currentSavedBitmap != null) {
            val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
            setBitmapDrawable(d)
        } else {
            try {
                val isNetworkAvailable = viewModel.isNetworkAvailable.value
                if (isNetworkAvailable != null && isNetworkAvailable) {
                    val url = URL(photo)
                    val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    viewModel.currentUserBitmap = getCircleBitmap(image)
                    val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
                    setBitmapDrawable(d)
                }
            } catch (e: IOException) {
                viewModel.setCurrentError(e)
            }
        }
    }

    private fun setBitmapDrawable(drawable: BitmapDrawable) = runOnUiThread {
        if (binding.mainToolbar.menu.size() > 1) {
            binding.mainToolbar.menu.getItem(3).icon = drawable
        }
    }

    private fun getLatestNotifications() {
        viewModel.getLatestNotifications()
    }

    private fun startNetworkCallback() {
        networkCallback = MyNetworkCallback(viewModel)
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(networkCallback)
        } else {
            cm.registerNetworkCallback(
                builder.build(), networkCallback
            )
        }
    }

    private fun stopNetworkCallback() {
        val cm: ConnectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
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

    private fun getMultipleImageIntent(): Intent {
        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png")

        return Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }

    private fun checkForNetworkPermissions(onCheck: (granted: Boolean) -> Unit) {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED -> {
                onCheck(true)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_NETWORK_STATE) -> {
                MaterialAlertDialogBuilder(this).setTitle("This app requires permission to check your internet connection ...")
                    .setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            else -> {
                onCheck(false)
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

    fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
    }

    fun selectProjectImages() {
        selectProjectImageLauncher.launch(getMultipleImageIntent())
    }

    fun selectChatUploadDocuments() {
        selectChatDocumentsUploadLauncher.launch(getMultipleImageIntent())
    }

    fun selectChatUploadImages() {
        selectChatImagesUploadLauncher.launch(getMultipleImageIntent())
    }

    fun selectReportUploadImages() {
        selectReportImagesLauncher.launch(getMultipleImageIntent())
    }

    fun selectMoreProjectImages() {
        selectMoreProjectImageLauncher.launch(getMultipleImageIntent())
    }

    // needs checking
    fun onLinkClick(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    /*fun showSnackBar(s: String) {
        Snackbar.make(binding.root, s, Snackbar.LENGTH_LONG).show()
    }*/

    fun showLoadingDialog(msg: String) {
        val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
        val loadingLayoutBinding = LoadingLayoutBinding.bind(loadingLayout)

        loadingLayoutBinding.loadingText.text = msg

        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(loadingLayout)
            .setCancelable(false)
            .show()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.mainToolbar)

        // must
        MyNotificationManager.init(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        binding.mainToolbar.setupWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val userInfoLayout = findViewById<View>(R.id.user_info)

            binding.mainToolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }

            onBackPressedDispatcher.addCallback {
                navController.navigateUp()
            }

            binding.mainToolbar.isTitleCentered = destination.id != R.id.homeFragment
            binding.mainToolbar.logo = if (destination.id != R.id.homeFragment) {
                null
            } else {
                ContextCompat.getDrawable(this, R.drawable.ic_collab_logo_small)
            }

            val sharedPreferences = getSharedPreferences("codesquare_shared", MODE_PRIVATE)
            val isInitiatedOnce = sharedPreferences.getBoolean("is_initiated_once", false)

            when (destination.id) {
                R.id.splashFragment -> {
                    binding.mainAppbar.hide()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params
                }
                R.id.reportFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.notificationFragment -> {
                    userInfoLayout?.hide()
                    binding.mainTabLayout.hide()
                    binding.mainToolbar.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.emailVerificationFragment -> {
                    binding.mainAppbar.hide()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params
                }
                R.id.onBoardingFragment -> {
                    binding.mainAppbar.hide()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params
                }
                R.id.loginFragment -> {
                    binding.mainAppbar.hide()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params

                    if (!isInitiatedOnce) {
                        navController.navigate(R.id.action_loginFragment_to_onBoardingFragment, null, slideRightNavOptions())
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("is_initiated_once", true)
                        editor.apply()
                    } else {
                        userInfoLayout?.hide()
                        userInfoLayout?.updateLayout(margin = 0)
                        binding.mainTabLayout.hide()
                        binding.mainToolbar.hide()
                    }
                }
                R.id.homeFragment -> {
                    binding.mainAppbar.show()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params

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
                    binding.mainAppbar.show()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params


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
                R.id.searchFragment -> {
                    userInfoLayout?.hide()
                    binding.mainToolbar.show()
                    binding.mainTabLayout.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                }
                R.id.settingsFragment -> {
                    userInfoLayout?.hide()
                    binding.mainToolbar.show()
                    binding.mainTabLayout.hide()
                }
            }
        }

        viewModel.currentUser.observe(this) {
            if (it != null) {

                NotificationProvider.init(it)

                lifecycleScope.launch (Dispatchers.IO) {
                    try {
                        val isNetworkAvailable = viewModel.isNetworkAvailable.value
                        if (isNetworkAvailable != null && isNetworkAvailable) {
                            val url = URL(it.photo)
                            val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            viewModel.currentUserBitmap = getCircleBitmap(image)
                        }
                    } catch (e: IOException) {
                        viewModel.setCurrentError(e)
                    }
                }
            }
        }

        viewModel.errors.observe(this) {
            if (it != null) {
                Log.d(TAG, it.localizedMessage ?: "Unknown error")
            }
        }

        viewModel.currentError.observe(this) { exception ->
            if (exception != null) {
                loadingDialog?.dismiss()
                Log.e(TAG, exception.localizedMessage.orEmpty())
            }
        }

        Firebase.auth.addAuthStateListener {
            loadingDialog?.dismiss()
            val firebaseUser = it.currentUser
            val currentDestination = navController.currentDestination?.id
            if (firebaseUser != null) {
                if (firebaseUser.isEmailVerified) {

                    UserManager.init(firebaseUser.uid)
                    lifecycleScope.launch {
                        ChatInterface.initialize(firebaseUser.uid)
                    }

                    if (currentDestination == R.id.loginFragment) {
                        // login fragment will decide
                    } else if (currentDestination == R.id.splashFragment) {
                        lifecycleScope.launch {
                            delay(4000)
                            val v = findViewById<View>(R.id.splash_logo)
                            val extras = FragmentNavigatorExtras(v to "logo_transition")
                            navController.navigate(R.id.action_splashFragment_to_homeFragment, null, null, extras)
                        }
                    }
                } else {
                    when (currentDestination) {
                        R.id.homeFragment -> {
                            navController.navigate(R.id.action_homeFragment_to_emailVerificationFragment, null, slideRightNavOptions())
                        }
                        R.id.createAccountFragment -> {
                            navController.navigate(R.id.action_createAccountFragment_to_emailVerificationFragment, null, slideRightNavOptions())
                        }
                        R.id.splashFragment -> {
                            val v = findViewById<View>(R.id.splash_logo)
                            val extras = FragmentNavigatorExtras(v to "logo_transition")
                            navController.navigate(R.id.action_splashFragment_to_loginFragment, null, null, extras)
                        }
                    }
                }
            } else {
                lifecycleScope.launch {
                    delay(4000)
                    if (currentDestination == R.id.homeFragment) {
                        navController.navigate(R.id.action_homeFragment_to_loginFragment, null, slideRightNavOptions())
                    } else if (currentDestination == R.id.splashFragment) {
                        val v = findViewById<View>(R.id.splash_logo)
                        val extras = FragmentNavigatorExtras(v to "logo_transition")
                        navController.navigate(R.id.action_splashFragment_to_loginFragment, null, null, extras)
                    }
                }
            }
        }

        viewModel.isNetworkAvailable.observe(this) {
            if (it != null) {
                if (it) {
                    if (networkFlag) {
                        Snackbar.make(binding.root, "Network connected", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    networkFlag = true
                    Snackbar.make(binding.root, "Network connection unavailable", Snackbar.LENGTH_INDEFINITE).setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.error_color)).show()
                }
            }
        }

        startNetworkCallback()

//        getLatestNotifications()

        viewModel.allUnreadNotifications.observe(this) {
            if (it.isNotEmpty()) {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    val badgeDrawable = BadgeDrawable.create(this)
                    badgeDrawable.number = it.size
                    attachBadgeDrawable(badgeDrawable, binding.mainToolbar, R.id.notifications)
                }
            }
        }

        UserManager.currentUser.observe(this) {
            if (it != null) {
                val photo = it.photo
                if (photo != null) {
                    setCurrentUserPhotoAsDrawable(photo)
                }

                // looking for newly added chat channel
                lifecycleScope.launch (Dispatchers.IO) {
                    for (channel in it.chatChannels) {
                        val chatChannel = viewModel.getLocalChatChannel(channel)
                        if (chatChannel == null) {
                            when (val result = viewModel.getChatChannel(channel)) {
                                is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                                is Result.Success -> {
                                    if (result.data.exists()) {
                                        val chatChannel1 = result.data.toObject(ChatChannel::class.java)!!
                                        ChatInterface.addChannelMessagesListener(chatChannel1)
                                        viewModel.insertChatChannels(listOf(chatChannel1))
                                    }
                                }
                            }
                        }
                    }
                }

                viewModel.insertCurrentUser(it)
            }
        }

        ChatInterface.currentData.observe(this) {
            val result = it ?: return@observe

            when (result) {
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
                is Result.Success -> {
                    val data = result.data
                    for (item in data) {
                        viewModel.insertChannelWithContributors(item.channel, item.contributors)
                    }

                    ChatInterface.addChannelsListener { it1 ->
                        viewModel.insertChatChannels(it1)
                    }
                }
            }
        }

        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        ChatInterface.channelMessagesMap.observe(this) {
            if (it.isNotEmpty()) {
                if (imagesDir != null && documentsDir != null) {
                    for (i in it) {
                        viewModel.insertChannelMessages(i.channel, imagesDir, documentsDir, i.messages)
                    }
                }
            }
        }

        NotificationProvider.newNotifications.observe(this) {
            if (!it.isNullOrEmpty()) {
                viewModel.insertNotifications(it)
            } else {
                Log.d(TAG, "No notifications provided.")
            }
        }

    }

    override fun onLocationClick(address: Address) {
        val formattedAddress = address.getAddressLine(0).orEmpty()
        viewModel.setCurrentProjectLocation(formattedAddress)
        navController.navigateUp()
    }

    override fun onProjectClick(project: Project) {
        val bundle = bundleOf("title" to project.title, "project" to project)
        navController.navigate(R.id.projectFragment, bundle)
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
        val bundle = bundleOf("commentChannelId" to project.commentChannel, "parent" to project, "title" to project.title)

        when (navController.currentDestination?.id) {
            R.id.homeFragment -> {
                navController.navigate(R.id.action_homeFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.projectFragment -> {
                navController.navigate(R.id.action_projectFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.notificationFragment -> {
                navController.navigate(R.id.action_notificationFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
        }
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
            arrayOf(likeDislikeUserText, saveUnSaveText, "Report")
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
                        2 -> {

                            val bundle = bundleOf("contextObject" to project)

                            if (navController.currentDestination?.id == R.id.homeFragment) {
                                navController.navigate(R.id.action_homeFragment_to_reportFragment, bundle)
                            } else if (navController.currentDestination?.id == R.id.profileFragment) {
                                navController.navigate(R.id.action_profileFragment_to_reportFragment, bundle)
                            }
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
                navController.navigate(R.id.action_homeFragment_to_profileFragment, bundle)
            }
            R.id.projectFragment -> {
                navController.navigate(R.id.action_projectFragment_to_profileFragment, bundle)
            }
            R.id.chatDetailFragment -> {
                navController.navigate(R.id.action_chatDetailFragment_to_profileFragment, bundle)
            }
            R.id.chatFragment -> {
                navController.navigate(R.id.action_chatFragment_to_profileFragment, bundle)
            }
            R.id.projectContributorsFragment -> {
                navController.navigate(R.id.action_projectContributorsFragment_to_profileFragment, bundle)
            }
            R.id.profileFragment -> {
                navController.navigate(R.id.action_profileFragment_self, bundle)
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
        when (navController.currentDestination?.id) {
            R.id.commentsFragment -> {
                navController.navigate(R.id.action_commentsFragment_self, bundle)
            }
            R.id.notificationFragment -> {
                navController.navigate(R.id.action_notificationFragment_to_commentsFragment, bundle)
            }
        }
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

    override fun onReportClick(comment: Comment) {
        navController.navigate(R.id.action_commentsFragment_to_reportFragment, bundleOf("contextObject" to comment))
    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        val bundle = bundleOf("chatChannel" to chatChannel, "title" to chatChannel.projectTitle)
        navController.navigate(R.id.action_homeFragment_to_chatFragment, bundle)
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

    override fun onMessageDoubleClick(message: Message) {
        viewModel.setCurrentlySelectedMessage(message)
    }

    override fun onNotificationRead(notification: Notification) {
        notification.read = true
        viewModel.updateNotification(notification)
    }

    @ExperimentalPagingApi
    override fun onNotificationClick(contextObject: Any) {
        when (contextObject) {
            is Project -> {
                onProjectClick(contextObject)
            }
            is Comment -> {
                if (contextObject.commentLevel == 0.toLong()) {
                    val projectRef = Firebase.firestore.collection("projects").document(contextObject.projectId)
                    FireUtility.getDocument(projectRef) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val project = it1.result.toObject(Project::class.java)!!
                            onProjectCommentClick(project)
                        } else {
                            Log.d(TAG, "Something went wrong" + it1.exception?.localizedMessage)
                        }
                    }
                } else {
                    onClick(contextObject)
                }
            }
            is User -> {
                onUserClick(contextObject)
            }
            else -> throw IllegalArgumentException("Only object of type Project, Comment and User is allowed.")
        }
    }

    override suspend fun onGetMessageReply(replyTo: String): Message? {
        return viewModel.getLocalMessage(replyTo)
    }

    override suspend fun onGetMessageReplyUser(senderId: String): User? {
        return viewModel.getLocalUser(senderId)
    }

    override fun onPause() {
        super.onPause()
        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            for (channel in currentUser.chatChannels) {
                viewModel.updateRestOfTheMessages(channel, -1)

                FirebaseMessaging.getInstance().subscribeToTopic(channel)
                    .addOnCompleteListener {
                        //
                    }

            }
        }

        stopNetworkCallback()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            for (channel in currentUser.chatChannels) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(channel)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d(TAG, "Unsubscribed from $channel")
                        } else {
                            Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                        }
                    }
            }
        }

        checkForNetworkPermissions {
            if (it) {
                startNetworkCallback()
            }
        }

        if (navController.currentDestination?.id == R.id.emailVerificationFragment) {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null) {
                val task = firebaseUser.reload()
                task.addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (Firebase.auth.currentUser?.isEmailVerified == true) {
                            navController.navigate(R.id.action_emailVerificationFragment_to_homeFragment)
                        }
                    } else {
                        Log.d(TAG, it.exception?.localizedMessage.orEmpty())
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivityDebugTag"
    }

}
