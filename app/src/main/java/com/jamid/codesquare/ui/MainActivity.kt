package com.jamid.codesquare.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.PlayBillingController.PremiumState.*
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.ImageSelectType.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalPagingApi
class MainActivity : LauncherActivity(), LocationItemClickListener, ProjectInviteListener,
    ProjectClickListener, ProjectRequestListener, UserClickListener, ChatChannelClickListener, NotificationItemClickListener, CommentListener, OptionClickListener {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var previouslyFetchedLocation: Location? = null
    private var currentImageViewer: View? = null
    private var animationStartView: View? = null
    private var animationEndView: View? = null
    private var updateJob: Job? = null
    private var isImageViewMode = false
    private var currentIndefiniteSnackbar: Snackbar? = null
    lateinit var networkManager: MainNetworkManager
    lateinit var playBillingController: PlayBillingController

    var subscriptionFragment: SubscriptionFragment? = null
    var optionsFragment: OptionsFragment? = null


    private val chatReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1 != null) {

                // checking if the message is sent by current user
                val senderId = p1.getStringExtra(SENDER_ID)
                if (senderId == Firebase.auth.currentUser?.uid)
                    return

                val chatChannelId = p1.getStringExtra(CHANNEL_ID)
                chatChannelId?.let {
                    FireUtility.getChatChannel(chatChannelId) {
                        val result = it ?: return@getChatChannel
                        when (result) {
                            is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                            is Result.Success -> {
                                val chatChannel = result.data

                                // check if the current destination is chat container
                                if (navController.currentDestination?.id == R.id.chatContainerSample)
                                    return@getChatChannel

                                val lastMessage = chatChannel.lastMessage
                                if (lastMessage != null) {
                                    Snackbar.make(binding.root, "${chatChannel.projectTitle}: ${lastMessage.sender.name} has sent a message", Snackbar.LENGTH_INDEFINITE).setAction("View"){
                                        onChannelClick(chatChannel)
                                    }.show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1 != null) {
                // checking if notification is sent by current user
                val senderId = p1.getStringExtra(SENDER_ID)
                if (senderId == Firebase.auth.currentUser?.uid)
                    return

                val notificationId = p1.getStringExtra(NOTIFICATION_ID)
                if (notificationId != null) {
                    val currentUserId = UserManager.currentUserId
                    FireUtility.getNotification(currentUserId, notificationId) {
                        val notificationResult = it ?: return@getNotification
                        when (notificationResult) {
                            is Result.Error -> Log.e(TAG, "onReceive: ${notificationResult.exception.localizedMessage}")
                            is Result.Success -> {
                                viewModel.insertNotifications(notificationResult.data)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectChatUploadDocuments() {
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        selectChatDocumentsUploadLauncher.launch(intent)
    }

    // needs checking
    fun onLinkClick(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    @SuppressLint("UnsafeOptInUsageError", "VisibleForTests")
    override fun onCreate() {
        super.onCreate()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        // initialize all nodes
        MobileAds.initialize(this)

        fusedLocationProviderClient = FusedLocationProviderClient(this)

        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setupNavigation()

        viewModel.currentUser.observe(this) {
            if (it != null) {
                if (previouslyFetchedLocation != null) {
                    it.location = previouslyFetchedLocation!!
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

        viewModel.allUnreadNotifications.observe(this) {
            if (!it.isNullOrEmpty()) {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    val badgeDrawable = BadgeDrawable.create(this)
                    badgeDrawable.number = it.size
                    attachBadgeDrawable(badgeDrawable, binding.mainToolbar, R.id.notifications)
                }
            }
        }



        UserManager.currentUserLive.observe(this) {
            if (it != null) {
                viewModel.currentUserBitmap = null
                viewModel.insertCurrentUser(it)

                if (it.premiumState.toInt() != -1) {
                    viewModel.deleteAdProjects()
                }

                setMessagesListener(it.chatChannels)

                viewModel.checkAndUpdateLocalProjects(it)
            }
        }

        UserManager.authState.observe(this) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                   /* if (UserManager.isEmailVerified) {
                        TODO("Do all actions that require current user to be legit")
                    }*/
                } else {
                    viewModel.isNetworkAvailable.removeObservers(this)
                }
            }
        }

        onReceiveData()

        clearActiveNotifications()


        playBillingController = PlayBillingController(this)


        lifecycle.addObserver(SnapshotListenerContainer(viewModel))
        networkManager = MainNetworkManager(this)
        lifecycle.addObserver(networkManager)
        lifecycle.addObserver(playBillingController)

        setLiveDataObservers()

        networkManager.networkAvailability.observe(this) { isNetworkAvailable ->
            if (isNetworkAvailable == true) {
                LocationProvider.initialize(fusedLocationProviderClient, this)
                currentIndefiniteSnackbar?.dismiss()
            } else {
                val (bgColor, txtColor) = if (isNightMode()) {
                    ContextCompat.getColor(this, R.color.error_color) to Color.WHITE
                } else {
                    Color.BLACK to Color.WHITE
                }

                currentIndefiniteSnackbar = Snackbar.make(binding.root, "Network not available", Snackbar.LENGTH_INDEFINITE)
                    .setBackgroundTint(bgColor)
                    .setTextColor(txtColor)

                currentIndefiniteSnackbar?.show()
            }
        }


        // prefetching some of the profile images for smoother Ui
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val isActivityOpenedFirstTime = sp.getBoolean("is_activity_opened_first_time", true)
        if (isActivityOpenedFirstTime) {
            prefetchProfileImages()

            val spEditor = sp.edit()
            spEditor.putBoolean("is_activity_opened_first_time", false)
            spEditor.apply()
        }

        setBroadcastReceivers()

    }

    private fun setBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(chatReceiver, IntentFilter("chat_receiver"))
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter("notification_receiver"))
    }

    private fun prefetchProfileImages() = lifecycleScope.launch (Dispatchers.IO) {
        val imagePipeline = Fresco.getImagePipeline()

        for (image in userImages) {
            val imageRequest = ImageRequest.fromUri(image)
            imagePipeline.prefetchToDiskCache(imageRequest, this)
        }
    }

    private fun setMessagesListener(chatChannels: List<String>) {
        for (channel in chatChannels) {
            Firebase.firestore.collection(CHAT_CHANNELS)
                .document(channel)
                .collection(MESSAGES)
                .limit(10)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, "setMessagesListener: ${error.localizedMessage}")
                    }

                    if (value != null && !value.isEmpty) {
                        val messages = value.toObjects(Message::class.java)
                        Log.d(TAG, "setMessagesListener: ${messages.map { it.content }}")
                        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                        val documentsDir =  getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

                        viewModel.insertMessages(imagesDir, documentsDir, messages)
                    }

                }
        }
    }


    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        updateUi(
            shouldShowAppBar = false,
            baseFragmentBehavior = null
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val userInfoLayout = findViewById<View>(R.id.user_info)
            invalidateOptionsMenu()

            binding.mainToolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }

            val dy = resources.getDimension(R.dimen.comment_layout_translation)
            binding.commentBottomRoot.slideDown(dy)

            binding.mainProgressBar.hide()

            if (destination.id != R.id.chatContainerSample) {
                binding.mainToolbar.findViewWithTag<SimpleDraweeView>("project_icon")?.hide()
                binding.mainToolbar.findViewWithTag<MaterialButton>("project_option")?.hide()
            }

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

            val authFragments = arrayOf(
                R.id.splashFragment1,
                R.id.onBoardingFragment,
                R.id.loginFragment,
                R.id.createAccountFragment,
                R.id.emailVerificationFragment,
                R.id.profileImageFragment,
                R.id.userInfoFragment
            )

            updateJob?.cancel()
            updateJob = when (destination.id) {
                R.id.notificationCenterFragment -> updateUi(shouldShowTabLayout = true)
                in authFragments -> updateUi(
                    shouldShowAppBar = false,
                    baseFragmentBehavior = null
                )
                R.id.homeFragment -> {
                    hideKeyboard(binding.root)
                    updateUi(
                        shouldShowTabLayout = true
                    )
                }
                R.id.profileFragment -> {
                    val actionBarHeight = resources.getDimension(R.dimen.action_bar_height)
                    userInfoLayout?.updateLayoutParams<CollapsingToolbarLayout.LayoutParams> {
                        setMargins(0, actionBarHeight.toInt(), 0, 0)
                    }
                    hideKeyboard(binding.root)
                    updateUi(
                        shouldShowTabLayout = true,
                        shouldShowUserInfo = true
                    )
                }
                R.id.chatMediaFragment -> updateUi(shouldShowTabLayout = true)
                R.id.imageViewFragment -> updateUi(baseFragmentBehavior = null)
                R.id.searchFragment -> updateUi(
                    shouldShowTabLayout = true, toolbarAdjustment = ToolbarAdjustment(
                        true,
                        R.color.normal_grey,
                        false
                    )
                )
                R.id.subscriberFragment -> updateUi(
                    shouldShowAppBar = false,
                    baseFragmentBehavior = null
                )
                else -> updateUi()
            }
        }
    }

    private fun onReceiveData() {
        val extras = intent.extras
        if (extras != null) {
            when {
                extras.containsKey(CHANNEL_ID) -> {
                    // chat notification

                    val senderId = extras[SENDER_ID] ?: return

                    if (senderId == Firebase.auth.currentUser?.uid)
                        return

                    val chatChannelId = extras[CHANNEL_ID] as String?
                    if (chatChannelId != null) {
                        FireUtility.getChatChannel(chatChannelId) {
                            val result = it ?: return@getChatChannel
                            when (result) {
                                is Result.Error -> Log.e(TAG, "onReceiveData: ${result.exception}")
                                is Result.Success -> onChannelClick(result.data)
                            }
                        }
                    }
                }
                extras.containsKey(NOTIFICATION_ID) -> {
                    if (extras.containsKey(TYPE)) {
                        val type = extras[TYPE] as String?
                        if (!type.isNullOrBlank()) {
                            if (type.isDigitsOnly()) {
                                val t = type.toInt()
                                navController.navigate(
                                    R.id.notificationCenterFragment,
                                    bundleOf(TYPE to t),
                                    slideRightNavOptions()
                                )
                            } else {
                                navController.navigate(
                                    R.id.notificationCenterFragment,
                                    bundleOf(TYPE to 0),
                                    slideRightNavOptions()
                                )
                            }
                        } else {
                            navController.navigate(
                                R.id.notificationCenterFragment,
                                bundleOf("type" to 0),
                                slideRightNavOptions()
                            )
                        }
                    }
                }
                else -> {
                    // nothing
                }
            }
        }
    }

    private fun updateUi(
        shouldShowAppBar: Boolean = true,
        shouldShowToolbar: Boolean = true,
        shouldShowTabLayout: Boolean = false,
        baseFragmentBehavior: CoordinatorLayout.Behavior<View>? = AppBarLayout.ScrollingViewBehavior(),
        toolbarAdjustment: ToolbarAdjustment = ToolbarAdjustment(),
        shouldShowUserInfo: Boolean = false
    ) = lifecycleScope.launch {
        delay(300)
        // need to wait for the fragment itself to be visible

        binding.mainAppbar.isVisible = shouldShowAppBar
        binding.mainToolbar.isVisible = shouldShowToolbar
        binding.mainTabLayout.isVisible = shouldShowTabLayout

        binding.mainTabLayout.isVisible = shouldShowTabLayout

        if (shouldShowAppBar) {
            if (binding.mainAppbar.translationY != 0f) {
                binding.mainAppbar.slideReset()
            }
            if (shouldShowToolbar) {
                binding.mainToolbar.apply {
                    isTitleCentered = toolbarAdjustment.isTitleCentered

                    if (isNightMode()) {
                        if (toolbarAdjustment.titleTextColor == R.color.black) {
                            setTitleTextColor(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.white
                                )
                            )
                        } else {
                            setTitleTextColor(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    toolbarAdjustment.titleTextColor
                                )
                            )
                        }
                    } else {
                        setTitleTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                toolbarAdjustment.titleTextColor
                            )
                        )
                    }

                    if (!toolbarAdjustment.shouldShowTitle) {
                        title = " "
                    }
                }
            }
        } else {
            binding.mainToolbar.hide()
        }

        val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = baseFragmentBehavior
        binding.navHostFragment.layoutParams = params

        val userInfoLayout = findViewById<View>(R.id.user_info)
        userInfoLayout?.isVisible = shouldShowUserInfo

    }


    private fun clearActiveNotifications() {
        NotificationManagerCompat.from(this).cancelAll()
        // TODO("Not actually working, need to do a whole lot of changes for this")
    }

    override fun onLocationClick(place: Place) {
        val latLang = place.latLng
        if (latLang != null) {
            val formattedAddress = place.address.orEmpty()
            val hash =
                GeoFireUtils.getGeoHashForLocation(GeoLocation(latLang.latitude, latLang.longitude))
            viewModel.setCurrentProjectLocation(
                Location(
                    latLang.latitude,
                    latLang.longitude,
                    formattedAddress,
                    hash
                )
            )

            navController.navigateUp()
        }
    }

    override fun onProjectClick(project: Project) {
        val bundle = bundleOf(TITLE to project.name, PROJECT to project, "image_pos" to 0)
        navController.navigate(R.id.projectFragmentContainer, bundle, slideRightNavOptions())
    }

    override fun onProjectClick(projectMinimal2: ProjectMinimal2) {
        getProjectImpulsive(projectMinimal2.objectID) {
            onProjectClick(it)
        }
    }

    override fun onProjectLikeClick(project: Project, onChange: (newProject: Project) -> Unit) {

        val currentUser = UserManager.currentUser

        fun onLike() {
            project.likes = project.likes + 1
            project.isLiked = true
            viewModel.updateLocalProject(project)

            onChange(project)
        }

        fun onDislike() {
            project.likes = project.likes - 1
            project.isLiked = false
            viewModel.updateLocalProject(project)

            onChange(project)
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
            val notification = Notification.createNotification(
                content,
                project.creator.userId,
                projectId = project.id,
                title = title
            )
            FireUtility.likeProject(project) {
                if (it.isSuccessful) {
                    // check if notification already exists
                    onLike()
                    sendNotificationImpulsive(notification)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onProjectSaveClick(project: Project, onChange: (newProject: Project) -> Unit) {
        if (project.isSaved) {
            // un-save
            FireUtility.unSaveProject(project) {
                if (it.isSuccessful) {
                    project.isSaved = false
                    viewModel.updateLocalProject(project)

                    onChange(project)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {
            // save
            FireUtility.saveProject(project) {
                if (it.isSuccessful) {
                    Snackbar.make(binding.root, "Saved this project", Snackbar.LENGTH_LONG).show()

                    project.isSaved = true
                    viewModel.updateLocalProject(project)

                    onChange(project)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    private fun setLiveDataObservers() {
        playBillingController.isPurchaseAcknowledged.observe(this) { isPurchaseAcknowledged ->
            if (isPurchaseAcknowledged == true) {
                subscriptionFragment?.dismiss()
                navController.navigate(R.id.subscriberFragment, null, slideRightNavOptions())
            }
        }

        playBillingController.premiumState.observe(this) {
            val premiumState = it ?: return@observe

            when (premiumState) {
                STATE_NO_PURCHASE -> {
                    subscriptionFragment?.dismiss()
                }
                STATE_HALF_PURCHASE -> {
                    //
                }
                STATE_FULL_PURCHASE -> {
                    //
                }
            }
        }

        playBillingController.purchaseDetails.observe(this) {p1 ->
            if (!p1.isNullOrEmpty()) {
                viewModel.setSubscriptionDetailsList(p1)
            }
        }

    }

    override fun onProjectJoinClick(project: Project, onChange: (newProject: Project) -> Unit) {
        if (project.isRequested) {
            // undo request
            onProjectUndoClick(project, onChange)
        } else {
            // send request
            val currentUser = UserManager.currentUser
            val content = currentUser.name + " wants to join your project"

            val notification = Notification.createNotification(
                content,
                project.creator.userId,
                type = 1,
                userId = currentUser.id,
                title = project.name
            )

            fun onRequestSent(projectRequest: ProjectRequest) {
                val requestsList = project.requests.addItemToList(projectRequest.requestId)
                project.requests = requestsList
                project.isRequested = true

                viewModel.updateLocalProject(project)
                viewModel.insertProjectRequests(projectRequest)

                onChange(project)
            }

            FireUtility.joinProject(notification.id, project) { task, projectRequest ->
                if (task.isSuccessful) {

                    Snackbar.make(binding.root, "Project request sent", Snackbar.LENGTH_LONG).show()

                    onRequestSent(projectRequest)

                    sendNotificationImpulsive(notification)
                } else {
                    viewModel.setCurrentError(task.exception)
                }
            }
        }


    }

    private fun updateLocalProjectOnRequestAccept(project: Project, projectRequest: ProjectRequest) {
        // removing the request id from project->requests list
        val newRequestsList = project.requests.removeItemFromList(projectRequest.requestId)
        project.requests = newRequestsList

        // adding the request sender to the project->contributors list
        val newContList = project.contributors.addItemToList(projectRequest.senderId)
        project.contributors = newContList

        // recording the time of update locally
        project.updatedAt = System.currentTimeMillis()

        val processedProject = processProjects(arrayOf(project)).first()

        viewModel.updateLocalProject(processedProject)
    }

    override fun onProjectRequestAccept(projectRequest: ProjectRequest) {
        val currentUser = UserManager.currentUser

        getProjectImpulsive(projectRequest.projectId) { project ->
            if (project.contributors.size < 5 || currentUser.premiumState.toInt() == 1) {

                FireUtility.acceptProjectRequest(project, projectRequest) { it1 ->
                    if (it1.isSuccessful) {
                        // 1. update the local project
                        updateLocalProjectOnRequestAccept(project, projectRequest)

                        // 2. insert the new user to local database
                        getNewContributorOnRequestAccept(projectRequest.senderId)

                        // 3. delete the project request
                        viewModel.deleteProjectRequest(projectRequest)

                        // 4. send notification
                        val title = project.name
                        val content = currentUser.name + " has accepted your project request"
                        val notification = Notification.createNotification(
                            content,
                            projectRequest.senderId,
                            projectId = project.id,
                            title = title
                        )

                        sendNotificationImpulsive(notification)

                    } else {
                        Log.e(
                            TAG,
                            "onProjectRequestAccept: ${it1.exception?.localizedMessage}"
                        )
                    }
                }
            } else {
                val upgradeMsg = getString(R.string.upgrade_plan_imsg)
                val frag = MessageDialogFragment.builder(upgradeMsg)
                    .setPositiveButton(getString(R.string.upgrade)) { _, _ ->
                        showSubscriptionFragment()
                    }
                    .setNegativeButton(getString(R.string.cancel)){ a, _ ->
                        a.dismiss()
                    }.build()

                frag.show(supportFragmentManager, MessageDialogFragment.TAG)
            }
        }


    }

    private fun getNewContributorOnRequestAccept(userId: String) {
        getUser(userId) {}
    }

    // project request must have project included inside it
    override fun onProjectRequestCancel(projectRequest: ProjectRequest) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " rejected your project request"
        val title = projectRequest.project.name

        val notification = Notification.createNotification(
            content,
            projectRequest.senderId,
            userId = currentUser.id,
            title = title
        )

        FireUtility.rejectRequest(projectRequest) {
            if (!it.isSuccessful) {
                viewModel.setCurrentError(it.exception)
            } else {
                viewModel.deleteLocalProjectRequest(projectRequest)
                viewModel.deleteNotificationById(projectRequest.notificationId)

                sendNotificationImpulsive(notification)
            }
        }
    }

    override fun onProjectLoad(project: Project) {
        if (project.expiredAt > System.currentTimeMillis()) {
            // delete project
            FireUtility.deleteProject(project) {
                if (it.isSuccessful) {
                    // delete requests and let the users know
                    FireUtility.getAllRequestsForProject(project) { result ->
                        when (result) {
                            is Result.Error -> viewModel.setCurrentError(result.exception)
                            is Result.Success -> {
                                val requests = result.data
                                FireUtility.postDeleteProject(requests) { it1 ->
                                    if (!it1.isSuccessful) {
                                        viewModel.setCurrentError(it1.exception)
                                    }
                                }
                            }
                            null -> {
                                Log.d(TAG, "Maybe there's none, but it\'s okay")
                            }
                        }
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    fun showSubscriptionFragment() {
        subscriptionFragment = SubscriptionFragment()
        subscriptionFragment?.show(supportFragmentManager, "SubscriptionFragment")
    }

    override fun onAdInfoClick() {

        val frag = MessageDialogFragment.builder("Argh \uD83D\uDE24! These ads are annoying. ❌ Remove ads from the app ? \uD83D\uDE04")
            .setPositiveButton("Remove ads") { _, _ ->
                showSubscriptionFragment()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .build()

        frag.show(supportFragmentManager, MessageDialogFragment.TAG)

    }

    override fun onAdError(project: Project) {
        viewModel.deleteLocalProject(project)
    }

    override fun onProjectLocationClick(project: Project) {
        navController.navigate(R.id.locationProjectsFragment, bundleOf(TITLE to "Showing projects near", SUB_TITLE to project.location.address, "location" to project.location), slideRightNavOptions())
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(project: Project) {

        fun onChangeNeeded(creator: User) {
            val changes = mapOf("creator" to creator.minify(), "updatedAt" to System.currentTimeMillis())

            FireUtility.updateProject(project.id, changes) { it1 ->
                if (it1.isSuccessful) {
                    project.creator = creator.minify()
                    viewModel.insertProjectToCache(project)
                    viewModel.updateLocalProject(project)
                } else {
                    Log.e(
                        ProjectFragment.TAG,
                        "setCreatorRelatedUi: ${it1.exception?.localizedMessage}"
                    )
                }
            }
        }

        if (UserManager.currentUserId == project.creator.userId) {
            if (UserManager.currentUser.minify() != project.creator) {
                onChangeNeeded(UserManager.currentUser)
            }
        } else {
            getUserImpulsive(project.creator.userId) { creator ->
                if (creator.minify() != project.creator) {
                    onChangeNeeded(creator)
                }
            }
        }
    }

    fun getProjectImpulsive(projectId: String, onProjectFetch: (Project) -> Unit) {
        val cachedProject = viewModel.getCachedProject(projectId)
        if (cachedProject == null) {
            viewModel.getProject(projectId) { localProject ->
                runOnUiThread {
                    if (localProject != null) {
                        viewModel.insertProjectToCache(localProject)
                        onProjectFetch(localProject)
                    } else {
                        getProject(projectId) { project ->
                            onProjectFetch(project)
                        }
                    }
                }
            }
        } else {
            onProjectFetch(cachedProject)
        }
    }

    fun getUserImpulsive(userId: String, onUserFound: (User) -> Unit) {
        val cachedUser = viewModel.getCachedUser(userId)
        if (cachedUser == null) {
            viewModel.getUser(userId) { localUser ->
                runOnUiThread {
                    if (localUser != null) {
                        viewModel.insertUserToCache(localUser)
                        onUserFound(localUser)
                    } else {
                        getUser(userId) { user ->
                            onUserFound(user)
                        }
                    }
                }
            }
        } else {
            onUserFound(cachedUser)
        }
    }

    fun getProject(projectId: String, onFetch: (Project) -> Unit) {
        FireUtility.getProject(projectId) child@ {
            val projectResult = it ?: return@child
            when (projectResult) {
                is Result.Error -> Log.e(
                    TAG,
                    "getProject: Error while trying to get project with projectId: $projectId"
                )
                is Result.Success -> {
                    val formattedProject = processProjects(arrayOf(projectResult.data))[0]
                    viewModel.insertProjectToCache(formattedProject)
                    viewModel.insertProjects(formattedProject)
                    onFetch(formattedProject)
                }
            }
        }
    }

    fun getUser(userId: String, onFetch: (User) -> Unit) {
        FireUtility.getUser(userId) child@ {
            val userResult = it ?: return@child
            when (userResult) {
                is Result.Error -> Log.e(
                    TAG,
                    "getUser: Error while trying to get user with userId = $userId"
                )
                is Result.Success -> {
                    val formattedUser = processUsers(userResult.data)[0]
                    viewModel.insertUsers(formattedUser)
                    viewModel.insertUserToCache(formattedUser)
                    onFetch(formattedUser)
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

    override fun onProjectRequestUndo(projectRequest: ProjectRequest) {
        FireUtility.undoJoinProject(projectRequest) {
            if (it.isSuccessful) {
                viewModel.deleteProjectRequest(projectRequest)
                getProjectImpulsive(projectRequest.projectId) { project ->
                    val requestsList = project.requests.removeItemFromList(projectRequest.requestId)
                    project.requests = requestsList
                    project.isRequested = false
                    viewModel.insertProjects(project)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectRequestClick(projectRequest: ProjectRequest) {
        getProjectImpulsive(projectRequest.projectId) { project ->
            onProjectClick(project)
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(projectRequest: ProjectRequest) {

        fun updateProjectRequest(changes: Map<String, Any?>) {
            FireUtility.updateProjectRequest(projectRequest.requestId, changes) { projectRequestUpdateResult ->
                if (projectRequestUpdateResult.isSuccessful) {
                    viewModel.insertProjectRequests(projectRequest)
                }
            }
        }

        fun onChangeNeeded(project: Project) {
            val changes = mapOf("project" to project.minify(), "updatedAt" to System.currentTimeMillis())
            projectRequest.project = project.minify()
            projectRequest.updatedAt = System.currentTimeMillis()
            updateProjectRequest(changes)
        }

        fun onChangeNeeded(user: User) {
            val changes = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            projectRequest.sender = user.minify()
            projectRequest.updatedAt = System.currentTimeMillis()
            updateProjectRequest(changes)
        }

        getProjectImpulsive(projectRequest.projectId) { project ->
            if (project.updatedAt > projectRequest.updatedAt) {
                onChangeNeeded(project)
            }
        }

        getUserImpulsive(projectRequest.senderId) { user ->
            if (user.minify() != projectRequest.sender) {
                onChangeNeeded(user)
            }
        }

    }

    override fun onProjectCreatorClick(project: Project) {
        if (project.creator.userId == UserManager.currentUserId) {
            when (navController.currentDestination?.id) {
                R.id.homeFragment -> {
                    navController.navigate(
                        R.id.action_homeFragment_to_profileFragment,
                        null,
                        slideRightNavOptions()
                    )
                }
                R.id.profileFragment -> {
                    navController.navigate(
                        R.id.action_profileFragment_self,
                        null,
                        slideRightNavOptions()
                    )
                }
                R.id.savedProjectsFragment -> {
                    navController.navigate(
                        R.id.action_savedProjectsFragment_to_profileFragment,
                        null,
                        slideRightNavOptions()
                    )
                }
                R.id.archiveFragment -> {
                    navController.navigate(
                        R.id.action_archiveFragment_to_profileFragment,
                        null,
                        slideRightNavOptions()
                    )
                }
                R.id.searchFragment -> {
                    navController.navigate(
                        R.id.action_searchFragment_to_profileFragment,
                        null,
                        slideRightNavOptions()
                    )
                }
            }

        } else {
            viewModel.getOtherUser(project.creator.userId) {
                if (it.isSuccessful && it.result.exists()) {
                    val user = it.result.toObject(User::class.java)!!
                    viewModel.insertUsers(user)
                    onUserClick(user)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }


    override fun onProjectCommentClick(project: Project) {
        val bundle = bundleOf(
            COMMENT_CHANNEL_ID to project.commentChannel,
            PARENT to project,
            TITLE to project.name
        )

        navController.navigate(R.id.commentsFragment, bundle, slideRightNavOptions())
        /*val frag = CommentsFragment2()
        frag.show(supportFragmentManager, "CommentsFrag")*/

    }

    override fun onProjectOptionClick(project: Project) {
        val currentUser = UserManager.currentUser
        val creatorId = project.creator.userId
        val isCurrentUser = creatorId == currentUser.id
        val name = project.creator.name

        val option1 = "Like $name"
        val option2 = "Dislike $name"
        val option3 = "Save project"
        val option4 = "Remove from saved"
        val option5 = "Archive project"
        val option6 = "Report"
        val option7 = "Unarchive project"
        val option8 = "Edit project"

        val isCreatorLiked = currentUser.likedUsers.contains(creatorId)
        val likeDislikeUserText = if (isCreatorLiked) {
            option2
        } else {
            option1
        }
        val saveUnSaveText = if (project.isSaved) {
            option4
        } else {
            option3
        }
        val archiveToggleText = if (project.isArchived) {
            option7
        } else {
            option5
        }
        val (choices, icons) = if (isCurrentUser) {
            arrayListOf(saveUnSaveText, archiveToggleText, option8) to arrayListOf(if (project.isSaved) {R.drawable.ic_remove_bookmark} else {R.drawable.ic_add_bookmark}, R.drawable.ic_round_archive_24, R.drawable.ic_edit)
        } else {
            arrayListOf(likeDislikeUserText, saveUnSaveText, option6) to arrayListOf(if (isCreatorLiked) {R.drawable.ic_dislike_user} else {R.drawable.ic_like_user}, if (project.isSaved) {R.drawable.ic_remove_bookmark} else {R.drawable.ic_add_bookmark}, R.drawable.ic_report)
        }

        optionsFragment = OptionsFragment.newInstance(project.name, choices, icons, project = project, user = project.creator.toUser())
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    override fun onProjectOptionClick(projectMinimal2: ProjectMinimal2) {
        getProjectImpulsive(projectMinimal2.objectID) {
            onProjectOptionClick(it)
        }
    }

    override fun onProjectUndoClick(project: Project, onChange: (newProject: Project) -> Unit) {
        FireUtility.getProjectRequest(project.id, UserManager.currentUserId) {
            val projectRequestResult = it ?: return@getProjectRequest

            when (projectRequestResult) {
                is Result.Error -> Log.e(TAG, "onProjectUndoClick: ${projectRequestResult.exception.localizedMessage}")
                is Result.Success -> {
                    val projectRequest = projectRequestResult.data
                    FireUtility.undoJoinProject(projectRequest) { it1 ->
                        if (it1.isSuccessful) {
                            project.isRequested = false
                            val newList = project.requests.removeItemFromList(projectRequest.requestId)
                            project.requests = newList
                            viewModel.updateLocalProject(project)
                            onChange(project)
                            viewModel.deleteProjectRequest(projectRequest)
                        } else {
                            viewModel.setCurrentError(it1.exception)
                        }
                    }
                }
            }
        }


    }

    override fun onProjectContributorsClick(project: Project) {

        val bundle = bundleOf(
            PROJECT to project
        )

        navController.navigate(
            R.id.projectContributorsFragment,
            bundle,
            slideRightNavOptions()
        )
    }

    override fun onProjectSupportersClick(project: Project) {
        val bundle = bundleOf(PROJECT_ID to project.id)
        navController.navigate(R.id.projectLikesFragment, bundle, slideRightNavOptions())
    }

    override fun onProjectNotFound(project: Project) {

    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }
        navController.navigate(R.id.profileFragment, bundle, slideRightNavOptions())
    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        getUserImpulsive(userMinimal.objectID) {
            onUserClick(it)
        }
    }

    override fun onUserOptionClick(user: User) {

    }

    override fun onUserOptionClick(userMinimal: UserMinimal2) {
        getUserImpulsive(userMinimal.objectID) {
            onUserOptionClick(it)
        }
    }

    private fun dislikeUser(userId: String) {
        FireUtility.dislikeUser(userId) {
            if (it.isSuccessful) {
                viewModel.dislikeLocalUserById(userId)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun likeUser(userId: String) {
        val currentUser = UserManager.currentUser
        FireUtility.likeUser(userId) {
            if (it.isSuccessful) {

                viewModel.likeLocalUserById(userId)

                val content = currentUser.name + " liked your profile"
                val notification = Notification.createNotification(
                    content,
                    userId,
                    userId = userId,
                    title = currentUser.name
                )

                sendNotificationImpulsive(notification)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onUserLikeClick(user: User) {
        if (user.isLiked) {
            dislikeUser(user.id)
        } else {
            likeUser(user.id)
        }
    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {
        getUserImpulsive(userMinimal.objectID) {
            onUserLikeClick(it)
        }
    }

    override fun onCommentLikeClicked(comment: Comment) {
        val currentUser = UserManager.currentUser
        if (!comment.isLiked) {
            FireUtility.likeComment(comment) {
                val content = currentUser.name + " liked your comment"
                val notification = Notification.createNotification(
                    content,
                    comment.senderId,
                    userId = currentUser.id,
                    title = currentUser.name
                )
                FireUtility.sendNotification(notification) {
                    if (it.isSuccessful) {
                        val newCommentLikesList = comment.likes.addItemToList(currentUser.id)
                        comment.likesCount = comment.likesCount + 1
                        comment.isLiked = true
                        comment.likes = newCommentLikesList
                        viewModel.updateComment(comment)
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            }
        } else {
            FireUtility.dislikeComment(comment) {
                if (it.isSuccessful) {
                    val newCommentLikesList = comment.likes.removeItemFromList(currentUser.id)

                    comment.likesCount = comment.likesCount - 1
                    comment.isLiked = false
                    comment.likes = newCommentLikesList
                    viewModel.updateComment(comment)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onCommentReply(comment: Comment) {
        viewModel.replyToContent.postValue(comment)
    }

    override fun onClick(comment: Comment) {
        val bundle = bundleOf(
            PARENT to comment,
            TITLE to COMMENTS,
            COMMENT_CHANNEL_ID to comment.threadChannelId
        )
        when (navController.currentDestination?.id) {
            R.id.commentsFragment -> {
                navController.navigate(
                    R.id.action_commentsFragment_self,
                    bundle,
                    slideRightNavOptions()
                )
            }
            R.id.notificationCenterFragment -> {
                navController.navigate(
                    R.id.action_notificationCenterFragment_to_commentsFragment,
                    bundle,
                    slideRightNavOptions()
                )
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

        val report = Report.getReportForComment(comment)
        val bundle = bundleOf(REPORT to report)

        navController.navigate(
            R.id.reportFragment, bundle, slideRightNavOptions()
        )
    }

    override fun onCommentUserClick(userMinimal: UserMinimal) {
        getUserImpulsive(userMinimal.userId) {
            onUserClick(it)
        }
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

        optionsFragment = OptionsFragment.newInstance(title = "Comment by $name", options = choices, icons = icons, comment = comment)
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        val bundle = bundleOf(
            CHAT_CHANNEL to chatChannel,
            TITLE to chatChannel.projectTitle
        )

        navController.navigate(
            R.id.action_homeFragment_to_chatContainerSample,
            bundle,
            slideRightNavOptions()
        )
    }

    override fun onChatChannelSelected(chatChannel: ChatChannel) {

    }

    override fun onChatChannelDeSelected(chatChannel: ChatChannel) {

    }

    override fun onBackPressed() {
        if (isImageViewMode) {
            removeImageViewFragment()
        } else {
            super.onBackPressed()
        }
    }

    fun hideSystemUI() {
        val view = currentImageViewer?.findViewById<View>(R.id.image_view_appbar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (view != null) {
            val controller = ViewCompat.getWindowInsetsController(view)
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = ViewCompat.getWindowInsetsController(binding.root)
        controller?.show(WindowInsetsCompat.Type.systemBars())
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

    fun showImageViewFragment(
        v1: View,
        image: Image,
        message: Message? = null
    ) {

        if (isImageViewMode)
            return

        val v = layoutInflater.inflate(R.layout.fragment_image_view, binding.root, false)
        val imageViewBinding = FragmentImageViewBinding.bind(v)

        val screenWidth = getWindowWidth()
        binding.root.addView(imageViewBinding.root)

        val heightInPx =
            (screenWidth * image.height) / image.width

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
            val controller = if (image.extra == ".webp") {
                Fresco.newDraweeControllerBuilder()
                    .setUri(image.url)
                    .setAutoPlayAnimations(true)
                    .build()
            } else {
                val imageRequest = ImageRequest.fromUri(image.url)
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
            imageViewBinding.fullscreenImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = ConstraintLayout.LayoutParams.MATCH_PARENT
                height = ConstraintLayout.LayoutParams.MATCH_PARENT
            }
        }

        if (message?.metadata != null) {
            imageViewBinding.bottomInfoView.show()
            val imageViewInfo = "Sent by ${message.sender.name} • " + SimpleDateFormat(
                "hh:mm a, dd/MM/yyyy",
                Locale.UK
            ).format(message.createdAt)
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
            val params1 =
                imageViewBinding.fullscreenImage.layoutParams as ConstraintLayout.LayoutParams
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
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            appbar.slideUp(dy)
            backgroundView.setBackgroundColor(Color.BLACK)

            val dy1 = resources.getDimension(R.dimen.image_info_translation)
            bottomInfoView.slideDown(dy1)

            hideSystemUI()
        } else {
            appbar?.slideReset()
            if (!isNightMode()) {
                backgroundView.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.lightest_grey
                    )
                )
            }

            bottomInfoView.slideReset()
            showSystemUI()
        }
    }

    override fun onNotificationRead(notification: Notification) {
        FireUtility.checkIfNotificationExistsById(
            notification.receiverId,
            notification.id
        ) { exists, error ->

            if (error != null) {
                viewModel.setCurrentError(error)
                return@checkIfNotificationExistsById
            }

            if (!exists) {
                viewModel.deleteNotification(notification)
            } else {
                if (!notification.read) {
                    FireUtility.updateNotification(notification) {
                        if (it.isSuccessful) {
                            notification.read = true
                            viewModel.updateNotification(notification)
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
        }
    }

    override fun onNotificationClick(notification: Notification) {
        val userId = notification.userId
        if (userId != null) {
            getUserImpulsive(userId) {
                onUserClick(it)
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have user Id")
        }

        val projectId = notification.projectId
        if (projectId != null) {
            getProjectImpulsive(projectId) { project ->
                onProjectClick(project)
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have project Id")
        }

        val commentId = notification.commentId
        if (commentId != null) {
            FireUtility.getComment(commentId) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        val comment = it.data
                        if (comment.commentLevel.toInt() == 0) {

                            getProjectImpulsive(comment.projectId) { project ->
                                onProjectCommentClick(project)
                            }

                        } else {
                            onClick(comment)
                        }
                    }
                    null -> {
                        Log.w(TAG, "Something went wrong while trying to get comment with comment id: $commentId")
                    }
                }
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have comment Id")
        }
    }

    override fun onNotificationError(notification: Notification) {
        // delete the notification if there are any discrepancies
        FireUtility.deleteNotification(notification) {
            viewModel.deleteNotification(notification)
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(notification: Notification) {

        fun onChangeNeeded(user: User) {

            viewModel.insertUserToCache(user)

            val changes = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            notification.sender = user.minify()
            notification.updatedAt = System.currentTimeMillis()

            FireUtility.updateNotification(notification.receiverId, notification.id, changes) {
                if (it.isSuccessful) {
                    viewModel.insertNotifications(notification)
                } else {
                    Log.e(TAG, "onChangeNeeded: ${it.exception?.localizedMessage}")
                }
            }

        }

        getUserImpulsive(notification.senderId) { user ->
            if (user.minify() != notification.sender) {
                onChangeNeeded(user)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        viewModel.setListenerForEmailVerification()
    }

    override fun onStop() {
        super.onStop()
        LocationProvider.stopLocationUpdates(fusedLocationProviderClient)

        FireUtility.updateUser2(mapOf("online" to false)) {
            if (it.isCanceled) {
                Log.e(TAG, "onStop: ${it.exception?.localizedMessage}")
            }
        }

    }

    companion object {
        private const val TAG = "MyMainActivity"
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onProjectInviteAccept(projectInvite: ProjectInvite) {

        val currentUser = UserManager.currentUser
        val content = currentUser.name + " accepted your project invite"
        val title = projectInvite.project.name
        val notification = Notification.createNotification(
            content,
            projectInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        fun onPostAccept(sender: User) {

            // local changes
            val newList =
                sender.projectInvites.removeItemFromList(
                    projectInvite.id
                )
            sender.projectInvites = newList

            sendNotificationImpulsive(notification)

        }

        Firebase.firestore.collection(PROJECTS)
            .document(projectInvite.projectId)
            .get()
            .addOnSuccessListener { it1 ->
                if (it1.exists()) {
                    FireUtility.acceptProjectInvite(currentUser, projectInvite) {
                        if (it.isSuccessful) {

                            // current user was added to project successfully
                            viewModel.deleteProjectInvite(projectInvite)
                            viewModel.deleteNotificationById(projectInvite.notificationId)

                            // extra
                            getUserImpulsive(projectInvite.senderId) { user ->
                                onPostAccept(user)
                            }
                        } else {
                            // something went wrong while trying to add the current user to the project
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                } else {
                    onProjectInviteProjectDeleted(projectInvite)
                }
            }.addOnFailureListener {
                Log.e(TAG, "onProjectInviteAccept: ${it.localizedMessage}")
            }
    }

    private fun sendNotificationImpulsive(notification: Notification) {
        if (notification.senderId != notification.receiverId) {
            FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                if (error != null) {
                    viewModel.setCurrentError(error)
                } else {
                    if (!exists) {
                        FireUtility.sendNotification(notification) { it1 ->
                            if (!it1.isSuccessful) {
                                viewModel.setCurrentError(it1.exception)
                            }
                        }
                    } else {
                        FireUtility.updateNotification(notification.receiverId, notification.id, mapOf("read" to false, UPDATED_AT to System.currentTimeMillis())) { it1 ->
                            if (!it1.isSuccessful) {
                                Log.e(TAG, "onProjectRequestAccept: ${it1.exception?.localizedMessage}")
                            }
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Not possible")
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onProjectInviteCancel(projectInvite: ProjectInvite) {

        val currentUser = UserManager.currentUser
        val title = projectInvite.project.name
        val content = currentUser.name + " has rejected your project invite"

        val notification = Notification.createNotification(
            content,
            projectInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        fun onPostCancel(sender: User) {

            // local changes
            val newList =
                sender.projectInvites.removeItemFromList(
                    projectInvite.id
                )
            sender.projectInvites = newList

            sendNotificationImpulsive(notification)
        }

        FireUtility.cancelProjectInvite(projectInvite) {
            if (it.isSuccessful) {
                // current user was added to project successfully
                viewModel.deleteProjectInvite(projectInvite)
                viewModel.deleteNotificationById(projectInvite.notificationId)

                getUserImpulsive(projectInvite.senderId) { user ->
                    onPostCancel(user)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onProjectInviteProjectDeleted(projectInvite: ProjectInvite) {

        fun onPostDelete(sender: User) {
            // local changes
            val newList =
                sender.projectInvites.removeItemFromList(
                    projectInvite.id
                )
            sender.projectInvites = newList
        }

        // either the project was deleted or archived
        FireUtility.deleteProjectInvite(projectInvite) {
            if (it.isSuccessful) {
                viewModel.deleteProjectInvite(projectInvite)

                getUserImpulsive(projectInvite.senderId) { user ->
                    onPostDelete(user)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(projectInvite: ProjectInvite) {

        fun updateProjectInvite(changes: Map<String, Any?>) {
            FireUtility.updateProjectInvite(projectInvite.receiverId, projectInvite.id, changes) { projectInviteUpdateResult ->
                if (projectInviteUpdateResult.isSuccessful) {
                    viewModel.insertProjectInvites(projectInvite)
                }
            }
        }

        fun onChangeNeeded(project: Project) {
            val changes = mapOf("project" to project.minify(), "updatedAt" to System.currentTimeMillis())
            projectInvite.project = project.minify()
            projectInvite.updatedAt = System.currentTimeMillis()
            updateProjectInvite(changes)
        }

        fun onChangeNeeded(user: User) {
            val changes = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            projectInvite.sender = user.minify()
            projectInvite.updatedAt = System.currentTimeMillis()
            updateProjectInvite(changes)
        }

        getProjectImpulsive(projectInvite.projectId) { project ->
            if (project.updatedAt > projectInvite.updatedAt) {
                onChangeNeeded(project)
            }
        }

        getUserImpulsive(projectInvite.senderId) { user ->
            if (user.minify() != projectInvite.sender) {
                onChangeNeeded(user)
            }
        }

    }


    fun selectImage(type: ImageSelectType) {
        imageSelectType = type

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        when (type) {
            IMAGE_PROFILE -> {
                sil.launch(intent)
            }
            IMAGE_CHAT, IMAGE_PROJECT, IMAGE_REPORT -> {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                sil.launch(intent)
            }
        }
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        project: Project?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?
    ) {

        optionsFragment?.dismiss()
        optionsFragment = null

        val option3 = OPTION_3 + user?.name
        val option4 = OPTION_4 + user?.name
        val option5 = OPTION_5 + user?.name

        when (option.item) {
            OPTION_1 -> {
                if (chatChannel != null && user != null) {
                    viewModel.setOtherUserAsAdmin(chatChannel.chatChannelId, user.id) {
                        if (!it.isSuccessful) {
                            viewModel.setCurrentError(it.exception)
                        } else {
                            Snackbar.make(binding.root,
                                "Successfully set ${user.name} as admin", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } else {
                    toast("either focused chat channel or focused user is null")
                }
            }
            OPTION_2 -> {
                if (chatChannel != null && user != null) {
                    viewModel.removeOtherUserFromAdmin(chatChannel.chatChannelId, user.id) {
                        if (!it.isSuccessful) {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            option3 -> {
                if (user != null) {
                    likeUser(user.id)
                    Snackbar.make(binding.root, "Liked ${user.name}", Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            option4 -> {
                if (user != null) {
                    dislikeUser(user.id)
                }
            }
            option5 -> {
                if (user != null) {
                    val report = Report.getReportForUser(user)
                    navController.navigate(
                        R.id.action_chatDetailFragment_to_reportFragment,
                        bundleOf("report" to report),
                        slideRightNavOptions()
                    )

                }

            }
            OPTION_6 -> {
                if (chatChannel != null && user != null) {

                    val frag = MessageDialogFragment.builder("Are you sure you want to remove ${user.name} from the project?")
                        .setTitle("Removing user from project")
                        .setPositiveButton("Remove") { _, _ ->
                            removeUserFromProject(user, chatChannel)
                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }.build()

                    frag.show(supportFragmentManager, MessageDialogFragment.TAG)
                }
            }
            OPTION_7 -> {
                if (chatChannel != null && user != null) {
                    FireUtility.removeUserFromProject(
                        user,
                        chatChannel.projectId,
                        chatChannel.chatChannelId
                    ) {
                        if (it.isSuccessful) {

                            navController.popBackStack(R.id.homeFragment, false)

                            viewModel.removeProjectFromUserLocally(
                                chatChannel.chatChannelId,
                                chatChannel.projectId,
                                user
                            )


                            getProjectImpulsive(chatChannel.projectId) { it1 ->
                                val contributors =
                                    it1.contributors.removeItemFromList(
                                        UserManager.currentUserId
                                    )
                                it1.contributors =
                                    contributors
                                viewModel.updateLocalProject(
                                    it1
                                )
                            }

                            // notifying other users that the current user has left the project
                            val content =
                                "${UserManager.currentUser.name} has left the project"
                            val notification =
                                Notification.createNotification(
                                    content,
                                    chatChannel.chatChannelId
                                )
                            FireUtility.sendNotificationToChannel(
                                notification
                            ) { it1 ->
                                if (!it1.isSuccessful) {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_8 -> {
                selectImage(IMAGE_CHAT)
            }
            OPTION_9 -> {
                selectChatUploadDocuments()
            }
            OPTION_10, OPTION_11 -> {
                if (project != null) {
                    val currentFeedRecycler = findViewById<RecyclerView>(R.id.pager_items_recycler)
                    if (currentFeedRecycler != null) {
                        val holderView = currentFeedRecycler.findViewWithTag<View>(project.id)
                        val projectViewHolder = currentFeedRecycler.getChildViewHolder(holderView)
                        if (projectViewHolder != null && projectViewHolder is ProjectViewHolder) {
                            projectViewHolder.saveUnSaveProject(project)
                        }
                    }
                }
            }
            OPTION_12 -> {
                if (project != null) {
                    if (project.isArchived) {

                        val frag = MessageDialogFragment.builder("Are you sure you want to un-archive this project?")
                            .setTitle("Un-archiving project ... ")
                            .setPositiveButton("Un-Archive") { _, _ ->
                                unArchiveProject(project)
                            }
                            .setNegativeButton("Cancel") { a, _ ->
                                a.dismiss()
                            }
                            .build()

                        frag.show(supportFragmentManager, MessageDialogFragment.TAG)

                    } else {

                        val frag = MessageDialogFragment.builder("Are you sure you want to archive this project?")
                            .setTitle("Archiving project ...")
                            .setPositiveButton("Archive") { _, _ ->
                                archiveProject(project)
                            }.setNegativeButton("Cancel") { a, _ ->
                                a.dismiss()
                            }.build()

                        frag.show(supportFragmentManager, MessageDialogFragment.TAG)
                    }
                }
            }
            OPTION_13 -> {
                if (project != null) {
                    FireUtility.unArchiveProject(project) {
                        if (it.isSuccessful) {
                            project.expiredAt = -1
                            project.isArchived = false
                            viewModel.updateLocalProject(project)
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_14 -> {
                if (project != null) {
                    val report = Report.getReportForProject(project)
                    val bundle = bundleOf(REPORT to report)
                    navController.navigate(R.id.reportFragment, bundle, slideRightNavOptions())
                }
            }
            OPTION_15 -> {
                val bundle = bundleOf(PREVIOUS_PROJECT to project)
                navController.navigate(R.id.createProjectFragment, bundle, slideRightNavOptions())
            }
            OPTION_16 -> {
                navController.navigate(R.id.projectFragment, bundleOf(PROJECT to project, TITLE to project?.name), slideRightNavOptions())
            }
            OPTION_17 -> {
                selectImage(IMAGE_PROFILE)
            }
            OPTION_18 -> {
                viewModel.setCurrentImage(null)
            }
            OPTION_19, OPTION_20, OPTION_21, OPTION_22 -> {

            }
            OPTION_23 -> {
                // log out
                UserManager.logOut(this) {
                    navController.navigate(R.id.loginFragment, null, slideRightNavOptions())
                    viewModel.signOut {}
                }
            }
            OPTION_24 -> {
                // saved pr
                navController.navigate(R.id.savedProjectsFragment, null, slideRightNavOptions())
            }
            OPTION_25 -> {
                // archive
                navController.navigate(R.id.archiveFragment, null, slideRightNavOptions())
            }
            OPTION_26 -> {
                // requests
                navController.navigate(R.id.myRequestsFragment, null, slideRightNavOptions())
            }
            OPTION_27 -> {
                // settings
                navController.navigate(R.id.settingsFragment, null, slideRightNavOptions())
            }
            OPTION_28 -> {
                val currentFocusedTag = viewModel.currentFocusedTag
                if (currentFocusedTag != null) {
                    val changes = mapOf(INTERESTS to FieldValue.arrayUnion(currentFocusedTag))
                    FireUtility.updateUser2(changes) {
                        if (it.isSuccessful) {
                            Snackbar.make(binding.root, "Added $currentFocusedTag to your interests", Snackbar.LENGTH_LONG).show()
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_29 -> {
                if (comment != null) {
                    onReportClick(comment)
                }
            }
            OPTION_30 -> {
                if (comment != null) {
                    onCommentDelete(comment)
                }
            }
        }
    }

    private fun removeUserFromProject(user: User, chatChannel: ChatChannel) {
        FireUtility.removeUserFromProject(
            user,
            chatChannel.projectId,
            chatChannel.chatChannelId
        ) {
            if (it.isSuccessful) {

                Snackbar.make(
                    binding.root,
                    "Removed ${user.name} from project",
                    Snackbar.LENGTH_LONG
                ).show()

                val content =
                    "${user.name} has left the project"
                val notification =
                    Notification.createNotification(
                        content,
                        chatChannel.chatChannelId
                    )

                getProjectImpulsive(chatChannel.projectId) { it1 ->
                    val contributors = it1.contributors.removeItemFromList(user.id)
                    it1.contributors = contributors
                    viewModel.insertProjects(it1)
                }

                viewModel.removeProjectFromUserLocally(
                    chatChannel.chatChannelId,
                    chatChannel.projectId,
                    user
                )

                FireUtility.sendNotificationToChannel(
                    notification
                ) { it1 ->
                    if (!it1.isSuccessful) {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun archiveProject(project: Project) {
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

}
