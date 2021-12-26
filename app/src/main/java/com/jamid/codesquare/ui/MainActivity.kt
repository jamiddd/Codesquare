package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.*
import android.os.Build
import android.os.Environment
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.paging.ExperimentalPagingApi
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.*
import com.jamid.codesquare.listeners.LocationItemClickListener
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalPagingApi
class MainActivity: LauncherActivity(), LocationItemClickListener, ProjectInviteListener, ProjectClickListener, ProjectRequestListener, UserClickListener, ChatChannelClickListener, MessageListener, NotificationItemClickListener, CommentListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var networkCallback: MyNetworkCallback
    private var previouslyFetchedLocation: Location? = null
    private var currentlyFocusedMessage: Message? = null
    private var currentMessageView: View? = null
    private var isImageViewMode = false
    private var currentImageViewer: View? = null
    private var animationStartView: View? = null
    private var animationEndView: View? = null


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
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectChatDocumentsUploadLauncher.launch(intent)
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

    fun showLoadingDialog(msg: String): AlertDialog {
        val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
        val loadingLayoutBinding = LoadingLayoutBinding.bind(loadingLayout)

        loadingLayoutBinding.loadingText.text = msg

        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(loadingLayout)
            .setCancelable(false)
            .show()

        return loadingDialog!!
    }

    @SuppressLint("UnsafeOptInUsageError", "VisibleForTests")
    override fun onCreate() {
        super.onCreate()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setSupportActionBar(binding.mainToolbar)

        // must
        MyNotificationManager.init(this)
        startNetworkCallback()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val userInfoLayout = findViewById<View>(R.id.user_info)

            binding.mainToolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }

            binding.mainProgressBar.hide()

            onBackPressedDispatcher.addCallback {
                if (isImageViewMode) {
                    removeImageViewFragment()
                } else {
                    navController.navigateUp()
                }
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
                R.id.splashFragment -> updateUi(
                    shouldShowAppBar = false,
                    baseFragmentBehavior = null
                )
                R.id.notificationCenterFragment -> updateUi(shouldShowTabLayout = true)
                R.id.onBoardingFragment -> updateUi(
                    shouldShowAppBar = false,
                    baseFragmentBehavior = null
                )
                R.id.loginFragment -> {
                    updateUi(
                        shouldShowAppBar = false,
                        baseFragmentBehavior = null
                    )

                    // TODO("Change this")
                    if (!isInitiatedOnce) {
                        navController.navigate(R.id.action_loginFragment_to_onBoardingFragment, null, slideRightNavOptions())
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("is_initiated_once", true)
                        editor.apply()
                    }
                }
                R.id.homeFragment -> {
                    updateUi(
                        shouldShowTabLayout = true
                    )
                    hideKeyboard(binding.root)
                }
                R.id.profileFragment -> {
                    updateUi(
                        shouldShowUserInfo = true,
                        shouldShowTabLayout = true
                    )
                    userInfoLayout?.updateLayout(marginTop = convertDpToPx(56))
                    hideKeyboard(binding.root)
                }
                R.id.projectFragment -> {
                    updateUi(shouldShowPrimaryAction = true)
                }
                R.id.chatMediaFragment -> {
                    updateUi(shouldShowTabLayout = true)
                }
                R.id.imageViewFragment -> {
                    updateUi(baseFragmentBehavior = null)
                }
                R.id.searchFragment -> {
                    updateUi(toolbarAdjustment = ToolbarAdjustment(true, R.color.normal_grey, false), shouldShowTabLayout = true)
                }
                else -> {
                    updateUi()
                }
                /*R.id.reportFragment, R.id.emailVerificationFragment, R.id.createProjectFragment, R.id.editProfileFragment, R.id.projectRequestFragment, R.id.savedProjectsFragment, R.id.commentsFragment, R.id.chatFragment, R.id.chatDetailFragment, R.id.projectContributorsFragment, R.id.tagFragment, R.id.preSearchFragment, R.id.settingsFragment, R.id.forgotPasswordFragment, R.id.updatePasswordFragment, R.id.messageDetailFragment -> {
                    updateUi()
                } */
            }
        }

        viewModel.currentUser.observe(this) {
            if (it != null) {
                if (previouslyFetchedLocation != null) {
                    it.location = previouslyFetchedLocation
                    viewModel.insertCurrentUser(it)
                    return@observe
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

        UserManager.authState.observe(this) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                    if (UserManager.isEmailVerified) {
                        listenForNotifications()
                    }
                } else {
                    viewModel.isNetworkAvailable.removeObservers(this)
                }
            }
        }

        viewModel.allUnreadNotifications.observe(this) {
            if (it.isNotEmpty()) {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    val badgeDrawable = BadgeDrawable.create(this)
                    badgeDrawable.number = it.size
                    attachBadgeDrawable(badgeDrawable, binding.mainToolbar, R.id.notifications)
                }
            }
        }

        UserManager.currentUserLive.observe(this) {
            if (it != null) {
              /*  // looking for newly added chat channel
                lifecycleScope.launch (Dispatchers.IO) {
                    for (channel in it.chatChannels) {
                        val chatChannel = viewModel.getLocalChatChannel(channel)
                        if (chatChannel == null) {
                            when (val result = viewModel.getChatChannel(channel)) {
                                is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                                is Result.Success -> {
                                    if (result.data.exists()) {
                                        val chatChannel1 = result.data.toObject(ChatChannel::class.java)!!
//                                        ChatInterface.addChannelMessagesListener(chatChannel1)
                                        viewModel.insertChatChannels(listOf(chatChannel1))
                                    }
                                }
                            }
                        }
                    }
                }*/
                viewModel.insertCurrentUser(it)
            }
        }

        viewModel.isNetworkAvailable.observe(this) {
            if (it == true) {
                LocationProvider.initialize(fusedLocationProviderClient, this)
            } else {
                toast("Network not available")
            }
        }

        val extras = intent.extras
        if (extras != null) {
            when {
                extras.containsKey("channelId") -> {
                    // chat notification
                    val chatChannelId = extras["channelId"] as String?
                    if (chatChannelId != null) {
                        val ref = Firebase.firestore.collection("chatChannels").document(chatChannelId)
                        ref.get().addOnSuccessListener {
                            if (it.exists()) {
                                val chatChannel = it.toObject(ChatChannel::class.java)
                                if (chatChannel != null) {
                                    onChannelClick(chatChannel)
                                }
                            }
                        }.addOnFailureListener {
                            Log.e(TAG, "670 => " + it.localizedMessage.orEmpty())
                        }
                    }
                }
                extras.containsKey("notificationId") -> {

                    if (extras.containsKey("type")) {
                        val type = extras["type"] as String?
                        if (!type.isNullOrBlank()) {
                            if (type.isDigitsOnly()) {
                                val t = type.toInt()
                                navController.navigate(R.id.notificationCenterFragment, bundleOf("type" to t), slideRightNavOptions())
                            } else {
                                navController.navigate(R.id.notificationCenterFragment, bundleOf("type" to 0), slideRightNavOptions())
                            }

                            /*if (extras.containsKey("projectId")) {
                                val projectId = extras["projectId"] as String?
                                if (!projectId.isNullOrBlank()) {
                                    //
                                }
                            }

                            if (extras.containsKey("userId")) {
                                val userId = extras["userId"] as String?
                                if (!userId.isNullOrBlank()) {
                                    //
                                }
                            }

                            if (extras.containsKey("commentId")) {
                                val commentId = extras["commentId"] as String?
                                if (!commentId.isNullOrBlank()) {
                                    //
                                }
                            }

                            if (extras.containsKey("commentChannelId")) {
                                val commentChannelId = extras["commentChannelId"] as String?
                                if (!commentChannelId.isNullOrBlank()) {
                                    //
                                }
                            }*/
                        } else {
                            navController.navigate(R.id.notificationCenterFragment, bundleOf("type" to 0), slideRightNavOptions())
                        }
                    }
                   /* val type = extras["type"] as String?
                    val notificationId = extras["notificationId"] as String?
                    val receiverId = extras["receiverId"] as String?
                    if (notificationId != null && receiverId != null && type != null) {
                        if (type == "request") {
                            val query = Firebase.firestore.collection("projectRequests")
                                .whereEqualTo("projectId", contextId)
                                .limit(1)

                            query.get()
                                .addOnSuccessListener {
                                    if (!it.isEmpty) {
                                        val projectRequest = it.toObjects(ProjectRequest::class.java).first()
                                        viewModel.insertProjectRequests(projectRequest)
                                        navController.navigate(R.id.action_homeFragment_to_projectRequestFragment, null, slideRightNavOptions())
                                    }
                                }.addOnFailureListener {
                                    Log.e(TAG, "694 => " + it.localizedMessage.orEmpty())
                                }

                        } else {
                            val ref = Firebase.firestore.collection("users").document(receiverId)
                            .collection("notifications").document(notificationId)

                            ref.get().addOnSuccessListener {
                                if (it.exists()) {
                                    val notification = it.toObject(Notification::class.java)
                                    if (notification != null) {
                                        viewModel.insertNotifications(notification)
                                        navController.navigate(R.id.action_homeFragment_to_notificationCenterFragment, null, slideRightNavOptions())
                                    }
                                }
                            }.addOnFailureListener {
                                Log.e(TAG, "710 => " + it.localizedMessage.orEmpty())
                            }

                        }
                    }*/
                }
                else -> {
                    // nothing
                }
            }
        }

        clearActiveNotifications()

    }

    private fun updateUi(
        shouldShowAppBar: Boolean = true,
        shouldShowToolbar: Boolean = true,
        shouldShowPrimaryAction: Boolean = false,
        shouldShowTabLayout: Boolean = false,
        baseFragmentBehavior: CoordinatorLayout.Behavior<View>? = AppBarLayout.ScrollingViewBehavior(),
        toolbarAdjustment: ToolbarAdjustment = ToolbarAdjustment(),
        shouldShowUserInfo: Boolean = false
    ) {
        binding.mainAppbar.isVisible = shouldShowAppBar
        binding.mainToolbar.isVisible = shouldShowToolbar
        binding.mainTabLayout.isVisible = shouldShowTabLayout

        if (shouldShowAppBar){
            if (binding.mainAppbar.translationY != 0f) {
                binding.mainAppbar.slideReset()
            }
            if (shouldShowToolbar) {
                binding.mainToolbar.apply {
                    isTitleCentered = toolbarAdjustment.isTitleCentered

                    if (isNightMode()) {
                        if (toolbarAdjustment.titleTextColor == R.color.black) {
                            setTitleTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        } else {
                            setTitleTextColor(ContextCompat.getColor(this@MainActivity, toolbarAdjustment.titleTextColor))
                        }
                    } else {
                        setTitleTextColor(ContextCompat.getColor(this@MainActivity, toolbarAdjustment.titleTextColor))
                    }

                    if (!toolbarAdjustment.shouldShowTitle) {
                        title = " "
                    }
                }
            }
        } else {
            binding.mainToolbar.hide()
        }

        if (shouldShowPrimaryAction) {
            binding.mainPrimaryAction.slideReset()
        } else {
            binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
        }

        val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = baseFragmentBehavior
        binding.navHostFragment.layoutParams = params

        val userInfoLayout = findViewById<View>(R.id.user_info)
        userInfoLayout?.isVisible = shouldShowUserInfo

    }


    private fun listenForNotifications() {
        FireUtility.listenForNotifications {
            viewModel.insertNotifications(*it.toTypedArray())
        }
    }

    private fun clearActiveNotifications() {
        NotificationManagerCompat.from(this).cancelAll()
    }

    override fun onLocationClick(place: Place) {
        val latLang = place.latLng
        if (latLang != null) {
            val formattedAddress = place.address.orEmpty()
            val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latLang.latitude, latLang.longitude))
            viewModel.setCurrentProjectLocation(Location(latLang.latitude, latLang.longitude, formattedAddress, hash))
            navController.navigateUp()
        }
    }

    override fun onProjectClick(project: Project) {
        val bundle = bundleOf("title" to project.name, "project" to project)
        navController.navigate(R.id.projectFragment, bundle, slideRightNavOptions())
    }

    override fun onProjectLikeClick(project: Project) {

        val currentUser = UserManager.currentUser

        fun onLike() {
            project.likes = project.likes + 1
            project.isLiked = true
            viewModel.updateLocalProject(project)
        }

        fun onDislike() {
            project.likes = project.likes - 1
            project.isLiked = false
            viewModel.updateLocalProject(project)
        }

        if (project.isLiked) {
            FireUtility.dislikeProject(project) {
                if (it.isSuccessful) {
                    onDislike()
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {
            val content = currentUser.name + " liked your project"
            val title = project.name
            val notification = Notification.createNotification(content, currentUser.id, project.creator.userId, projectId = project.id, title = title)
            FireUtility.likeProject(project) {
                if (it.isSuccessful) {
                    // check if notification already exists
                    if (notification.senderId != notification.receiverId) {
                        FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                            if (error != null) {
                                viewModel.setCurrentError(error)
                                return@checkIfNotificationExistsByContent
                            }
                            if (!exists) {
                                FireUtility.sendNotification(notification) {
                                    onLike()
                                }
                            }
                        }
                    } else {
                        onLike()
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onProjectSaveClick(project: Project) {
        FireUtility.dislikeProject(project) {
            if (it.isSuccessful) {
                project.likes = project.likes - 1
                project.isLiked = false
                viewModel.insertProjects(project)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectJoinClick(project: Project) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " wants to join your project"
        val notification = Notification.createNotification(content, currentUser.id, project.creator.userId, type = 1, userId = currentUser.id, title = project.name)

        fun onRequestSent(projectRequest: ProjectRequest) {
            viewModel.insertNotifications(notification)
            val requestsList = project.requests.addItemToList(projectRequest.requestId)
            project.requests = requestsList
            project.isRequested = true
            viewModel.insertProjectsWithoutProcessing(project)
            viewModel.insertProjectRequests(projectRequest)
        }

        FireUtility.joinProject(notification.id, project) { task, projectRequest ->
            if (task.isSuccessful) {
                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) {
                                    if (it.isSuccessful) {
                                        onRequestSent(projectRequest)
                                    } else {
                                        viewModel.setCurrentError(it.exception)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "It's not possible")
                }
            } else {
                viewModel.setCurrentError(task.exception)
            }
        }
    }

    // project request must have project included inside it
    override fun onProjectRequestAccept(projectRequest: ProjectRequest) {
        viewModel.acceptRequest(projectRequest)
    }

    // project request must have project included inside it
    override fun onProjectRequestCancel(projectRequest: ProjectRequest) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " rejected your project request"
        val title = projectRequest.project?.name
        val notification = Notification.createNotification(content, currentUser.id, projectRequest.senderId, userId = currentUser.id, title = title)
        FireUtility.rejectRequest(projectRequest) {
            if (!it.isSuccessful) {
                viewModel.setCurrentError(it.exception)
            } else {
                FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                    if (error != null) {
                        viewModel.setCurrentError(error)
                    } else {
                        if (!exists) {

                            val requestNotificationId = projectRequest.notificationId
                            if (requestNotificationId != null) {
                                viewModel.deleteNotificationById(requestNotificationId)
                            }

                            FireUtility.sendNotification(notification) {
                                Log.d(TAG, "Project request cancelled")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onProjectRequestProjectDeleted(projectRequest: ProjectRequest) {
        deleteProjectRequest(projectRequest)
    }

    private fun deleteProjectRequest(projectRequest: ProjectRequest) {
        FireUtility.deleteProjectRequest(projectRequest) {
            if (it.isSuccessful) {
                viewModel.deleteProjectRequest(projectRequest)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectRequestSenderDeleted(projectRequest: ProjectRequest) {
        deleteProjectRequest(projectRequest)
    }

    override fun updateProjectRequest(newProjectRequest: ProjectRequest) {
        viewModel.insertProjectRequests(newProjectRequest)
    }

    override fun onProjectCreatorClick(project: Project) {
        if (project.creator.userId == UserManager.currentUserId) {
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


    override fun onProjectCommentClick(project: Project) {
        val bundle = bundleOf("commentChannelId" to project.commentChannel, "parent" to project, "title" to project.name)

        when (navController.currentDestination?.id) {
            R.id.homeFragment -> {
                navController.navigate(R.id.action_homeFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.profileFragment -> {
                navController.navigate(R.id.action_profileFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.projectFragment -> {
                navController.navigate(R.id.action_projectFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
            R.id.notificationCenterFragment -> {
                navController.navigate(R.id.action_notificationCenterFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
        }
    }


   /* private fun likeUser(userId: String) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " liked your profile"
        val notification = Notification.createNotification(content, currentUser.id, userId, userId = userId, title = currentUser.name)
        FireUtility.likeUser(userId, notification) {
            if (!it.isSuccessful) {
                viewModel.setCurrentError(it.exception)
            }
        }
    }*/

    override fun onProjectOptionClick(viewHolder: ProjectViewHolder, project: Project) {
        val currentUser = UserManager.currentUser
        val creatorId = project.creator.userId
        val isCurrentUser = creatorId == currentUser.id
        val name = project.creator.name

        val option1 = "Like $name"
        val option2 = "Dislike $name"
        val option3 = "Save"
        val option4 = "Remove from saved"
        val option5 = "Close project"
        val option6 = "Report"

        val isCreatorLiked = currentUser.likedUsers.contains(creatorId)
        val likeDislikeUserText = if (isCreatorLiked) { option2 } else { option1 }
        val saveUnSaveText = if (project.isSaved) { option4 } else { option3 }
        val choices = if (isCurrentUser) { arrayOf(saveUnSaveText, option5) }
        else { arrayOf(likeDislikeUserText, saveUnSaveText, option6) }

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(project.name)
            .setItems(choices) { _, index ->
                when (choices[index]) {
                    option1 -> {
                        if (isCreatorLiked) {
                            viewModel.dislikeUser(creatorId)
                        } else {
                            viewModel.likeUser(creatorId)
                        }
                    }
                    option2 -> {
                        if (isCreatorLiked) {
                            viewModel.dislikeUser(creatorId)
                        } else {
                            viewModel.likeUser(creatorId)
                        }
                    }
                    option3, option4 -> {
                        viewHolder.onSaveProjectClick(project)
                    }
                    option5 -> {
                        /*
                        archiving project for 30 days. if no action is taken, project will
                        be deleted
                        */
                        viewModel.archiveProject(currentUser.id, project)
                    }
                    option6 -> {
                        val bundle = bundleOf("contextObject" to project)

                        if (navController.currentDestination?.id == R.id.homeFragment) {
                            navController.navigate(R.id.action_homeFragment_to_reportFragment, bundle, slideRightNavOptions())
                        } else if (navController.currentDestination?.id == R.id.profileFragment) {
                            navController.navigate(R.id.action_profileFragment_to_reportFragment, bundle, slideRightNavOptions())
                        }
                    }
                }
            }
            .show()

        alertDialog.window?.setGravity(Gravity.BOTTOM)

    }

    override fun onProjectUndoClick(project: Project, projectRequest: ProjectRequest) {
        FireUtility.undoJoinProject(projectRequest) {
            if (it.isSuccessful) {
                project.isRequested = false
                val newList = project.requests.removeItemFromList(projectRequest.requestId)
                project.requests = newList
                viewModel.updateLocalProject(project)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
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
            R.id.profileFragment -> {
                navController.navigate(R.id.action_profileFragment_self, bundle, slideRightNavOptions())
            }
            R.id.commentsFragment -> {
                navController.navigate(R.id.action_commentsFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.searchFragment -> {
                navController.navigate(R.id.action_searchFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
            R.id.messageDetailFragment -> {
                navController.navigate(R.id.action_messageDetailFragment_to_profileFragment, bundle, slideRightNavOptions())
            }
        }
    }

    override fun onUserOptionClick(projectId: String, chatChannelId: String, view: View, user: User, administrators: List<String>) {

        val option1 = "Set as admin"
        val option2 = "Remove from admin"
        val option3 = "Like ${user.name}"
        val option4 = "Dislike ${user.name}"
        val option5 = "Report ${user.name}"
        val option6 = "Remove from project"
        val option7 = "Leave project"

        val currentUser = UserManager.currentUser
        val currentUserId = currentUser.id
        val isCurrentUserAdministrator = administrators.contains(currentUserId)

        if (currentUser.id == user.id) {
            val choices = arrayOf(option7)

            val alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("You")
                .setItems(choices) { _, index ->
                    when (choices[index]) {
                        option7 -> {
                            viewModel.leaveProject(user, projectId, chatChannelId) {
                                TODO("1. send a notification that the user has left the group to himself" +
                                        "2. send another notification to all other members that current user has left the group")
                            }
                        }
                    }
                }.show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)

        } else {
            val isOtherUserLiked = currentUser.likedUsers.contains(user.id)

            val likeText = if (isOtherUserLiked) {
                option4
            } else {
                option3
            }

            val choices = if (administrators.contains(user.id)) {
                if (isCurrentUserAdministrator) {
                    arrayOf(option2, likeText, option5, option6)
                } else {
                    arrayOf(likeText, option5)
                }
            } else {
                if (isCurrentUserAdministrator) {
                    arrayOf(option1, likeText, option5, option6)
                } else {
                    arrayOf(likeText, option5)
                }
            }

            val alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle(user.name)
                .setItems(choices) { _, index ->
                    when (choices[index]) {
                        option1 -> {
                            viewModel.setOtherUserAsAdmin(chatChannelId, user.id) {
                                if (!it.isSuccessful) {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }
                        }
                        option2 -> {
                            viewModel.removeOtherUserFromAdmin(chatChannelId, user.id) {
                                if (!it.isSuccessful) {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }
                        }
                        option3 -> {
                            viewModel.likeUser(user.id)
//                            viewModel.likeUser(user = user)
                        }
                        option4 -> {
                            viewModel.dislikeUser(user.id)
                        }
                        option5 -> {
                            navController.navigate(R.id.action_chatDetailFragment_to_reportFragment, bundleOf("contextObject" to user), slideRightNavOptions())
                        }
                        option6 -> {
                            viewModel.castVoteToRemoveUser(user, projectId, currentUserId) {
                                if (it.isSuccessful) {
                                    val ad = MaterialAlertDialogBuilder(this)
                                        .setTitle("Collab")
                                        .setMessage("Your vote to remove ${user.name} has been submitted anonymously. No one is notified of this action. As more contributors cast their vote on favor, the decision will be concluded.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK") { a, _ ->
                                            a.dismiss()
                                        }.setNegativeButton("Undo") { _, _ ->
                                            viewModel.undoVoteCast(user, projectId, currentUserId) { it1 ->
                                                if (it1.isSuccessful) {
                                                    Snackbar.make(binding.root, "Your vote has been removed.", Snackbar.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.setCurrentError(it1.exception)
                                                }
                                            }
                                        }
                                        .show()

                                    ad.window?.setGravity(Gravity.BOTTOM)
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }
                        }
                    }
                }
                .show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    override fun onUserLikeClick(user: User) {
        val currentUser = UserManager.currentUser
        if (currentUser.likedUsers.contains(user.id)) {
            // dislike the user
            viewModel.dislikeUser(user.id)
        } else {
            // like the user
            viewModel.likeUser(user.id)
        }
    }

    override fun onCommentLike(comment: Comment) {
        val currentUser = UserManager.currentUser
        FireUtility.likeComment(comment) {
            val content = currentUser.name + " liked your comment"
            val notification = Notification.createNotification(content, currentUser.id, comment.senderId, userId = currentUser.id, title = currentUser.name)
            FireUtility.sendNotification(notification) {
                if (it.isSuccessful) {
                    val newCommentLikesList = comment.likes.toMutableList()
                    comment.likesCount = comment.likesCount - 1
                    comment.isLiked = false
                    newCommentLikesList.remove(currentUser.id)

                    viewModel.updateComment(comment)

                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
        viewModel.onCommentLiked(comment)
    }

    override fun onCommentReply(comment: Comment) {
        viewModel.replyToContent.postValue(comment)
    }

    override fun onClick(comment: Comment) {
        val bundle = bundleOf("parent" to comment, "title" to "Comments", COMMENT_CHANNEL_ID to comment.threadChannelId)
        when (navController.currentDestination?.id) {
            R.id.commentsFragment -> {
                navController.navigate(R.id.action_commentsFragment_self, bundle, slideRightNavOptions())
            }
            R.id.notificationCenterFragment -> {
                navController.navigate(R.id.action_notificationCenterFragment_to_commentsFragment, bundle, slideRightNavOptions())
            }
        }
    }

    override fun onCommentDelete(comment: Comment) {
        FireUtility.deleteComment(comment) {
            if (it.isSuccessful) {
                viewModel.deleteComment(comment)
                toast("Your comment was deleted.")
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onCommentUpdate(comment: Comment) {
        viewModel.updateComment(comment)
    }

    override fun onNoUserFound(userId: String) {
        viewModel.deleteUserById(userId)
    }

    override fun onReportClick(comment: Comment) {
        navController.navigate(R.id.action_commentsFragment_to_reportFragment, bundleOf(
            CONTEXT_OBJECT to comment), slideRightNavOptions())
    }

    override fun onCommentUserClick(user: User) {
        onUserClick(user)
    }

    override fun onOptionClick(comment: Comment) {
        val isCommentByMe = comment.senderId == UserManager.currentUserId

        val option1 = "Report"
        val option2 = "Delete"

        val choices = if (isCommentByMe) {
            arrayOf(option1, option2)
        } else {
            arrayOf(option1)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Comment")
            .setItems(choices) { _, index ->
                when (choices[index]) {
                    option1 -> {
                        onReportClick(comment)
                    }
                    option2 -> {
                        onCommentDelete(comment)
                    }
                }
            }.show()
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

    override fun onBackPressed() {
        if (isImageViewMode) {
            removeImageViewFragment()
        } else {
            super.onBackPressed()
        }
    }

    private fun hideSystemUI() {
        val view = currentImageViewer?.findViewById<View>(R.id.image_view_appbar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (view != null) {
            WindowInsetsControllerCompat(window, view).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDocumentClick(message: Message) {
        val externalDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val destination = File(externalDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file) 
    }

    override fun onImageClick(view: View, message: Message, controllerListener: FrescoImageControllerListener) {
        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val name = message.content + message.metadata!!.ext
        val destination = File(imagesDir, message.chatChannelId)
        val file = File(destination, name)
        val uri = Uri.fromFile(file)

        showImageViewFragment(view, uri, message.metadata?.ext!!, controllerListener, message)

    }

    private fun removeImageViewFragment() {

        isImageViewMode = false

        val transform = MaterialContainerTransform().apply {
            // Manually tell the container transform which Views to transform between.
            startView = animationStartView
            endView = animationEndView

            // Ensure the container transform only runs on a single target
            addTarget(endView)

            // Optionally add a curved path to the transform
            pathMotion = MaterialArcMotion()

            // Since View to View transforms often are not transforming into full screens,
            // remove the transition's scrim.
            scrimColor = Color.TRANSPARENT
        }

        // Begin the transition by changing properties on the start and end views or
        // removing/adding them from the hierarchy.
        TransitionManager.beginDelayedTransition(binding.root, transform)

        showSystemUI()

        binding.root.removeView(currentImageViewer)
    }

    fun showImageViewFragment(v1: View, uri: Uri, ext: String, controllerListener: FrescoImageControllerListener, message: Message? = null) {
        val v = layoutInflater.inflate(R.layout.fragment_image_view, binding.root, false)
        val imageViewBinding = FragmentImageViewBinding.bind(v)

        val screenWidth = getWindowWidth()
        binding.root.addView(imageViewBinding.root)

        val heightInPx = (screenWidth * controllerListener.finalHeight)/controllerListener.finalWidth

        val params = imageViewBinding.fullscreenImage.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = imageViewBinding.fullscreenImageContainer.id
        params.endToEnd = imageViewBinding.fullscreenImageContainer.id
        params.topToTop = imageViewBinding.fullscreenImageContainer.id
        params.bottomToBottom = imageViewBinding.fullscreenImageContainer.id
        params.height = heightInPx
        params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        // (match_parent x con.height)/con.width
        imageViewBinding.fullscreenImage.layoutParams = params

        val transform = MaterialContainerTransform().apply {
            // Manually tell the container transform which Views to transform between.
            startView = v1
            endView = imageViewBinding.fullscreenImage

            // Ensure the container transform only runs on a single target
            addTarget(endView)

            // Optionally add a curved path to the transform
            pathMotion = MaterialArcMotion()

            // Since View to View transforms often are not transforming into full screens,
            // remove the transition's scrim.
            scrimColor = Color.TRANSPARENT
        }

        currentImageViewer = imageViewBinding.root
        animationStartView = imageViewBinding.fullscreenImage
        animationEndView = v1

        // Begin the transition by changing properties on the start and end views or
        // removing/adding them from the hierarchy.

        TransitionManager.beginDelayedTransition(binding.root, transform)

        imageViewBinding.fullscreenImage.apply {
            val controller = if (ext == ".webp") {
                Fresco.newDraweeControllerBuilder()
                    .setUri(uri)
                    .setAutoPlayAnimations(true)
                    .build()
            } else {
                val imageRequest = ImageRequest.fromUri(uri)
                Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setCallerContext(this)
                    .build()
            }

            val multiGestureListener = MultiGestureListener()
//            multiGestureListener.addListener(FlingListener(this@ImageViewFragment))
            multiGestureListener.addListener(TapListener(this))
            multiGestureListener.addListener(DoubleTapGestureListener(this))
            setTapListener(multiGestureListener)

            setController(controller)
//            setOnClickListener(this@ImageViewFragment)
        }

        lifecycleScope.launch {
            delay(500)
            imageViewBinding.fullscreenImage.updateLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        }

        if (message?.metadata != null) {
            imageViewBinding.bottomInfoView.show()
            val imageViewInfo = "Sent by ${message.sender.name} • " + SimpleDateFormat("hh:mm a, dd/MM/yyyy", Locale.UK).format(message.createdAt)
            imageViewBinding.userTimeInfo.text = imageViewInfo
            imageViewBinding.imageSize.text = getTextForSizeInBytes(message.metadata!!.size)
        } else {
            imageViewBinding.bottomInfoView.hide()
        }

        imageViewBinding.fullscreenImage.setOnClickListener {
            onImageViewerClick(imageViewBinding.fullscreenImage, imageViewBinding.bottomInfoView)
        }

        imageViewBinding.fullscreenImageContainer.setOnClickListener {
            onImageViewerClick(imageViewBinding.fullscreenImage, imageViewBinding.bottomInfoView)
        }

        imageViewBinding.imageViewToolbar.setNavigationOnClickListener {
            val params1 = imageViewBinding.fullscreenImage.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = imageViewBinding.fullscreenImageContainer.id
            params.endToEnd = imageViewBinding.fullscreenImageContainer.id
            params.topToTop = imageViewBinding.fullscreenImageContainer.id
            params.bottomToBottom = imageViewBinding.fullscreenImageContainer.id
            params.height = heightInPx
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            // (match_parent x con.height)/con.width
            imageViewBinding.fullscreenImage.layoutParams = params1

            lifecycleScope.launch {
                delay(200)
                removeImageViewFragment()
            }
        }

        isImageViewMode = true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun onImageViewerClick(backgroundView: View, bottomInfoView: View) {
        val appbar = currentImageViewer?.findViewById<AppBarLayout>(R.id.image_view_appbar)

        if (appbar?.translationY == 0f) {
            appbar.slideUp(convertDpToPx(100).toFloat())
            backgroundView.setBackgroundColor(Color.BLACK)
            bottomInfoView.slideDown(convertDpToPx(150).toFloat())
            hideSystemUI()
        } else {
            appbar?.slideReset()
            if (!isNightMode()) {
                backgroundView.setBackgroundColor(ContextCompat.getColor(this, R.color.lightest_grey))
            }

            bottomInfoView.slideReset()
            showSystemUI()
        }
    }

    // make sure to change this later on for null safety
    override fun onMessageRead(message: Message) {
        val a1 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val a2 = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

        val currentUserId = UserManager.currentUserId
        if (!message.readList.contains(currentUserId)) {
            viewModel.updateReadList(a1, a2, message)
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
        toast("Double CLicked")
    }

    override fun onMessageLongPress(p0: MotionEvent?) {
        if (currentlyFocusedMessage != null) {
            if (currentlyFocusedMessage!!.state == -1) {
                currentlyFocusedMessage!!.state = 0

                viewModel.updateRestOfTheMessages(currentlyFocusedMessage!!.chatChannelId, 0)

                lifecycleScope.launch {
                    delay(300)
                    currentlyFocusedMessage!!.state = 1
                    viewModel.updateMessage(currentlyFocusedMessage!!)
                }
            }
        }
    }

    override fun onMessageClick(p0: MotionEvent?): Boolean {
        if (currentlyFocusedMessage != null && currentMessageView != null) {
            if (currentlyFocusedMessage!!.type != text && !currentlyFocusedMessage!!.isDownloaded) {
                return true
            }

            if (currentlyFocusedMessage!!.state == 0) {
                currentlyFocusedMessage!!.state = 1
                currentMessageView!!.isSelected = true
                if (isNightMode()) {
                    currentMessageView!!.setBackgroundColor(ContextCompat.getColor(this, R.color.lightest_black))
                } else {
                    currentMessageView!!.setBackgroundColor(ContextCompat.getColor(this, R.color.lightest_blue))
                }
                viewModel.updateMessage(currentlyFocusedMessage!!)
            } else {
                currentlyFocusedMessage!!.state = 0
                currentMessageView!!.isSelected = false
                currentMessageView!!.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
                viewModel.updateMessage(currentlyFocusedMessage!!)
            }
        }
        return true
    }

    override fun onMessageDoubleTapped(p0: MotionEvent?): Boolean {
        if (viewModel.selectedMessages.value.isNullOrEmpty() && viewModel.singleSelectedMessage.value == null) {
            val flag = currentlyFocusedMessage?.senderId != UserManager.currentUserId

            val popupMenu = if (!flag) {
                PopupMenu(this, currentMessageView!!, Gravity.END)
            } else {
                PopupMenu(this, currentMessageView!!, Gravity.START)
            }

            popupMenu.inflate(R.menu.chat_popup_menu)

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.popup_reply -> {
                        viewModel.setCurrentlySelectedMessage(currentlyFocusedMessage)
                    }
                    R.id.popup_details -> {
                        val bundle = bundleOf("message" to currentlyFocusedMessage)
                        navController.navigate(R.id.action_chatFragment_to_messageDetailFragment, bundle, slideRightNavOptions())
                    }
                }
                true
            }

            if (currentlyFocusedMessage?.senderId != UserManager.currentUserId) {
                popupMenu.menu.getItem(2).isVisible = false
            }

            popupMenu.show()
        }
        return true
    }

    override fun onNotificationRead(notification: Notification) {
        notification.read = true
        FireUtility.checkIfNotificationExistsById(notification.receiverId, notification.id) { exists, error ->

            if (error != null) {
                viewModel.setCurrentError(error)
                return@checkIfNotificationExistsById
            }

            if (!exists) {
                viewModel.deleteNotification(notification)
            } else {
                viewModel.updateNotification(notification)
            }
        }
    }

    override fun onNotificationClick(
        notification: Notification,
        user: User?,
        project: Project?,
        comment: Comment?
    ) {

        if (user != null) {
            onUserClick(user)
        }

        if (project != null) {
            navController.navigate(R.id.action_notificationCenterFragment_to_projectFragment, null, slideRightNavOptions())
        }

        if (comment != null) {
            if (comment.commentLevel.toInt() == 0) {
                FireUtility.getProject(comment.projectId) {
                    when (it) {
                        is Result.Error -> viewModel.setCurrentError(it.exception)
                        is Result.Success -> {
                            val project1 = it.data
                            onProjectCommentClick(project1)
                        }
                        null -> {
                            // delete notification
                        }
                    }
                }
            } else {
                onClick(comment)
            }
        }
    }

    /*private fun deleteNotification(notification: Notification) {
        FireUtility.deleteNotification(notification) { it1 ->
            if (it1.isSuccessful) {
                viewModel.deleteNotification(notification)
            } else {
                viewModel.setCurrentError(it1.exception)
            }
        }
    }*/

    override fun onNotificationUserNotFound(notification: Notification) {
        // the user is probably archived or deleted, this notification must be deleted.
        FireUtility.deleteNotification(notification) {
            viewModel.deleteNotification(notification)
        }
    }

    override suspend fun onGetMessageReply(replyTo: String): Message? {
        return viewModel.getLocalMessage(replyTo)
    }

    override fun onGetReplyMessage(
        parentMessage: Message,
        onResult: (newMessage: Message) -> Unit
    ) {
        if (parentMessage.replyTo != null) {

            val ref = Firebase.firestore.collection("chatChannels")
                .document(parentMessage.chatChannelId)
                .collection("messages")
                .document(parentMessage.replyTo!!)

            FireUtility.getDocument(ref) {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val replyMessage = it.result.toObject(Message::class.java)!!
                        lifecycleScope.launch {
                            val sender = viewModel.getLocalUser(replyMessage.senderId)
                            if (sender != null) {
                                // this step is necessary as it cannot convert a message to reply message if there is no user attached to it.
                                replyMessage.sender = sender
                                parentMessage.replyMessage = replyMessage.toReplyMessage()
                                onResult(parentMessage)
                                viewModel.updateMessage(parentMessage)
                            }
                        }
                    } else {
                        Log.e(TAG, "1097 ${it.exception?.localizedMessage}")
                    }
                } else {
                    Log.e(TAG, "1100 ${it.exception?.localizedMessage}")
                }
            }
        }
    }

    override suspend fun onGetMessageReplyUser(senderId: String): User? {
        return viewModel.getLocalUser(senderId)
    }

    override fun onMessageFocused(message: Message, parent: View) {
        currentlyFocusedMessage = message
        currentMessageView = parent
    }

    override fun onPause() {
        super.onPause()

        if (UserManager.isInitialized) {
            val currentUser = UserManager.currentUser
            val batch = Firebase.firestore.batch()

            for (channel in currentUser.chatChannels) {
                viewModel.updateRestOfTheMessages(channel, -1)

                for (token in currentUser.registrationTokens) {
                    val ref = Firebase.firestore.collection("chatChannels").document(channel)
                    batch.update(ref, "registrationTokens", FieldValue.arrayUnion(token))
                }
            }

            batch.commit().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "Successfully changed offline status to online in chat channel document.")
                } else {
                    Log.e(TAG, "1109 - ${it.exception?.localizedMessage}")
                }
            }
        }

        stopNetworkCallback()
    }

    override fun onResume() {
        super.onResume()
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null && UserManager.isInitialized) {
            val currentUser = UserManager.currentUser
            val batch = Firebase.firestore.batch()
            for (channel in currentUser.chatChannels) {
                for (token in currentUser.registrationTokens) {
                    val ref = Firebase.firestore.collection("chatChannels").document(channel)
                    batch.update(ref, "registrationTokens", FieldValue.arrayRemove(token))
                }
            }

            batch.commit().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "Successfully set the online status to offline in chat channel document")
                } else {
                    Log.e(TAG, "1142 - ${it.exception?.localizedMessage}")
                }
            }
        }

        checkForNetworkPermissions {
            if (it) {
                startNetworkCallback()
            }
        }

        lifecycleScope.launch {
            UserManager.listenForUserVerification(20, 5)
        }

        /*if (::navController.isInitialized) {
            // if the user has returned from checking their mail in the phone,
            // this will check again if the user is verified or not.
            if (navController.currentDestination?.id == R.id.emailVerificationFragment) {
                if (firebaseUser != null) {
                    val task = firebaseUser.reload()
                    task.addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (Firebase.auth.currentUser?.isEmailVerified == true) {
                                navController.navigate(R.id.action_emailVerificationFragment_to_homeFragment, null, slideRightNavOptions())
                            }
                        } else {
                            Log.d(TAG, it.exception?.localizedMessage.orEmpty())
                        }
                    }
                }
            }
        }*/
    }

    override fun onStop() {
        super.onStop()
        LocationProvider.stopLocationUpdates(fusedLocationProviderClient)
    }

    companion object {
        private const val TAG = "MainActivityDebugTag"
    }

    override fun updateProjectInvite(newProjectInvite: ProjectInvite) {
        viewModel.insertProjectInvites(newProjectInvite)
    }

    override fun onProjectInviteAccept(projectInvite: ProjectInvite) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " accepted your project invite"
        val title = projectInvite.project?.name
        val notification = Notification.createNotification(content, currentUser.id, projectInvite.senderId, userId = currentUser.id, title = title)

        FireUtility.acceptProjectInvite(currentUser, projectInvite) {
            if (it.isSuccessful) {
                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (it1.isSuccessful) {
                                        // current user was added to project successfully
                                        viewModel.addCurrentUserToProject(projectInvite)
                                        val inviteNotificationId = projectInvite.notificationId
                                        if (inviteNotificationId != null) {
                                            viewModel.deleteNotificationById(inviteNotificationId)
                                        }
                                        // extra
                                        val otherUser = projectInvite.sender
                                        if (otherUser != null) {
                                            val newList = otherUser.projectInvites.removeItemFromList(projectInvite.id)
                                            otherUser.projectInvites = newList
                                            viewModel.updateOtherUserLocally(otherUser)
                                        }
                                    } else {
                                        viewModel.setCurrentError(it1.exception)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Not possible")
                }
            } else {
                // something went wrong while trying to add the current user to the project
                viewModel.setCurrentError(it.exception)
            }
        }

    }

    override fun onProjectInviteCancel(projectInvite: ProjectInvite) {
        val currentUser = UserManager.currentUser
        val title = projectInvite.project?.name
        val content = currentUser.name + " has rejected your project invite"
        val notification = Notification.createNotification(content, currentUser.id, projectInvite.senderId, userId = currentUser.id, title = title)
        FireUtility.cancelProjectInvite(currentUser, projectInvite) {
            if (it.isSuccessful) {
                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (it1.isSuccessful) {
                                        viewModel.deleteProjectInvite(projectInvite)
                                        val inviteNotificationId = projectInvite.notificationId
                                        if (inviteNotificationId != null) {
                                            viewModel.deleteNotificationById(inviteNotificationId)
                                        }

                                        // extra
                                        val otherUser = projectInvite.sender
                                        if (otherUser != null) {
                                            val newList = otherUser.projectInvites.removeItemFromList(projectInvite.id)
                                            otherUser.projectInvites = newList
                                            viewModel.updateOtherUserLocally(otherUser)
                                        }
                                    } else {
                                        viewModel.setCurrentError(it1.exception)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Not possible")
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectInviteProjectDeleted(projectInvite: ProjectInvite) {
        // either the project was deleted or archived
        FireUtility.deleteProjectInvite(projectInvite) {
            if (it.isSuccessful) {
                viewModel.deleteProjectInvite(projectInvite)
                // extra
                val otherUser = projectInvite.sender
                if (otherUser != null) {
                    val newList = otherUser.projectInvites.removeItemFromList(projectInvite.id)
                    otherUser.projectInvites = newList
                    viewModel.updateOtherUserLocally(otherUser)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    fun selectImage1() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher1.launch(intent)
    }

}
