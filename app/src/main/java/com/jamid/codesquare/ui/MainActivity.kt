package com.jamid.codesquare.ui

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.*
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.os.bundleOf
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
import com.android.billingclient.api.PurchasesUpdatedListener
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.FireUtility.dislikeUser2
import com.jamid.codesquare.FireUtility.likeUser2
import com.jamid.codesquare.PlayBillingController.PremiumState.*
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.ImageSelectType.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalPagingApi
class MainActivity : LauncherActivity(), LocationItemClickListener, PostInviteListener,
    PostClickListener, PostRequestListener, UserClickListener, ChatChannelClickListener, NotificationItemClickListener, CommentListener, OptionClickListener, NetworkStateListener {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var previouslyFetchedLocation: Location? = null
    private var currentImageViewer: View? = null
    private var animationStartView: View? = null
    private var animationEndView: View? = null
    private var isImageViewMode = false
    private var currentIndefiniteSnackbar: Snackbar? = null
    private lateinit var networkManager: MyNetworkManager
    lateinit var playBillingController: PlayBillingController
    var subscriptionFragment: SubscriptionFragment? = null
    var optionsFragment: OptionsFragment? = null
    private var isNetworkAvailable = false


    var initialLoadWaitFinished = false


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

                                if (navController.currentDestination?.id == R.id.homeFragment && binding.mainTabLayout.selectedTabPosition == 1) {
                                    return@getChatChannel
                                }

                                val lastMessage = chatChannel.lastMessage
                                if (lastMessage != null) {
                                    showCustomChipNotification("${chatChannel.postTitle}: ${lastMessage.sender.name} has sent a message", chatChannel.postImage) {
                                        onChannelClick(chatChannel)
                                    }
                                    /*Snackbar.make(binding.root, "${chatChannel.postTitle}: ${lastMessage.sender.name} has sent a message", Snackbar.LENGTH_INDEFINITE).setAction("View"){
                                        onChannelClick(chatChannel)
                                    }.setBehavior(NoSwipeBehavior()).show()*/
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBitmapDrawable(bitmap: Bitmap): Drawable {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.height, bitmap.height, false)
        val length = resources.getDimension(R.dimen.unit_len) * 6
        val drawable = RoundedBitmapDrawableFactory.create(resources, scaledBitmap).also {
            it.cornerRadius = length
        }
        return drawable
    }

    fun showCustomChipNotification(msg: String, image: String? = null, onClick: () -> Unit) {
        binding.mainNotifyChip.text = msg

        var dy = resources.getDimension(R.dimen.action_height)
        // default 56dp

        if (binding.mainTabLayout.isVisible) {
            // add extra 64 dp
            dy += resources.getDimension(R.dimen.large_padding)
        }

        if (image != null) {
            binding.mainNotifyChip.isChipIconVisible = true
            downloadBitmapUsingFresco(this, image) { i ->
                runOnUiThread {
                    if (i != null) {
                        binding.mainNotifyChip.chipIcon = getBitmapDrawable(i)
                    }
                }
            }
        }

        binding.mainNotifyChip.show()

        fun slideReset() {
            val slideResetAni = binding.mainNotifyChip.slideReset()
            slideResetAni.doOnEnd {
                binding.mainNotifyChip.hide()
            }
        }

        // auto
        val slideDownAni = binding.mainNotifyChip.slideDown(dy)
        slideDownAni.doOnEnd {
            lifecycleScope.launch {
                delay(5000)

                runOnUiThread {
                    if (binding.mainNotifyChip.translationY != 0f) {
                       slideReset()
                    }
                }
            }
        }

        binding.mainNotifyChip.setOnClickListener {
            slideReset()
            onClick()
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

    fun onLocationPermissionRequestRejected() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()
        editor.putBoolean("location_permission_hard_reject", true)
        editor.apply()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        // initialize all nodes
        MobileAds.initialize(this) {}

        if (!isNetworkConnected()) {
            onNetworkNotAvailable()
        }

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
                    badgeDrawable.badgeTextColor = getColorResource(R.color.white)
                    attachBadgeDrawable(badgeDrawable, binding.mainToolbar, R.id.notifications)
                }
            }
        }

        UserManager.currentUserLive.observe(this) {
            if (it != null) {

                viewModel.currentUserBitmap = null
                viewModel.insertCurrentUser(it)

                if (it.premiumState.toInt() != -1) {
                    viewModel.deleteAdPosts()
                }

                setMessagesListener(it.chatChannels)

                viewModel.checkAndUpdateLocalPosts(it)
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
        networkManager = MyNetworkManager(this, this)
        lifecycle.addObserver(networkManager)
        lifecycle.addObserver(playBillingController)

        setLiveDataObservers()

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

//        binding.navHostFragment
    }

    private fun getCurrentFragmentInfo(navHostFragment: NavHostFragment) {
        val lastFragment = navHostFragment.childFragmentManager.fragments.lastOrNull()
        if (lastFragment != null) {
            /*when (lastFragment) {
                is ChatFragment -> {
                    Log.d(TAG, "onViewCreated: Last Fragment is Chat Fragment")
                }
                is ChatDetailFragment -> {
                    Log.d(TAG, "onViewCreated: Last Fragment is Chat Detail Fragment")
                }
                is MessageDetailFragment -> {
                    Log.d(TAG, "onViewCreated: Last Fragment is Message Detail Fragment")
                }
                is ChannelGuidelinesFragment -> {
                    Log.d(TAG, "onViewCreated: Last Fragment is ChannelGuidelinesFragment")
                }
                else -> {
                    Log.d(TAG, "onViewCreated: ${lastFragment::class.java.simpleName}")
                }
            }*/
            Log.d(TAG, "onViewCreated: ${lastFragment::class.java.simpleName}")
        } else {
            Log.d(TAG, "onViewCreated: Last fragment is null")
        }
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

            invalidateOptionsMenu()

            invalidateTabLayoutBadges()

            binding.mainToolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }

            binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(this,
                R.animator.app_bar_elevation)

            val dy = resources.getDimension(R.dimen.comment_layout_translation)
            binding.commentBottomRoot.slideDown(dy)

            binding.mainProgressBar.hide()

            if (destination.id != R.id.homeFragment) {
                binding.mainPrimaryBtn.hide()
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
                if (isNightMode()) {
                    ContextCompat.getDrawable(this, R.drawable.ic_logo_xy_night)
                } else {
                    ContextCompat.getDrawable(this, R.drawable.ic_logo_xy)
                }
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

            when (destination.id) {
                R.id.notificationCenterFragment -> updateUi(shouldShowTabLayout = true)
                in authFragments -> updateUi(
                    shouldShowAppBar = false,
                    baseFragmentBehavior = null
                )
                R.id.homeFragment -> {
                    hideKeyboard(binding.root)
                    lifecycleScope.launch {
                        delay(300)
                        updateUi(
                            shouldShowTabLayout = true
                        )
                    }
                }
                R.id.profileFragment -> {
                    hideKeyboard(binding.root)
                    updateUi()
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

    private fun invalidateTabLayoutBadges() {
        val acceptedFragments = listOf(R.id.homeFragment, R.id.notificationCenterFragment)
        if (!acceptedFragments.contains(navController.currentDestination?.id)) {
            for (i in 0 until binding.mainTabLayout.tabCount) {
                binding.mainTabLayout.getTabAt(i)?.removeBadge()
            }
        }
    }

    private fun onReceiveData() {
        val extras = intent.extras
        if (extras != null) {
            when {
                extras.containsKey(CHANNEL_ID) -> {
                    // chat notification
                    binding.mainPrimaryBtn.hide()

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
                    binding.mainPrimaryBtn.hide()

                    if (extras.containsKey(TYPE)) {
                        val type = extras[TYPE] as String?

                        Log.d(TAG, "onReceiveData: $type")

                        val pos = when (type) {
                            "0" -> {
                                0
                            }
                            "1" -> {
                                1
                            }
                            "-1" -> {
                                2
                            }
                            else -> {
                                0
                            }
                        }

                        navController.navigate(
                            R.id.notificationCenterFragment,
                            bundleOf(TYPE to pos),
                            slideRightNavOptions()
                        )
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
        toolbarAdjustment: ToolbarAdjustment = ToolbarAdjustment()
    ) = runOnUiThread {

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

        if (!toolbarAdjustment.shouldShowSubTitle) {
            binding.mainToolbar.subtitle = null
        }

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
            viewModel.setCurrentPostLocation(
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

    override fun onLocationClick(autocompletePrediction: AutocompletePrediction) {

    }

    override fun onPostClick(post: Post) {
        val bundle = bundleOf(POST to post, "image_pos" to 0)
        viewModel.insertPosts(post)
        viewModel.insertPostToCache(post)
        navController.navigate(R.id.postFragmentContainer, bundle, slideRightNavOptions())
    }

    override fun onPostClick(postMinimal2: PostMinimal2) {
        getPostImpulsive(postMinimal2.objectID) {
            onPostClick(it)
        }
    }

    override fun onPostLikeClick(post: Post, onChange: (newPost: Post) -> Unit) {

        val currentUser = UserManager.currentUser

        if (post.isLiked) {
            FireUtility.dislikePost2(post) { newPost, it ->
                if (it.isSuccessful) {
                    onChange(newPost)
                    viewModel.insertPost(newPost)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {
            val content = currentUser.name + " liked your post"
            val title = post.name
            val notification = Notification.createNotification(
                content,
                post.creator.userId,
                postId = post.id,
                title = title
            )
            FireUtility.likePost2(post) { newPost, it ->
                if (it.isSuccessful) {
                    // check if notification already exists
                    onChange(newPost)
                    viewModel.insertPost(newPost)
                    sendNotificationImpulsive(notification)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onPostSaveClick(post: Post, onChange: (newPost: Post) -> Unit) {
        if (post.isSaved) {
            // un-save
            FireUtility.undoSavePost(post) { newPost, it ->
                if (it.isSuccessful) {
                    onChange(newPost)
                    viewModel.insertPost(newPost)
                   /* viewModel.insertPostToCache(newPost)
                    viewModel.insertPosts(newPost)*/
                    viewModel.deleteReferenceItem(newPost.id)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {
            // save
            FireUtility.savePost2(post) { newPost, it ->
                if (it.isSuccessful) {
                    onChange(newPost)
                    viewModel.insertPost(newPost)
                    /*viewModel.insertPostToCache(newPost)
                    viewModel.insertPosts(newPost)*/
                    Snackbar.make(binding.root, "Saved this post", Snackbar.LENGTH_LONG)
                        .setAction("View all") {
                            navController.navigate(R.id.savedPostsFragment, null, slideRightNavOptions())
                        }
                        .setBehavior(NoSwipeBehavior())
                        .show()
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    private fun setLiveDataObservers() {

        playBillingController.isPurchaseAcknowledged.observe(this) { isPurchaseAcknowledged ->
            if (isPurchaseAcknowledged == true) {
                if (subscriptionFragment != null && subscriptionFragment!!.isVisible) {
                    subscriptionFragment!!.dismiss()
                }
                navController.navigate(R.id.subscriberFragment, null, slideRightNavOptions())
            }
        }

        playBillingController.premiumState.observe(this) {
            val premiumState = it ?: return@observe

            when (premiumState) {
                STATE_NO_PURCHASE -> {
                    if (subscriptionFragment != null && subscriptionFragment!!.isVisible) {
                        subscriptionFragment!!.dismiss()
                    }
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
                viewModel.setProductDetailsList(p1)
            }
        }

        playBillingController.errors.observe(this) {
            if (it != null) {
                subscriptionFragment?.dismiss()
            }
        }

    }

    override fun onPostJoinClick(post: Post, onChange: (newPost: Post) -> Unit) {
        if (post.isRequested) {
            // undo request
            onPostUndoClick(post, onChange)
        } else {
            // send request
            val currentUser = UserManager.currentUser
            val content = currentUser.name + " wants to join your post"

            val notification = Notification.createNotification(
                content,
                post.creator.userId,
                type = 1,
                userId = currentUser.id,
                title = post.name
            )

            fun onRequestSent(postRequest: PostRequest) {
                val requestsList = post.requests.addItemToList(postRequest.requestId)
                post.requests = requestsList
                post.isRequested = true

                viewModel.updateLocalPost(post)
                viewModel.insertPostRequests(postRequest)

                onChange(post)
            }

            FireUtility.joinPost(notification.id, post) { task, postRequest ->
                if (task.isSuccessful) {

                    Snackbar.make(binding.root, "Post request sent", Snackbar.LENGTH_LONG)
                        .setBehavior(NoSwipeBehavior())
                        .show()

                    onRequestSent(postRequest)

                    sendNotificationImpulsive(notification)
                } else {
                    viewModel.setCurrentError(task.exception)
                }
            }
        }


    }

    private fun updateLocalPostOnRequestAccept(post: Post, postRequest: PostRequest) {
        // removing the request id from post->requests list
        val newRequestsList = post.requests.removeItemFromList(postRequest.requestId)
        post.requests = newRequestsList

        // adding the request sender to the post->contributors list
        val newContList = post.contributors.addItemToList(postRequest.senderId)
        post.contributors = newContList

        // recording the time of update locally
        post.updatedAt = System.currentTimeMillis()

        val processedPost = processPosts(arrayOf(post)).first()

        viewModel.updateLocalPost(processedPost)
    }

    fun isEligibleToCreateProject(): Boolean {
        val currentUser = UserManager.currentUser
        return if (currentUser.posts.size + currentUser.archivedProjects.size > 0) {
            // check if premium user
            currentUser.premiumState.toInt() == 1
        } else {
            true
        }
    }

    fun isEligibleToAddContributor(post: Post): Boolean {
        val currentUser = UserManager.currentUser
        return if (post.contributors.size < 5) {
            true
        } else {
            currentUser.premiumState.toInt() == 1
        }
    }

    fun isEligibleToCollaborate(): Boolean {
        val currentUser = UserManager.currentUser
        return currentUser.collaborationsCount.toInt() < 1
    }

    fun showLimitDialog(cause: AdLimit) {
        when (cause) {
            AdLimit.MAX_POSTS -> showMaxPostsLimitUi()
            AdLimit.MAX_CONTRIBUTORS -> showMaxContributorsLimitUi()
            AdLimit.MAX_COLLABORATIONS -> showMaxCollaborationsLimitUi()
        }
    }

    private fun showPreSubscriptionDialog(msg: String) {
        val frag = MessageDialogFragment.builder(msg)
            .setPositiveButton("Upgrade") { _, _ ->
                showSubscriptionFragment()
            }.setNegativeButton("Cancel") { a, _ ->
                a.dismiss()
            }.build()

        frag.show(supportFragmentManager, MessageDialogFragment.TAG)
    }

    private fun showMaxPostsLimitUi() {
        val currentUser = UserManager.currentUser
        val msg = if (currentUser.archivedProjects.isNotEmpty()) {
            "You have created a post and archived it. To create more, upgrade your subscription plan"
        } else {
            "You have already created 1 post. To create more, upgrade your subscription plan!"
        }

        showPreSubscriptionDialog(msg)
    }

    private fun showMaxContributorsLimitUi() {
        val msg = "This post already has 5 contributors. To add more, upgrade your subscription plan."
        showPreSubscriptionDialog(msg)
    }

    private fun showMaxCollaborationsLimitUi() {
        val msg = "You have already collaborated once. To collaborate with more posts, upgrade your subscription plan."
        showPreSubscriptionDialog(msg)
    }

    override fun onPostRequestAccept(postRequest: PostRequest, onFailure: () -> Unit) {
        val currentUser = UserManager.currentUser

        getPostImpulsive(postRequest.postId) { post ->
            if (isEligibleToAddContributor(post)) {
                FireUtility.acceptPostRequest(post, postRequest) { it1 ->
                    if (it1.isSuccessful) {
                        // 1. update the local post
                        updateLocalPostOnRequestAccept(post, postRequest)

                        // 2. insert the new user to local database
                        getNewContributorOnRequestAccept(postRequest.senderId)

                        // 3. delete the post request
                        viewModel.deletePostRequest(postRequest)

                        viewModel.deleteNotificationById(postRequest.notificationId)

                        // 4. send notification
                        val title = post.name
                        val content = currentUser.name + " has accepted your post request"
                        val notification = Notification.createNotification(
                            content,
                            postRequest.senderId,
                            postId = post.id,
                            title = title
                        )

                        sendNotificationImpulsive(notification)

                    } else {
                        Log.e(
                            TAG,
                            "onPostRequestAccept: ${it1.exception?.localizedMessage}"
                        )
                    }
                }
            } else {
                showLimitDialog(AdLimit.MAX_CONTRIBUTORS)
            }
        }
    }

    private fun getNewContributorOnRequestAccept(userId: String) {
        getUser(userId) {}
    }

    // post request must have post included inside it
    override fun onPostRequestCancel(postRequest: PostRequest) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " rejected your post request"
        val title = postRequest.post.name

        val notification = Notification.createNotification(
            content,
            postRequest.senderId,
            userId = currentUser.id,
            title = title
        )

        FireUtility.rejectRequest(postRequest) {
            if (!it.isSuccessful) {
                viewModel.setCurrentError(it.exception)
            } else {
                viewModel.deleteLocalPostRequest(postRequest)
                viewModel.deleteNotificationById(postRequest.notificationId)

                sendNotificationImpulsive(notification)
            }
        }
    }

    override fun onPostLoad(post: Post) {
        if (post.expiredAt > System.currentTimeMillis()) {
            // delete post
            FireUtility.deletePost(post) {
                if (it.isSuccessful) {
                    // delete requests and let the users know
                    FireUtility.getAllRequestsForPost(post) { result ->
                        when (result) {
                            is Result.Error -> viewModel.setCurrentError(result.exception)
                            is Result.Success -> {
                                val requests = result.data
                                FireUtility.postDeletePost(requests) { it1 ->
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

    override fun onAdError(post: Post) {
        viewModel.deleteLocalPost(post)
    }

    override fun onPostLocationClick(post: Post) {
        navController.navigate(R.id.locationPostsFragment, bundleOf(TITLE to "Showing posts near", SUB_TITLE to post.location.address, "location" to post.location), slideRightNavOptions())
    }

    @Suppress("LABEL_NAME_CLASH")
    override suspend fun onCheckForStaleData(post: Post, onUpdate: (newPost: Post) -> Unit) {

        fun onChangeNeeded(creator: User) {
            val changes = mapOf(CREATOR to creator.minify(), UPDATED_AT to System.currentTimeMillis())

            FireUtility.updatePost(post.id, changes) { it1 ->
                if (it1.isSuccessful) {
                    post.creator = creator.minify()

                    runOnUiThread {
                        onUpdate(post)
                    }

                    viewModel.insertPost(post)
                } else {
                    Log.e(
                        PostFragment.TAG,
                        "setCreatorRelatedUi: ${it1.exception?.localizedMessage}"
                    )
                }
            }
        }

        if (UserManager.currentUserId == post.creator.userId) {
            if (UserManager.currentUser.minify() != post.creator) {
                onChangeNeeded(UserManager.currentUser)
            }
        } else {
            getUserImpulsive(post.creator.userId) { creator ->
                if (creator.minify() != post.creator) {
                    onChangeNeeded(creator)
                }
            }
        }
    }

    override fun onPostUpdate(newPost: Post) {
        viewModel.insertPost(newPost)
    }

    fun getPostImpulsive(postId: String, onPostFetch: (Post) -> Unit) {
        val cachedPost = viewModel.getCachedPost(postId)
        if (cachedPost == null) {
            viewModel.getPost(postId) { localPost ->
                runOnUiThread {
                    if (localPost != null) {
                        viewModel.insertPostToCache(localPost)
                        onPostFetch(localPost)
                    } else {
                        getPost(postId) { post ->
                            onPostFetch(post)
                        }
                    }
                }
            }
        } else {
            onPostFetch(cachedPost)
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

    fun getPost(postId: String, onFetch: (Post) -> Unit) {
        FireUtility.getPost(postId) child@ {
            val postResult = it ?: return@child
            when (postResult) {
                is Result.Error -> Log.e(
                    TAG,
                    "getPost: Error while trying to get post with postId: $postId"
                )
                is Result.Success -> {
                    val formattedPost = processPosts(arrayOf(postResult.data))[0]
                    viewModel.insertPostToCache(formattedPost)
                    viewModel.insertPosts(formattedPost)
                    onFetch(formattedPost)
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


    override fun onPostRequestPostDeleted(postRequest: PostRequest) {
        deletePostRequest(postRequest)
    }

    private fun deletePostRequest(postRequest: PostRequest) {
        FireUtility.deletePostRequest(postRequest) {
            if (it.isSuccessful) {
                viewModel.deletePostRequest(postRequest)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onPostRequestSenderDeleted(postRequest: PostRequest) {
        deletePostRequest(postRequest)
    }

    override fun updatePostRequest(newPostRequest: PostRequest) {
        viewModel.insertPostRequests(newPostRequest)
    }

    override fun onPostRequestUndo(postRequest: PostRequest) {
        FireUtility.undoJoinPost(postRequest) {
            if (it.isSuccessful) {
                viewModel.deletePostRequest(postRequest)
                getPostImpulsive(postRequest.postId) { post ->
                    val requestsList = post.requests.removeItemFromList(postRequest.requestId)
                    post.requests = requestsList
                    post.isRequested = false
                    viewModel.insertPosts(post)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onPostRequestClick(postRequest: PostRequest) {
        getPostImpulsive(postRequest.postId) { post ->
            onPostClick(post)
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(postRequest: PostRequest) {

        fun updatePostRequest(changes: Map<String, Any?>) {
            FireUtility.updatePostRequest(postRequest.requestId, changes) { postRequestUpdateResult ->
                if (postRequestUpdateResult.isSuccessful) {
                    viewModel.insertPostRequests(postRequest)
                }
            }
        }

        fun onChangeNeeded(post: Post) {
            val changes = mapOf("post" to post.minify(), "updatedAt" to System.currentTimeMillis())
            postRequest.post = post.minify()
            postRequest.updatedAt = System.currentTimeMillis()
            updatePostRequest(changes)
        }

        fun onChangeNeeded(user: User) {
            val changes = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            postRequest.sender = user.minify()
            postRequest.updatedAt = System.currentTimeMillis()
            updatePostRequest(changes)
        }

        getPostImpulsive(postRequest.postId) { post ->
            if (post.updatedAt > postRequest.updatedAt) {
                onChangeNeeded(post)
            }
        }

        getUserImpulsive(postRequest.senderId) { user ->
            if (user.minify() != postRequest.sender) {
                onChangeNeeded(user)
            }
        }

    }

    override fun onPostCreatorClick(post: Post) {
        if (post.creator.userId == UserManager.currentUserId) {
            navController.navigate(
                R.id.profileFragment,
                null,
                slideRightNavOptions()
            )
        } else {
            getUserImpulsive(post.creator.userId) {
                onUserClick(it)
            }
        }
    }


    override fun onPostCommentClick(post: Post) {
        val bundle = bundleOf(
            COMMENT_CHANNEL_ID to post.commentChannel,
            PARENT to post,
            TITLE to post.name
        )

        navController.navigate(R.id.commentsFragment, bundle, slideRightNavOptions())
    }

    override fun onPostOptionClick(post: Post) {
        val currentUser = UserManager.currentUser
        val creatorId = post.creator.userId
        val isCurrentUser = creatorId == currentUser.id

        val option1 = OPTION_32
        val option2 = OPTION_10
        val option3 = OPTION_11
        val option4 = OPTION_12
        val option5 = OPTION_13
        val option6 = OPTION_14
        val option7 = OPTION_15

        val saveUnSaveText = if (post.isSaved) {
            option3
        } else {
            option2
        }
        val archiveToggleText = if (post.archived) {
            option5
        } else {
            option4
        }
        val (choices, icons) = if (isCurrentUser) {
            arrayListOf(saveUnSaveText, archiveToggleText, option7) to arrayListOf(if (post.isSaved) {R.drawable.ic_round_bookmark_remove_24} else {R.drawable.ic_round_bookmark_add_24}, R.drawable.ic_round_archive_24, R.drawable.ic_round_edit_note_24)
        } else {
            arrayListOf(option1, saveUnSaveText, option6) to arrayListOf(R.drawable.ic_round_account_circle_24, if (post.isSaved) {R.drawable.ic_round_bookmark_remove_24} else {R.drawable.ic_round_bookmark_add_24}, R.drawable.ic_round_report_24)
        }

        optionsFragment = OptionsFragment.newInstance(post.name, choices, icons, post = post, user = post.creator.toUser())
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    override fun onPostOptionClick(postMinimal2: PostMinimal2) {
        getPostImpulsive(postMinimal2.objectID) {
            onPostOptionClick(it)
        }
    }

    override fun onPostUndoClick(post: Post, onChange: (newPost: Post) -> Unit) {
        FireUtility.getPostRequest(post.id, UserManager.currentUserId) {
            val postRequestResult = it ?: return@getPostRequest

            when (postRequestResult) {
                is Result.Error -> Log.e(TAG, "onPostUndoClick: ${postRequestResult.exception.localizedMessage}")
                is Result.Success -> {
                    val postRequest = postRequestResult.data
                    FireUtility.undoJoinPost(postRequest) { it1 ->
                        if (it1.isSuccessful) {
                            post.isRequested = false
                            val newList = post.requests.removeItemFromList(postRequest.requestId)
                            post.requests = newList
                            viewModel.updateLocalPost(post)
                            onChange(post)
                            viewModel.deletePostRequest(postRequest)
                        } else {
                            viewModel.setCurrentError(it1.exception)
                        }
                    }
                }
            }
        }


    }

    override fun onPostContributorsClick(post: Post) {

        val bundle = bundleOf(
            POST to post,
            TITLE to CONTRIBUTORS.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            SUB_TITLE to post.name.uppercase()
        )

        navController.navigate(
            R.id.postContributorsFragment,
            bundle,
            slideRightNavOptions()
        )
    }

    override fun onPostSupportersClick(post: Post) {
        val bundle = bundleOf(POST_ID to post.id)
        navController.navigate(R.id.postLikesFragment, bundle, slideRightNavOptions())
    }

    override fun onPostNotFound(post: Post) {

    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }
        navController.navigate(R.id.profileFragment, bundle, slideRightNavOptions())
    }

    override fun onUserClick(userId: String) {
        getUserImpulsive(userId) {
            onUserClick(it)
        }
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
        dislikeUser2(userId) {
            if (it.isSuccessful) {
                viewModel.dislikeLocalUserById(userId)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun likeUser(userId: String) {
        val currentUser = UserManager.currentUser
        likeUser2(userId) {
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
            dislikeUser2(user.id) {
                Snackbar.make(binding.root, "Disliked ${user.name}", Snackbar.LENGTH_LONG)
                    .setBehavior(NoSwipeBehavior())
                    .show()
            }
        } else {
            likeUser2(user.id) {
                Snackbar.make(binding.root, "Liked ${user.name}", Snackbar.LENGTH_LONG)
                    .setBehavior(NoSwipeBehavior())
                    .show()
            }
        }
    }

    override fun onUserLikeClick(userId: String) {
        getUserImpulsive(userId) {
            onUserLikeClick(it)
        }
    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {
        getUserImpulsive(userMinimal.objectID) {
            onUserLikeClick(it)
        }
    }

    override fun onCommentLikeClicked(comment: Comment, onChange: (newComment: Comment) -> Unit) {
        val currentUser = UserManager.currentUser
        if (!comment.isLiked) {
            FireUtility.likeComment2(comment) { newComment, _ ->

                onChange(newComment)

                val content = currentUser.name + " liked your comment"
                val notification = Notification.createNotification(
                    content,
                    comment.senderId,
                    userId = currentUser.id,
                    title = currentUser.name
                )

                FireUtility.sendNotification(notification) {
                    if (it.isSuccessful) {
                        viewModel.updateComment(newComment)
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            }
        } else {
            FireUtility.dislikeComment2(comment) { newComment, task ->
                if (task.isSuccessful) {
                    onChange(newComment)
                    viewModel.updateComment(newComment)
                } else {
                    viewModel.setCurrentError(task.exception)
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
            TITLE to "Reply to ${comment.sender.name}'s comment",
            COMMENT_CHANNEL_ID to comment.threadChannelId
        )
        navController.navigate(
            R.id.commentsFragment,
            bundle,
            slideRightNavOptions()
        )
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
            arrayListOf(OPTION_29, OPTION_30) to arrayListOf(R.drawable.ic_round_report_24, R.drawable.ic_round_delete_24)
        } else {
            name = comment.sender.name
            arrayListOf(OPTION_29) to arrayListOf(R.drawable.ic_round_report_24)
        }

        optionsFragment = OptionsFragment.newInstance(title = "Comment by $name", options = choices, icons = icons, comment = comment)
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    override fun onCheckForStaleData(comment: Comment, onUpdate: (newComment: Comment) -> Unit) {

        fun onChangeNeeded(user: User) {
            val changes = mapOf(SENDER to user.minify(), UPDATED_AT to System.currentTimeMillis())
            comment.sender = user.minify()
            comment.updatedAt = System.currentTimeMillis()

            FireUtility.updateComment(comment, changes) { _, task ->
                if (task.isSuccessful) {
                    onUpdate(comment)
                } else {
                    Log.e(TAG, "onChangeNeeded: ${task.exception?.localizedMessage}")
                }
            }
        }

        getUserImpulsive(comment.senderId) { user ->
            if (user.minify() != comment.sender) {
                onChangeNeeded(user)
            }
        }

    }

    override fun onCommentInfoClick(comment: Comment) {
        val bundle = bundleOf(COMMENT to comment)
        navController.navigate(R.id.commentLikesFragment, bundle, slideRightNavOptions())
    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        chatChannel.isNewLastMessage = false
        viewModel.updateChatChannel(chatChannel)

        val bundle = bundleOf(
            CHAT_CHANNEL to chatChannel,
            TITLE to chatChannel.postTitle
        )

        navController.navigate(
            R.id.chatContainerSample,
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
            runOnUiThread {
                imageViewBinding.fullscreenImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.MATCH_PARENT
                    height = ConstraintLayout.LayoutParams.MATCH_PARENT
                }
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

            backgroundView.setBackgroundColor(getColorResource(R.color.dank_grey))

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

        val postId = notification.postId
        if (postId != null) {
            getPostImpulsive(postId) { post ->
                onPostClick(post)
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have post Id")
        }

        val commentId = notification.commentId
        if (commentId != null) {
            FireUtility.getComment(commentId) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        val comment = it.data
                        if (comment.commentLevel.toInt() == 0) {

                            getPostImpulsive(comment.postId) { post ->
                                onPostCommentClick(post)
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
        if (Firebase.auth.currentUser != null && !Firebase.auth.currentUser!!.isEmailVerified)
            viewModel.setListenerForEmailVerification()
    }

    override fun onStop() {
        super.onStop()
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
    override fun onPostInviteAccept(postInvite: PostInvite, onFailure: () -> Unit) {

        val currentUser = UserManager.currentUser
        val content = currentUser.name + " accepted your post invite"
        val title = postInvite.post.name
        val notification = Notification.createNotification(
            content,
            postInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        fun onPostAccept(sender: User) {

            // local changes
            val newList =
                sender.postInvites.removeItemFromList(
                    postInvite.id
                )
            sender.postInvites = newList

            sendNotificationImpulsive(notification)

        }

        Firebase.firestore.collection(POSTS)
            .document(postInvite.postId)
            .get()
            .addOnSuccessListener { postDocument ->
                if (postDocument.exists()) {

                    val post = postDocument.toObject(Post::class.java)!!

                    if (post.contributors.size < 5 || currentUser.premiumState.toInt() == 1) {
                        FireUtility.acceptPostInvite(currentUser, postInvite) {
                            if (it.isSuccessful) {

                                // current user was added to post successfully
                                viewModel.deletePostInvite(postInvite)
                                viewModel.deleteNotificationById(postInvite.notificationId)

                                // extra
                                getUserImpulsive(postInvite.senderId) { user ->
                                    onPostAccept(user)
                                }
                            } else {
                                // something went wrong while trying to add the current user to the post
                                viewModel.setCurrentError(it.exception)
                            }
                        }
                    } else {
                        onFailure()

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
                } else {
                    onPostInvitePostDeleted(postInvite)
                }
            }.addOnFailureListener {
                Log.e(TAG, "onPostInviteAccept: ${it.localizedMessage}")
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
                                Log.e(TAG, "onPostRequestAccept: ${it1.exception?.localizedMessage}")
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
    override fun onPostInviteCancel(postInvite: PostInvite) {

        val currentUser = UserManager.currentUser
        val title = postInvite.post.name
        val content = currentUser.name + " has rejected your post invite"

        val notification = Notification.createNotification(
            content,
            postInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        fun onPostCancel(sender: User) {
            // local changes
            val newList =
                sender.postInvites.removeItemFromList(
                    postInvite.id
                )
            sender.postInvites = newList

            sendNotificationImpulsive(notification)
        }

        FireUtility.cancelPostInvite(postInvite) {
            if (it.isSuccessful) {
                // current user was added to post successfully
                viewModel.deletePostInvite(postInvite)
                viewModel.deleteNotificationById(postInvite.notificationId)

                getUserImpulsive(postInvite.senderId) { user ->
                    onPostCancel(user)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onPostInvitePostDeleted(postInvite: PostInvite) {

        fun onPostDelete(sender: User) {
            // local changes
            val newList =
                sender.postInvites.removeItemFromList(
                    postInvite.id
                )
            sender.postInvites = newList
        }

        // either the post was deleted or archived
        FireUtility.deletePostInvite(postInvite) {
            if (it.isSuccessful) {
                viewModel.deletePostInvite(postInvite)

                getUserImpulsive(postInvite.senderId) { user ->
                    onPostDelete(user)
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(postInvite: PostInvite) {

        fun updatePostInvite(changes: Map<String, Any?>) {
            FireUtility.updatePostInvite(postInvite.receiverId, postInvite.id, changes) { postInviteUpdateResult ->
                if (postInviteUpdateResult.isSuccessful) {
                    viewModel.insertPostInvites(postInvite)
                }
            }
        }

        fun onChangeNeeded(post: Post) {
            val changes = mapOf("post" to post.minify(), "updatedAt" to System.currentTimeMillis())
            postInvite.post = post.minify()
            postInvite.updatedAt = System.currentTimeMillis()
            updatePostInvite(changes)
        }

        fun onChangeNeeded(user: User) {
            val changes = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            postInvite.sender = user.minify()
            postInvite.updatedAt = System.currentTimeMillis()
            updatePostInvite(changes)
        }

        getPostImpulsive(postInvite.postId) { post ->
            if (post.updatedAt > postInvite.updatedAt) {
                onChangeNeeded(post)
            }
        }

        getUserImpulsive(postInvite.senderId) { user ->
            if (user.minify() != postInvite.sender) {
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
            IMAGE_PROFILE, IMAGE_TEST -> {
                sil.launch(intent)
            }
            IMAGE_CHAT, IMAGE_POST, IMAGE_REPORT -> {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                sil.launch(intent)
            }
        }
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        post: Post?,
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
                                "Successfully set ${user.name} as admin", Snackbar.LENGTH_LONG)
                                .setBehavior(NoSwipeBehavior())
                                .show()
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
                        .setBehavior(NoSwipeBehavior())
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
                        R.id.reportFragment,
                        bundleOf("report" to report),
                        slideRightNavOptions()
                    )

                }

            }
            OPTION_6 -> {
                if (chatChannel != null && user != null) {
                    val frag = MessageDialogFragment.builder("Are you sure you want to remove ${user.name} from the post?")
                        .setTitle("Removing user from post")
                        .setPositiveButton("Remove") { _, _ ->
                            removeUserFromPost(user, chatChannel)
                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }.build()

                    frag.show(supportFragmentManager, MessageDialogFragment.TAG)
                }
            }
            OPTION_7 -> {
                if (chatChannel != null && user != null) {
                    FireUtility.removeUserFromPost(
                        user,
                        chatChannel.postId,
                        chatChannel.chatChannelId
                    ) {
                        if (it.isSuccessful) {

                            navController.popBackStack(R.id.homeFragment, false)

                            viewModel.removePostFromUserLocally(
                                chatChannel.chatChannelId,
                                chatChannel.postId,
                                user
                            )


                            getPostImpulsive(chatChannel.postId) { it1 ->
                                val contributors =
                                    it1.contributors.removeItemFromList(
                                        UserManager.currentUserId
                                    )
                                it1.contributors =
                                    contributors
                                viewModel.updateLocalPost(
                                    it1
                                )
                            }

                            // notifying other users that the current user has left the post
                            val content =
                                "${UserManager.currentUser.name} has left the post"
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
                if (post != null) {
                    val currentFeedRecycler = findViewById<RecyclerView>(R.id.pager_items_recycler)
                    if (currentFeedRecycler != null) {
                        val holderView = currentFeedRecycler.findViewWithTag<View>(post.id)
                        val postViewHolder = currentFeedRecycler.getChildViewHolder(holderView)
                        if (postViewHolder != null && postViewHolder is PostViewHolder) {
                            postViewHolder.post = post
                            postViewHolder.onSaveBtnClick()
                        }
                    }
                }
            }
            OPTION_12 -> {
                if (post != null && !post.archived) {
                    val frag = MessageDialogFragment.builder("Are you sure you want to archive this post?")
                        .setTitle("Archiving post ...")
                        .setPositiveButton("Archive") { _, _ ->
                            archivePost(post)
                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }.build()

                    frag.show(supportFragmentManager, MessageDialogFragment.TAG)
                }
            }
            OPTION_13 -> {
                if (post != null && post.archived ) {
                    val frag = MessageDialogFragment.builder("Are you sure you want to un-archive this post?")
                        .setTitle("Un-archiving post ... ")
                        .setPositiveButton("Un-Archive") { _, _ ->
                            unArchivePost(post)
                        }
                        .setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }
                        .build()

                    frag.show(supportFragmentManager, MessageDialogFragment.TAG)
                    /*FireUtility.unArchivePost(post) {
                        if (it.isSuccessful) {
                            post.expiredAt = -1
                            post.archived = false
                            viewModel.updateLocalPost(post)
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }*/
                }
            }
            OPTION_14 -> {
                // TODO("Simplify")
                if (post != null) {
                    val report = Report.getReportForPost(post)
                    val bundle = bundleOf(REPORT to report)
                    navController.navigate(R.id.reportFragment, bundle, slideRightNavOptions())
                } else if (user != null) {
                    val report = Report.getReportForUser(user)
                    val bundle = bundleOf(REPORT to report)
                    navController.navigate(R.id.reportFragment, bundle, slideRightNavOptions())
                }

            }
            OPTION_15 -> {
                val bundle = bundleOf(PREVIOUS_POST to post)
                navController.navigate(R.id.createPostFragment, bundle, slideRightNavOptions())
            }
            OPTION_16 -> {
                navController.navigate(R.id.postFragmentContainer, bundleOf(POST to post), slideRightNavOptions())
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
                navController.navigate(R.id.savedPostsFragment, null, slideRightNavOptions())
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
                if (tag != null) {
                    val changes = mapOf(INTERESTS to FieldValue.arrayUnion(tag))
                    FireUtility.updateUser2(changes) {
                        if (it.isSuccessful) {
                            Snackbar.make(binding.root, "Added $tag to your interests", Snackbar.LENGTH_LONG)
                                .setBehavior(NoSwipeBehavior())
                                .show()
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
            OPTION_31 -> {
                navController.navigate(R.id.invitesFragment, null, slideRightNavOptions())
            }
            OPTION_32 -> {
                if (user != null) {
                    onUserClick(user)
                }
            }

            OPTION_34 -> {
                if (user != null) {
                    FireUtility.unblockUser(user) {
                        if (it.isSuccessful) {
                            Log.d(TAG, "onOptionClick: ")
                        } else {
                            Log.e(TAG, "onOptionClick: ${it.exception?.localizedMessage}")
                        }
                    }
                }
            }
        }
    }

    private fun removeUserFromPost(user: User, chatChannel: ChatChannel) {
        FireUtility.removeUserFromPost(
            user,
            chatChannel.postId,
            chatChannel.chatChannelId
        ) {
            if (it.isSuccessful) {

                Snackbar.make(
                    binding.root,
                    "Removed ${user.name} from post",
                    Snackbar.LENGTH_LONG
                ).setBehavior(NoSwipeBehavior()).show()

                val content =
                    "${user.name} has left the post"
                val notification =
                    Notification.createNotification(
                        content,
                        chatChannel.chatChannelId
                    )

                getPostImpulsive(chatChannel.postId) { it1 ->
                    val contributors = it1.contributors.removeItemFromList(user.id)
                    it1.contributors = contributors
                    viewModel.insertPosts(it1)
                }

                viewModel.removePostFromUserLocally(
                    chatChannel.chatChannelId,
                    chatChannel.postId,
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

    private fun archivePost(post: Post) {
        FireUtility.archivePost(post) { newPost, it ->
            if (it.isSuccessful) {

                viewModel.insertPosts(newPost)
                viewModel.insertPostToCache(newPost)

                Snackbar.make(
                    binding.root,
                    "Archived post successfully",
                    Snackbar.LENGTH_LONG
                ).setBehavior(NoSwipeBehavior())
                    .show()
                // notify the other contributors that the post has been archived
                val content = "${post.name} has been archived."
                val notification = Notification.createNotification(
                    content,
                    post.chatChannel
                )
                FireUtility.sendNotificationToChannel(notification) { it1 ->
                    if (it1.isSuccessful) {
                        // updating post locally
                        post.archived = true
                        viewModel.updateLocalPost(post)
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }


    private fun unArchivePost(post: Post) {
        FireUtility.unArchivePost(post) { newPost, it ->
            if (it.isSuccessful) {

                viewModel.insertPosts(newPost)
                viewModel.insertPostToCache(newPost)

                Snackbar.make(
                    binding.root,
                    "Un-archived post successfully",
                    Snackbar.LENGTH_LONG
                ).setBehavior(NoSwipeBehavior()).show()
                post.archived = false
                viewModel.updateLocalPost(post)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun isNetworkConnected(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    override fun onNetworkAvailable() {
        isNetworkAvailable = true
        currentIndefiniteSnackbar?.dismiss()
    }

    override fun onNetworkNotAvailable() {
        isNetworkAvailable = false

        val (bgColor, txtColor) = if (isNightMode()) {
            ContextCompat.getColor(this, R.color.error_color) to Color.WHITE
        } else {
            Color.BLACK to Color.WHITE
        }

        currentIndefiniteSnackbar = Snackbar.make(binding.root, "Network not available", Snackbar.LENGTH_INDEFINITE)
            .setBackgroundTint(bgColor)
            .setBehavior(NoSwipeBehavior())
            .setTextColor(txtColor)

        currentIndefiniteSnackbar?.show()
    }


}
