package com.jamid.codesquare.ui

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.StyleSpan
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import androidx.annotation.AnimatorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
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
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMain2Binding
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.databinding.TopSnackBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
// something simple
class MainActivity : LauncherActivity(), LocationItemClickListener, PostInviteListener,
    PostClickListener, PostRequestListener, UserClickListener, ChatChannelClickListener,
    NotificationItemClickListener, CommentListener, OptionClickListener, NetworkStateListener,
    MediaClickListener {

    var shouldDelay = true
    lateinit var binding: ActivityMain2Binding
    private lateinit var navController: NavController
    private var previouslyFetchedLocation: Location? = null
    private var currentImageViewer: View? = null
    private var animationStartView: View? = null
    private var animationEndView: View? = null
    private var isImageViewMode = false
    private var currentIndefiniteSnackbar: Snackbar? = null
    private lateinit var networkManager: MyNetworkManager
    lateinit var playBillingController: PlayBillingController

    var splashFragment: SplashFragment? = null

    private var justStarted = true

    private var subscriptionFragment: SubscriptionFragment? = null
    var optionsFragment: OptionsFragment? = null
    private var isNetworkAvailable = false

    var currentBottomAnchor: View? = null

    private val authFragments = arrayOf(
        R.id.loginFragment,
        R.id.onBoardingFragment,
        R.id.splashFragment1,
        R.id.createAccountFragment,
        R.id.profileImageFragment,
        R.id.emailVerificationFragment,
        R.id.userInfoFragment,
        R.id.forgotPasswordFragment2
    )
    /*private val bottomBarLessFragments = mutableListOf(
        R.id.commentsFragment,
        R.id.createPostFragment,
        R.id.editProfileFragment,
        R.id.settingsFragment
    ).apply {
        addAll(authFragments)
    }*/

    private val tabbedFragments = arrayOf(
        R.id.searchFragment,
        R.id.notificationCenterFragment,
        R.id.profileFragment,
        R.id.profileFragment2,
        R.id.chatMediaFragment
    )

    private val settingsFragments = arrayOf(
        R.id.settingsFragment,
        R.id.editProfileFragment,
        R.id.savedPostsFragment,
        R.id.testFragment,
        R.id.extraFragment,
        R.id.forgotPasswordFragment,
        R.id.savedPostsFragment,
        R.id.archiveFragment,
        R.id.myRequestsFragment,
        R.id.invitesFragment,
        R.id.blockedAccountsFragment,
        R.id.updatePasswordFragment,
        R.id.feedbackFragment,
        R.id.reportFragment
    )

    private val chatFragments = arrayOf(
        R.id.chatFragment2,
        R.id.chatDetailFragment,
        R.id.chatDetailFragment2,
        R.id.chatMediaFragment,
        R.id.channelGuidelinesFragment,
        R.id.chatContributorsFragment,
        R.id.messageDetailFragment
    )

    private val chatReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1 != null) {
                // checking if the message is sent by current user
                val senderId = p1.getStringExtra(SENDER_ID)
                if (senderId == Firebase.auth.currentUser?.uid)
                    return

                val chatChannelId = p1.getStringExtra(CHANNEL_ID)
                chatChannelId?.let {
                    FireUtility.getChatChannel(chatChannelId) { chatChannel ->
                        if (chatChannel != null) {
                            // check if the current destination is chat container
                            if (binding.mainPrimaryBottom.selectedItemId == R.id.navigation_chats) {
                                return@getChatChannel
                            }

                            if (!chatChannel.authorized)
                                return@getChatChannel

                            val lastMessage = chatChannel.lastMessage
                            if (lastMessage != null) {
                                val content = when (lastMessage.type) {
                                    image -> "Image"
                                    document -> "Document"
                                    video -> "Video"
                                    else -> lastMessage.content
                                }

                                val ss = if (chatChannel.type == CHANNEL_PRIVATE) {
                                    SpannableString("${lastMessage.sender.name}: $content").apply {
                                        setSpan(StyleSpan(Typeface.BOLD), 0, lastMessage.sender.name.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                } else {
                                    SpannableString("${chatChannel.postTitle}\n${lastMessage.sender.name}: $content").apply {
                                        setSpan(StyleSpan(Typeface.BOLD), 0, chatChannel.postTitle.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                }
                                showTopSnack(ss, lastMessage.sender.photo, action = {
                                    binding.mainPrimaryBottom.selectedItemId = R.id.navigation_chats
                                    runDelayed(400) {
                                        onChannelClick(chatChannel, 0)
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }


    /*fun stringForTime(timeMs: Float): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }*/


    /* Notifications that arrive when the app is in foreground*/
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
                            is Result.Error -> Log.e(
                                TAG,
                                "onReceive: ${notificationResult.exception.localizedMessage}"
                            )
                            is Result.Success -> {
                                viewModel.insertNotifications(notificationResult.data)
                                showTopSnack(notificationResult.data.content, notificationResult.data.image) {
                                    binding.mainPrimaryBottom.selectedItemId = R.id.navigation_notifications
                                    runDelayed(500) {
                                        findViewById<ViewPager2>(R.id.notification_pager)?.setCurrentItem(notificationResult.data.type, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

    var initialNavColor = Color.BLACK

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        initialNavColor = window.navigationBarColor

        // initialize all nodes
        MobileAds.initialize(this) {}
//        ExoPlayerProvider.initialize(this)

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

        UserManager.currentUserLive.observe(this) {
            if (it != null) {
                setMessagesListener(it.chatChannels)
                viewModel.onUserUpdate(it)
            }
        }

        UserManager.authState.observe(this) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                    /*if (UserManager.isEmailVerified) {
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

//        tempTest()


        viewModel.allUnreadNotifications.observe(this) { un ->
            if (!un.isNullOrEmpty()) {
                binding.mainPrimaryBottom.getOrCreateBadge(R.id.navigation_notifications).let {
                    it.number = un.size
                    it.backgroundColor = ContextCompat.getColor(this, R.color.pink)
                    it.badgeTextColor = Color.WHITE
                }
            } else {
                binding.mainPrimaryBottom.removeBadge(R.id.navigation_notifications)
            }
        }

        viewModel.getUnreadChatChannels().observe(this) { uc ->
            if (!uc.isNullOrEmpty()) {
                binding.mainPrimaryBottom.getOrCreateBadge(R.id.navigation_chats).let {
                    it.number = uc.size
                    it.backgroundColor = ContextCompat.getColor(this, R.color.pink)
                    it.badgeTextColor = Color.WHITE
                }
            } else {
                binding.mainPrimaryBottom.removeBadge(R.id.navigation_chats)
            }
        }

    }

    private fun setBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(chatReceiver, IntentFilter("chat_receiver"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(notificationReceiver, IntentFilter("notification_receiver"))
    }

    private fun prefetchProfileImages() = lifecycleScope.launch(Dispatchers.IO) {
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
                .limit(5)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, "setMessagesListener: ${error.localizedMessage}")
                    }

                    if (value != null && !value.isEmpty) {
                        val messages = value.toObjects(Message::class.java)
                        viewModel.chatRepository.insertChannelMessages(messages)
                    }

                }
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        val primary = setOf(
            R.id.splashFragment,
            R.id.onBoardingFragment,
            R.id.loginFragment,
            R.id.feedFragment,
            R.id.chatListFragment2,
            R.id.rankedFragment,
            R.id.notificationCenterFragment,
            R.id.profileFragment
        )

        val appBarConfiguration = AppBarConfiguration(
            primary
        )

        // very important in case of fullscreen
        binding.mainPrimaryBottom.setOnApplyWindowInsetsListener(null)

        binding.mainPrimaryBottom.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id == R.id.feedFragment) {
                        binding.root.findViewById<RecyclerView>(R.id.pager_items_recycler)
                            ?.smoothScrollToPosition(0)
                    } else {
                        navController.popBackStack(R.id.feedFragment, false)
                    }
                }
                R.id.navigation_profile -> {
                    if (navController.currentDestination?.id == R.id.profileFragment) {
                        findViewById<AppBarLayout>(R.id.profile_app_bar)?.setExpanded(false, true)
                    } else {
                        navController.popBackStack(R.id.profileFragment, false)
                    }
                }
            }
        }

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.mainPrimaryBottom, navController)

        layoutChangesOnStart()

        val logo = if (isNightMode()) {
            R.drawable.ic_logo_xy_night
        } else {
            R.drawable.ic_logo_xy
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->

            invalidateDefaultFragmentChanges()

            when (val frag = destination.id) {
                in primary -> {
                    when (frag) {
                        R.id.feedFragment -> {
                            updateUi(
                                UiConfig.ToolbarUiConfig(
                                    title = "",
                                    logo = logo
                                ),
                                UiConfig.PrimaryActionUiConfig(
                                    isVisible = true
                                ) {
                                    val filterFrag = FilterFragment()
                                    filterFrag.show(supportFragmentManager, FilterFragment.TAG)
                                }
                            )

                        }
                        in authFragments -> {
                            updateUi(
                                UiConfig.AppBarUiConfig(
                                    isVisible = false
                                ), UiConfig.ContainerUiConfig(
                                    behavior = null
                                ), UiConfig.BottomNavUiConfig(
                                    isVisible = false
                                )
                            )
                        }
                        in tabbedFragments -> {
                            updateUi(
                                UiConfig.AppBarUiConfig(
                                    elevationState = R.animator.app_bar_elevation_reverse,
                                )
                            )
                        }
                        else -> {
                            updateUi()
                        }
                    }
                }
                in authFragments -> {
                    when (frag) {
                        R.id.loginFragment, R.id.createAccountFragment, R.id.emailVerificationFragment, R.id.profileImageFragment, R.id.userInfoFragment -> {
                            updateUi(
                                UiConfig.AppBarUiConfig(
                                    isVisible = false
                                ), UiConfig.ContainerUiConfig(
                                    behavior = null
                                ), UiConfig.BottomNavUiConfig(
                                    isVisible = false
                                )
                            )
                        }
                        R.id.forgotPasswordFragment2, R.id.updatePasswordFragment2 -> {
                            updateUi(
                                UiConfig.BottomNavUiConfig(
                                    isVisible = false
                                )
                            )
                        }
                    }
                }
                R.id.imageViewFragment -> {
                    updateUi(
                        UiConfig.ContainerUiConfig(
                            behavior = null
                        )
                    )
                }
                in tabbedFragments -> {
                    updateUi(
                        UiConfig.AppBarUiConfig(
                            elevationState = R.animator.app_bar_elevation_reverse,
                        )
                    )

                    if (frag in chatFragments) {
                        updateUi(
                            UiConfig.BottomNavUiConfig(
                                isVisible = false
                            )
                        )
                    }

                    if (frag == R.id.searchFragment) {
                        updateUi(
                            UiConfig.ToolbarUiConfig(
                                isTitleCentered = false
                            ),
                            UiConfig.BottomNavUiConfig(
                                isVisible = false
                            )
                        )
                    }

                }
                R.id.commentsFragment, R.id.preSearchFragment, R.id.createPostFragment, in settingsFragments, in chatFragments -> {
                    updateUi(
                        UiConfig.BottomNavUiConfig(
                            isVisible = false
                        )
                    )

                    when (frag) {
                        R.id.commentsFragment, R.id.createPostFragment, R.id.chatFragment2 -> {
                            updateUi(UiConfig.KeyboardUiConfig(true))
                        }
                    }
                }
                R.id.mediaViewerFragment -> {
                    updateUi(
                        UiConfig.BottomNavUiConfig(false),
                        UiConfig.AppBarUiConfig(R.animator.app_bar_elevation_reverse, false),
                        UiConfig.ContainerUiConfig(null)
                    )
                }
                else -> updateUi()
            }
        }


        // just adding a fragment to show for the first time
        if (justStarted) {
            justStarted = false

            splashFragment = SplashFragment()

            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, splashFragment!!, "SplashFragment")
                .commit()

        }

    }

    private fun layoutChangesOnStart() {
        updateUi(
            UiConfig.AppBarUiConfig(
                isVisible = false
            ), UiConfig.ContainerUiConfig(
                behavior = null
            ), UiConfig.BottomNavUiConfig(
                isVisible = false
            )
        )
    }

    private fun invalidateDefaultFragmentChanges() {
        invalidateOptionsMenu()
        setDefaultUiConfig()

        currentBottomAnchor = null

        binding.mainToolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }

        binding.mainToolbar.setOnClickListener {
            //
        }

        binding.mainProgressBar.hide()

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
                            if (it != null) {
                                onChannelClick(it, 0)
                            }
                        }
                    }
                }
                extras.containsKey(NOTIFICATION_ID) -> {
                    if (extras.containsKey(TYPE)) {
                        val type = extras[TYPE] as String? ?: return
                        val ch = type.first()
                        val pos = ch.digitToInt()

                        binding.mainPrimaryBottom.selectedItemId = R.id.navigation_notifications

                        runDelayed(500) {
                            findViewById<ViewPager2>(R.id.notification_pager)?.setCurrentItem(pos, false)
                        }
                    }
                }
                else -> {
                    // nothing
                }
            }
        }
    }

    fun runDelayed(duration: Long, scope: () -> Unit) {
        lifecycleScope.launch {
            delay(duration)
            runOnUiThread{
                scope()
            }
        }
    }

    sealed class UiConfig {
        data class ToolbarUiConfig(
            val title: String? = null,
            val subtitle: String? = null,
            @DrawableRes val logo: Int? = null,
            val isTitleCentered: Boolean = true
        ) : UiConfig()

        data class BottomNavUiConfig(
            val isVisible: Boolean = true
        ) : UiConfig()

        data class AppBarUiConfig(
            @AnimatorRes val elevationState: Int = R.animator.app_bar_elevation,
            val isVisible: Boolean = true
        ) : UiConfig()

        data class PrimaryActionUiConfig(
            val isVisible: Boolean = false,
            val action: (() -> Unit)? = null
        ) : UiConfig()

        data class KeyboardUiConfig(
            val isVisible: Boolean = false
        ) : UiConfig()

        data class ContainerUiConfig(
            val behavior: CoordinatorLayout.Behavior<View>? = AppBarLayout.ScrollingViewBehavior()
        ) : UiConfig()
    }

    private fun setDefaultUiConfig() {
        updateUi(
            UiConfig.AppBarUiConfig(),
            UiConfig.BottomNavUiConfig(),
            UiConfig.ContainerUiConfig(),
            UiConfig.KeyboardUiConfig(),
            UiConfig.PrimaryActionUiConfig(),
            UiConfig.ToolbarUiConfig()
        )
    }

    private fun updateUi(vararg uiConfigs: UiConfig) {

        for (change in uiConfigs) {
            when (change) {
                is UiConfig.AppBarUiConfig -> {
                    binding.mainAppbar.isVisible = change.isVisible
                    binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(
                        this,
                        change.elevationState
                    )
                }
                is UiConfig.BottomNavUiConfig -> {
                    lifecycleScope.launch {
                        delay(300)
                        runOnUiThread {
                            if (change.isVisible) {
                                binding.mainPrimaryBottom.slideReset()
                            } else {
                                val dy = resources.getDimension(R.dimen.appbar_slide_translation)
                                binding.mainPrimaryBottom.slideDown(dy)
                            }
                        }
                    }
                }
                is UiConfig.ContainerUiConfig -> {
                    binding.navHostFragment.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                        behavior = change.behavior
                    }
                }
                is UiConfig.KeyboardUiConfig -> {
                    if (change.isVisible) {
                        showKeyboard()
                    } else {
                        hideKeyboard(binding.root)
                    }
                }
                is UiConfig.PrimaryActionUiConfig -> {
                    binding.mainActionBtn.isVisible = change.isVisible
                    binding.mainActionBtn.setOnClickListener {
                        change.action?.let { it1 -> it1() }
                    }
                }
                is UiConfig.ToolbarUiConfig -> {
                    if (!change.title.isNullOrBlank()) {
                        binding.mainToolbar.title = change.title
                    }

                    binding.mainToolbar.isTitleCentered = change.isTitleCentered

                    binding.mainToolbar.subtitle = change.subtitle
                    binding.mainToolbar.logo = change.logo?.let { getImageResource(it) }
                }
            }
        }
    }

    /*private fun updateUi(
        shouldShowAppBar: Boolean = true,
        shouldShowToolbar: Boolean = true,
        shouldShowTabLayout: Boolean = false,
        baseFragmentBehavior: CoordinatorLayout.Behavior<View>? = AppBarLayout.ScrollingViewBehavior(),
        toolbarAdjustment: ToolbarAdjustment = ToolbarAdjustment()
    ) = runOnUiThread {

        binding.mainAppbar.isVisible = shouldShowAppBar
        binding.mainToolbar.isVisible = shouldShowToolbar


        if (shouldShowAppBar) {
            if (binding.mainAppbar.translationY != 0f) {
                binding.mainAppbar.slideReset()
            }
            if (shouldShowToolbar) {
                binding.mainToolbar.apply {

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

    }*/


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
        viewModel.insertPost(post)
        navController.navigate(R.id.postFragment2, bundle)
    }

    override fun onPostClick(postMinimal2: PostMinimal2) {
        FireUtility.getPost(postMinimal2.objectID) {
            it?.let {
                onPostClick(it)
            }
        }
    }

    override fun onPostLikeClick(post: Post) {

        val currentUser = UserManager.currentUser

        if (post.isLiked) {
            FireUtility.dislikePost2(post) {
                if (it.isSuccessful) {

                    Log.d(TAG, "onPostLikeClick: Disliked a post")

                    viewModel.insertPost(post)
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
            FireUtility.likePost2(post) {
                if (it.isSuccessful) {

                    Log.d(TAG, "onPostLikeClick: Liked a post")

                    viewModel.insertPost(post)
                    sendNotification(notification)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onPostSaveClick(post: Post) {
        if (post.isSaved) {
            FireUtility.unsavePost(post) {
                if (it.isSuccessful) {
                    viewModel.insertPost(post.apply { isSaved = false })
                }
            }
        } else {
            FireUtility.savePost(post) {
                if (it.isSuccessful) {
                    viewModel.insertPost(post.apply { isSaved = true })
                    showSnackMessage("Saved this post", "View all") {
                        navController.navigate(
                            R.id.savedPostsFragment
                        )
                    }
                }
            }
        }
    }

    private fun showSnackMessage(
        message: String,
        label: String? = null,
        action: View.OnClickListener? = null
    ) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)

        if (binding.mainPrimaryBottom.isVisible) {
            snackbar.anchorView = binding.mainPrimaryBottom
        }

        if (label != null) {
            snackbar.setAction(label, action)
        }

        snackbar.show()
    }

    private fun setLiveDataObservers() {
        /*playBillingController.premiumState.observe(this) {
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
        }*/

        playBillingController.purchaseDetails.observe(this) { p1 ->
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

    override fun onPostJoinClick(post: Post) {
        if (post.isRequested) {
            // undo request
            FireUtility.getPostRequest(post.id, UserManager.currentUserId) {
                val postRequestResult = it ?: return@getPostRequest

                when (postRequestResult) {
                    is Result.Error -> Log.e(
                        TAG,
                        "onPostUndoClick: ${postRequestResult.exception.localizedMessage}"
                    )
                    is Result.Success -> {
                        val postRequest = postRequestResult.data
                        FireUtility.undoJoinPost(postRequest) { it1 ->
                            if (it1.isSuccessful) {
                                post.isRequested = false
                                val newList = post.requests.removeItemFromList(postRequest.requestId)
                                post.requests = newList
                                viewModel.insertPost(post)
                                viewModel.deletePostRequest(postRequest)
                            } else {
                                viewModel.setCurrentError(it1.exception)
                            }
                        }
                    }
                }
            }
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

                viewModel.insertPost(post)
                viewModel.insertPostRequests(postRequest)
            }

            FireUtility.joinPost(notification.id, post) { task, postRequest ->
                if (task.isSuccessful) {
                    showSnackMessage("Post request sent")
                    onRequestSent(postRequest)
                    sendNotification(notification)
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

        processPost(post)

        viewModel.insertPost(post)
    }


    override fun onPostRequestAccept(postRequest: PostRequest, onFailure: () -> Unit) {
        val currentUser = UserManager.currentUser

        FireUtility.getPost(postRequest.postId) { post ->
            post?.let {
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

                        sendNotification(notification)

                    } else {
                        Log.e(
                            TAG,
                            "onPostRequestAccept: ${it1.exception?.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    private fun getNewContributorOnRequestAccept(userId: String) {
        FireUtility.getUser(userId) {
            if (it != null) {
                viewModel.insertUser(it)
            }
        }
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

                sendNotification(notification)
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

    override fun onAdInfoClick() {
        navController.navigate(R.id.advertiseInfoFragment)
    }

    override fun onAdError(post: Post) {
        viewModel.deleteLocalPost(post)
    }

    override fun onPostLocationClick(post: Post) {
        navController.navigate(
            R.id.locationPostsFragment,
            bundleOf(
                TITLE to "Showing posts near",
                SUB_TITLE to post.location.address,
                "location" to post.location
            )
        )
    }

    /*@Suppress("LABEL_NAME_CLASH")
    override suspend fun onCheckForStaleData(post: Post, onUpdate: (newPost: Post) -> Unit) {

        fun onChangeNeeded(creator: User) {
            val changes =
                mapOf(CREATOR to creator.minify(), UPDATED_AT to System.currentTimeMillis())

            FireUtility.updatePost(post.id, changes) { it1 ->
                if (it1.isSuccessful) {
                    post.creator = creator.minify()

                    runOnUiThread {
                        onUpdate(post)
                    }

                    viewModel.insertPost(post)
                } else {
                    Log.e(
                        TAG,
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
            FireUtility.getUser(post.creator.userId) { creator ->
                if (creator != null && creator.minify() != post.creator) {
                    onChangeNeeded(creator)
                }
            }
            *//*getUserImpulsive(post.creator.userId) { creator ->
                if (creator.minify() != post.creator) {
                    onChangeNeeded(creator)
                }
            }*//*
        }
    }*/

    override fun onPostUpdate(newPost: Post) {
        viewModel.insertPost(newPost)
    }

    /*fun getPostImpulsive(postId: String, onPostFetch: (Post) -> Unit) {
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
    }*/

    /*fun getUserImpulsive(userId: String, onUserFound: (User) -> Unit) {
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
    }*/

    /*fun getPost(postId: String, onFetch: (Post) -> Unit) {
        FireUtility.getPost(postId) { post ->

        } child@{
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
    }*/

    /*fun getUser(userId: String, onFetch: (User) -> Unit) {
        FireUtility.getUser(userId) child@{
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
    }*/


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
                FireUtility.getPost(postRequest.postId) { post ->
                    post?.let {
                        val requestsList = post.requests.removeItemFromList(postRequest.requestId)
                        post.requests = requestsList
                        post.isRequested = false
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onPostRequestClick(postRequest: PostRequest) {
        FireUtility.getUser(postRequest.senderId) { user ->
            if (user != null) {
                onUserClick(user)
            }
        }
    }


    override fun onCheckForStaleData(postRequest: PostRequest) {

        fun updatePostRequest(changes: Map<String, Any?>) {
            FireUtility.updatePostRequest(
                postRequest.requestId,
                changes
            ) { postRequestUpdateResult ->
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
            val changes =
                mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            postRequest.sender = user.minify()
            postRequest.updatedAt = System.currentTimeMillis()
            updatePostRequest(changes)
        }

        FireUtility.getPost(postRequest.postId) { post ->
            if (post != null && post.updatedAt > postRequest.updatedAt) {
                onChangeNeeded(post)
            }
        }

        FireUtility.getUser(postRequest.senderId) { user ->
            if (user != null && user.minify() != postRequest.sender) {
                onChangeNeeded(user)
            }
        }

    }

    override fun onPostCreatorClick(post: Post) {
        if (post.creator.userId == UserManager.currentUserId) {
            navController.navigate(
                R.id.profileFragment, null
            )
        } else {
            FireUtility.getUser(post.creator.userId) { user ->
                if (user != null) {
                    onUserClick(user)
                }
            }
            /*getUserImpulsive(post.creator.userId) {
                onUserClick(it)
            }*/
        }
    }


    override fun onPostCommentClick(post: Post) {
        val bundle = bundleOf(
            COMMENT_CHANNEL_ID to post.commentChannel,
            PARENT to post,
            TITLE to post.name,
            POST to post
        )

        navController.navigate(R.id.commentsFragment, bundle)
    }

    override fun onPostOptionClick(post: Post, creator: User) {
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
            arrayListOf(
                saveUnSaveText,
                archiveToggleText,
                option7
            ) to arrayListOf(
                if (post.isSaved) {
                    R.drawable.ic_round_bookmark_remove_24
                } else {
                    R.drawable.ic_round_bookmark_add_24
                }, R.drawable.ic_round_archive_24, R.drawable.ic_round_edit_note_24
            )
        } else {
            arrayListOf(
                option1,
                saveUnSaveText,
                option6
            ) to arrayListOf(
                R.drawable.ic_round_account_circle_24, if (post.isSaved) {
                    R.drawable.ic_round_bookmark_remove_24
                } else {
                    R.drawable.ic_round_bookmark_add_24
                }, R.drawable.ic_round_report_24
            )
        }

        optionsFragment = OptionsFragment.newInstance(
            post.name,
            choices,
            icons,
            post = post,
            user = creator
        ).apply {
            fullscreen = false
        }
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)
    }

    override fun onPostOptionClick(postMinimal2: PostMinimal2, creator: User) {
        FireUtility.getPost(postMinimal2.objectID) { post ->
            if (post != null) {
                onPostOptionClick(post, creator)
            }
        }
    }

    override fun onPostContributorsClick(post: Post) {

        val bundle = bundleOf(
            POST to post,
            TITLE to CONTRIBUTORS.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            SUB_TITLE to post.name
        )

        navController.navigate(
            R.id.postContributorsFragment,
            bundle
        )
    }

    override fun onPostSupportersClick(post: Post) {
        val bundle = bundleOf(POST_ID to post.id)
        navController.navigate(R.id.postLikesFragment, bundle)
    }

    override fun onPostNotFound(post: Post) {

    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }

        if (user.isCurrentUser) {

            /*navController.navigate(R.id.profileFragment, bundle)*/
        } else {
            if (user.blockedUsers.contains(UserManager.currentUserId)) {
                val frag = MessageDialogFragment.builder("You have been blocked by ${user.name}. You cannot access ${user.name}'s profile.")
                    .setTitle("Blocked")
                    .setPositiveButton("Done") { _, _ ->
                        //
                    }
                    .build()

                frag.show(supportFragmentManager, "BlockedUserFrag")
            } else {
                navController.navigate(R.id.profileFragment2, bundle)
            }
        }

    }

    override fun onUserClick(userId: String) {
        FireUtility.getUser(userId) { user ->
            if (user != null) {
                onUserClick(user)
            }
        }
    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        FireUtility.getUser(userMinimal.objectID) { user ->
            if (user != null) {
                onUserClick(user)
            }
        }
    }

    override fun onUserOptionClick(user: User) {

    }

    override fun onUserOptionClick(userMinimal: UserMinimal2) {
        FireUtility.getUser(userMinimal.objectID) {
            if (it != null) {
                onUserOptionClick(it)
            }
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

    private fun likeUser(userMinimal: UserMinimal) {
        val currentUser = UserManager.currentUser
        likeUser2(userMinimal) {
            if (it.isSuccessful) {
                viewModel.likeLocalUserById(userMinimal.userId)

                val content = currentUser.name + " liked your profile"
                val notification = Notification.createNotification(
                    content,
                    userMinimal.userId,
                    userId = userMinimal.userId,
                    title = currentUser.name
                )

                sendNotification(notification)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onUserLikeClick(user: User) {
        if (user.isLiked) {
            dislikeUser2(user.id) {
                showSnackMessage("Disliked ${user.name}")
            }
        } else {
            likeUser2(user.minify()) {
                showSnackMessage("Liked ${user.name}")
            }
        }
    }

    override fun onUserLikeClick(userId: String) {
        FireUtility.getUser(userId) { user ->
            if (user != null) {
                onUserLikeClick(user)
            }
        }
    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {
        FireUtility.getUser(userMinimal.objectID) { user ->
            if (user != null) {
                onUserLikeClick(user)
            }
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
            R.id.reportFragment, bundle
        )
    }

    override fun onCommentUserClick(userMinimal: UserMinimal) {
        FireUtility.getUser(userMinimal.userId) {
            if (it != null) {
                onUserClick(it)
            }
        }
    }

    override fun onOptionClick(comment: Comment) {
        val isCommentByMe = comment.senderId == UserManager.currentUserId
        val name: String

        val (choices, icons) = if (isCommentByMe) {
            name = "You"
            arrayListOf(OPTION_29, OPTION_30) to arrayListOf(
                R.drawable.ic_round_report_24,
                R.drawable.ic_round_delete_24
            )
        } else {
            name = comment.sender.name
            arrayListOf(OPTION_29) to arrayListOf(R.drawable.ic_round_report_24)
        }

        optionsFragment = OptionsFragment.newInstance(
            title = "Comment by $name",
            options = choices,
            icons = icons,
            comment = comment
        )
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    /*override fun onCheckForStaleData(comment: Comment, onUpdate: (newComment: Comment) -> Unit) {

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

        FireUtility.getUser(comment.senderId) { user ->
            if (user != null && user.minify() != comment.sender) {
                onChangeNeeded(user)
            }
        }

    }*/

    override fun onCommentInfoClick(comment: Comment) {
        val bundle = bundleOf(COMMENT to comment)
        navController.navigate(R.id.commentLikesFragment, bundle)
    }


    /*override fun onBackPressed() {
        if (isImageViewMode) {
            Log.d(TAG, "onBackPressed: non super")
            removeImageViewFragment()
        } else {
            Log.d(TAG, "onBackPressed: super")
            super.onBackPressed()
        }
    }*/

    fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.show(WindowInsetsCompat.Type.systemBars())
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


    fun showMediaFragment(mediaList: List<MediaItem>, currentPosition: Int = 0) {
//        val transaction = supportFragmentManager.beginTransaction()

        val arrayList = arrayListOf<MediaItem>()
        arrayList.addAll(mediaList)

        navController.navigate(R.id.mediaViewerFragment, bundleOf("list" to arrayList, "current_position" to currentPosition))
        /*
        val fragment = MediaViewerFragment()
        fragment.arguments = bundleOf("list" to arrayList, "current_position" to currentPosition)
        transaction.add(android.R.id.content, fragment, "MediaViewerFragment")
            .addToBackStack("MediaViewerFragment")
            .commit()*/
    }

    private fun showMediaFragment(message: Message) {

        if (message.type == video || message.type == image) {
            val mediaList = listOf(MediaItem().apply {
                url = message.metadata!!.url
                type = message.type
            })

            showMediaFragment(mediaList)
        }

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
                Log.d(TAG, "onNotificationRead: ${error.localizedMessage}")
                viewModel.setCurrentError(error)
                return@checkIfNotificationExistsById
            }

            if (!exists) {
                viewModel.deleteNotification(notification)

                Log.d(TAG, "onNotificationRead: It doesn't exist")
            } else {

                Log.d(TAG, "onNotificationRead: It exists")

                if (!notification.read) {
                    Log.d(TAG, "onNotificationRead: But not read")

                    FireUtility.updateNotification(notification) {
                        if (it.isSuccessful) {
                            notification.read = true
                            Log.d(TAG, "onNotificationRead: Successfully updated notification")
                            viewModel.updateNotification(notification)
                        } else {
                            Log.d(TAG, "onNotificationRead: ${it.exception?.localizedMessage}")
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
            FireUtility.getUser(userId) { user ->
                if (user != null) {
                    onUserClick(user)
                }
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have user Id")
        }

        val postId = notification.postId
        if (postId != null) {
            FireUtility.getPost(postId) { post ->
                post?.let {
                    onPostClick(post)
                }
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

                            FireUtility.getPost(comment.postId) { post ->
                                if (post != null) {
                                    onPostCommentClick(post)
                                }
                            }
                        } else {
                            onClick(comment)
                        }
                    }
                    null -> {
                        Log.w(
                            TAG,
                            "Something went wrong while trying to get comment with comment id: $commentId"
                        )
                    }
                }
            }
        } else {
            Log.i(
                TAG,
                "Notification with notification id: ${notification.id} doesn't have comment Id"
            )
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

            val changes =
                mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
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

        FireUtility.getUser(notification.senderId) { user ->
            if (user != null && user.minify() != notification.sender) {
                onChangeNeeded(user)
            }
        }
        /*getUserImpulsive(notification.senderId) { user ->
            if (user.minify() != notification.sender) {
                onChangeNeeded(user)
            }
        }*/

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

            sendNotification(notification)

        }

        FireUtility.acceptPostInvite(currentUser, postInvite) {
            if (it.isSuccessful) {

                // current user was added to post successfully
                viewModel.deletePostInvite(postInvite)
                viewModel.deleteNotificationById(postInvite.notificationId)

                // extra
                FireUtility.getUser(postInvite.senderId) { user ->
                    if (user != null) {
                        onPostAccept(user)
                    }
                }
            } else {
                // something went wrong while trying to add the current user to the post
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun sendNotification(notification: Notification) {
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
                        FireUtility.updateNotification(
                            notification.receiverId,
                            notification.id,
                            mapOf("read" to false, UPDATED_AT to System.currentTimeMillis())
                        ) { it1 ->
                            if (!it1.isSuccessful) {
                                Log.e(
                                    TAG,
                                    "sendNotification: ${it1.exception?.localizedMessage}"
                                )
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

            sendNotification(notification)
        }

        FireUtility.cancelPostInvite(postInvite) {
            if (it.isSuccessful) {
                // current user was added to post successfully
                viewModel.deletePostInvite(postInvite)
                viewModel.deleteNotificationById(postInvite.notificationId)


                FireUtility.getUser(postInvite.senderId) { user ->
                    if (user != null) {
                        onPostCancel(user)
                    }
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

                FireUtility.getUser(postInvite.senderId) { user ->
                    if (user != null) {
                        onPostDelete(user)
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(postInvite: PostInvite) {

        fun updatePostInvite(changes: Map<String, Any?>) {
            FireUtility.updatePostInvite(
                postInvite.receiverId,
                postInvite.id,
                changes
            ) { postInviteUpdateResult ->
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
            val changes =
                mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            postInvite.sender = user.minify()
            postInvite.updatedAt = System.currentTimeMillis()
            updatePostInvite(changes)
        }

        FireUtility.getPost(postInvite.postId) { post ->
            if (post != null) {
                if (post.updatedAt > postInvite.updatedAt) {
                    onChangeNeeded(post)
                }
            }
        }

        FireUtility.getUser(postInvite.senderId) { user ->
            if (user != null && user.minify() != postInvite.sender) {
                onChangeNeeded(user)
            }
        }

    }


//    fun selectMedia() {
//        val intent = Intent()
//        intent.type = "*/*"
//        intent.action = Intent.ACTION_GET_CONTENT
//
//        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png", "video/mp4")
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        sml.launch(intent)
//    }

//    fun selectImage(type: ImageSelectType) {
//        imageSelectType = type
//
//        val intent = Intent()
//        intent.type = "image/*"
//        intent.action = Intent.ACTION_GET_CONTENT
//
//        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png")
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
//
//        when (type) {
//            IMAGE_PROFILE, IMAGE_TEST -> {
//                sil.launch(intent)
//            }
//            IMAGE_CHAT, IMAGE_POST, IMAGE_REPORT -> {
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//                sil.launch(intent)
//            }
//        }
//    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        post: Post?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?,
        message: Message?
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
                            showSnackMessage("Successfully set ${user.name} as admin")
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
                    likeUser(user.minify())
                    showSnackMessage("Liked ${user.name}")
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

                    )

                }

            }
            OPTION_6 -> {
                if (chatChannel != null && user != null) {
                    val frag =
                        MessageDialogFragment.builder("Are you sure you want to remove ${user.name} from the post?")
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

                            FireUtility.getPost(chatChannel.postId) { post ->
                                if (post != null) {
                                    val contributors =
                                        post.contributors.removeItemFromList(
                                            UserManager.currentUserId
                                        )
                                    post.contributors =
                                        contributors
                                    viewModel.insertPost(post)
                                }
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
                /*if (chatChannel != null) {
                    *//*navController.navigate(
                        R.id.gallerySelectorFragment,
                        bundleOf("chatChannelId" to chatChannel.chatChannelId),

                    )*//*
                }*/
            }
            OPTION_9 -> {

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
                    val frag =
                        MessageDialogFragment.builder("Are you sure you want to archive this post?")
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
                if (post != null && post.archived) {
                    val frag =
                        MessageDialogFragment.builder("Are you sure you want to un-archive this post?")
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
                val report = when {
                    post != null -> {
                        Report.getReportForPost(post)
                    }
                    user != null -> {
                        Report.getReportForUser(user)
                    }
                    else -> {
                        return
                    }
                }
                val bundle = bundleOf(REPORT to report)
                navController.navigate(R.id.reportFragment, bundle)

            }
            OPTION_15 -> {
                val bundle = bundleOf(PREVIOUS_POST to post)
                navController.navigate(R.id.createPostFragment, bundle)
            }
            OPTION_16 -> {
                navController.navigate(
                    R.id.postFragmentContainer,
                    bundleOf(POST to post),

                )
            }
            /*OPTION_17 -> {
                selectImage(IMAGE_PROFILE)
            }*/
            OPTION_18 -> {
                viewModel.setCurrentImage(null)
            }
            OPTION_19, OPTION_20, OPTION_21, OPTION_22 -> {

            }
            OPTION_23 -> {
                // log out
                UserManager.logOut(this) {
                    navController.navigate(R.id.loginFragment, null)
                    viewModel.signOut {}
                }
            }
            OPTION_24 -> {
                // saved pr
                navController.navigate(R.id.savedPostsFragment, null)
            }
            OPTION_25 -> {
                // archive
                navController.navigate(R.id.archiveFragment, null)
            }
            OPTION_26 -> {
                // requests
                navController.navigate(R.id.myRequestsFragment, null)
            }
            OPTION_27 -> {
                // settings
                navController.navigate(R.id.settingsFragment, null)
            }
            OPTION_28 -> {
                if (tag != null) {
                    val changes = mapOf(INTERESTS to FieldValue.arrayUnion(tag))
                    FireUtility.updateUser2(changes) {
                        if (it.isSuccessful) {
                            showSnackMessage("Added $tag to your interests")
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
                navController.navigate(R.id.invitesFragment, null)
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

//    private fun selectChatVideos() {
//        val intent = Intent().apply {
//            type = "video/*"
//            val mimeTypes = arrayOf("video/mp4")
//            action = Intent.ACTION_GET_CONTENT
//            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        }
//        chatVideosLauncher.launch(intent)
//    }

    /*fun setPrimaryBtn(btn: ExtendedFloatingActionButton, @IntRange(from = 0, to = 2) fragmentPos: Int, shouldShowEnterButton: Boolean = true) {

        if (navController.currentDestination?.id != R.id.homeFragment) {
            Log.d(com.jamid.codesquare.TAG, "setPrimaryBtn: Current destination is not HomeFragment")
            return
        }

        if (!initialLoadWaitFinished) {
            Log.d(com.jamid.codesquare.TAG, "setPrimaryBtn: Initial load has not finished")
            return
        }

        Log.d(com.jamid.codesquare.TAG, "setPrimaryBtn: Setting primary btn $fragmentPos")

        when (fragmentPos) {
            0 -> {
                btn.show()
                btn.extend()
                btn.text = "Filter"
                btn.icon = getImageResource(R.drawable.ic_round_filter_list_24)
                btn.setOnClickListener {
                    val frag = FilterFragment()
                    frag.show(supportFragmentManager, FilterFragment.TAG)
                }
            }
            1 -> {
                btn.hide()
                btn.text = ""
                btn.icon = null
                btn.setOnClickListener {}
            }
            2 -> {
                if (shouldShowEnterButton) {
                    btn.show()
                    btn.extend()
                    btn.text = "Enter"
                    btn.icon = getImageResource(R.drawable.ic_round_login_24)
                    btn.setOnClickListener {
                        navController.navigate(R.id.rankedEntryFragment, null)
                    }
                } else {
                    btn.hide()
                }
            }
        }
    }*/

    private fun removeUserFromPost(user: User, chatChannel: ChatChannel) {
        FireUtility.removeUserFromPost(
            user,
            chatChannel.postId,
            chatChannel.chatChannelId
        ) {
            if (it.isSuccessful) {

                showSnackMessage("Removed ${user.name} from post")

                val content =
                    "${user.name} has left the post"
                val notification =
                    Notification.createNotification(
                        content,
                        chatChannel.chatChannelId
                    )

                FireUtility.getPost(chatChannel.postId) { post ->
                    if (post != null) {
                        val contributors = post.contributors.removeItemFromList(user.id)
                        post.contributors = contributors
                        viewModel.insertPosts(post)
                    }
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

                showSnackMessage("Archived post successfully")

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
                        viewModel.insertPost(post)
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

                showSnackMessage("Un-archived post successfully")

                post.archived = false
                viewModel.insertPost(post)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    @SuppressLint("MissingPermission")
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

        /* val (bgColor, txtColor) = if (isNightMode()) {
             ContextCompat.getColor(this, R.color.error_color) to Color.WHITE
         } else {
             Color.BLACK to Color.WHITE
         }*/

        showSnackMessage("Network not available")

        /* currentIndefiniteSnackbar = Snackbar.make(binding.root, "Network not available", Snackbar.LENGTH_INDEFINITE)
             .setBackgroundTint(bgColor)
             .setTextColor(txtColor)

         currentIndefiniteSnackbar?.show()*/
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {
        showMediaFragment(mediaItems, currentPos)
    }

    override fun onMediaMessageItemClick(message: Message) {
        showMediaFragment(message)
    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

    }

    fun selectMediaItems(
        isMultipleAllowed: Boolean = true,
        type: String,
        mimeTypes: Array<String>?
    ) {
        val intent = Intent()
        intent.type = type
        intent.action = Intent.ACTION_GET_CONTENT

        if (mimeTypes != null) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultipleAllowed)
        gallerySelectLauncher.launch(intent)
    }

    private fun block(user: User) {
        FireUtility.blockUser(user) {

            if (it.isSuccessful) {
                // delete all posts that belong to that user
                // delete all comments that belong to that user
                viewModel.deleteCommentsByUserId(user.id)
                viewModel.deletePostsByUserId(user.id)
                viewModel.deletePreviousSearchByUserId(user.id)

                val frag = MessageDialogFragment.builder("${user.name} is blocked. A blocked user cannot see your profile. They cannot see your work. To unblock any blocked users, go to, Settings-Blocked accounts.")
                    .setTitle("This user is blocked.")
                    .setPositiveButton("Done") { a, _ ->
                        a.dismiss()
                        navController.navigateUp()
                    }.build()

                frag.show(supportFragmentManager, MessageDialogFragment.TAG)
            } else {
                it.exception?.localizedMessage?.let { msg ->
                    onIssueWhileBlocking(msg)
                }
            }
        }
    }

    private fun onIssueWhileBlocking(msg: String) {
        val f = MessageDialogFragment.builder(msg)
            .setTitle("Could not block ...")
            .build()

        f.show(supportFragmentManager, MessageDialogFragment.TAG)
    }

    fun blockUser(user: User) {
        val d = MessageDialogFragment.builder("Are you sure you want to block ${user.name}")
            .setPositiveButton("Block") { _, _ ->

                val hits = user.collaborations.intersect(UserManager.currentUser.posts)
                if (hits.isNotEmpty()) {
                    // there are posts where the blocked user is a collaborator, ask the current user to remove them first
                    val msg = "Cannot block ${user.name} because he/she is a collaborator in one of your posts. Remove them before blocking."
                    onIssueWhileBlocking(msg)
                } else {
                    block(user)
                }

            }.setNegativeButton("Cancel") { a, _ ->
                a.dismiss()
            }.build()

        d.show(supportFragmentManager, MessageDialogFragment.TAG)
    }

    override fun onChannelClick(chatChannel: ChatChannel, pos: Int) {
        chatChannel.isNewLastMessage = false
        viewModel.updateChatChannel(chatChannel)

        val title = if (chatChannel.type == "private") {
            val data1 = chatChannel.data1!!
            val data2 = chatChannel.data2!!

            if (data1.userId != UserManager.currentUserId) {
                data1.name
            } else {
                data2.name
            }
        } else {
            chatChannel.postTitle
        }

        val bundle = bundleOf(
            CHAT_CHANNEL to chatChannel,
            TITLE to title
        )

        navController.navigate(
            R.id.chatFragment2,
            bundle
        )
    }

    override fun onChannelUnread(chatChannel: ChatChannel) {

    }

    fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go

                val root = filesDir
                val photoFile: File? = try {
                    val dir = getNestedDir(root, "images/camera")
                    if (dir != null) {
                        getFile(dir, randomId() + ".jpg")
                    } else {
                        null
                    }
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "dispatchTakePictureIntent: ${ex.localizedMessage}")
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        FILE_PROV_AUTH,
                        it
                    )

                    cameraPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    fun setNavigationBarColor(color: Int?) {
        if (color != null) {
            window.navigationBarColor = color
        } else {
            window.navigationBarColor = initialNavColor
        }
    }

    private fun runOnBackgroundThread(block: suspend CoroutineScope.() -> Unit): Job {
        return lifecycleScope.launch {
            block()
        }
    }

    fun showTopSnack(msg: CharSequence, img: String? = null, bitmap: Bitmap? = null, label: String? = null, action: (() -> Unit)? = null) {
        val topSnackBinding = TopSnackBinding.inflate(layoutInflater)
        binding.root.addView(topSnackBinding.root)

        var job: Job? = null

        topSnackBinding.topSnackText.text= msg
        if (img != null) {
            topSnackBinding.topSnackImg.show()
            topSnackBinding.topSnackImg.setImageURI(img)
        }

        if (bitmap != null) {
            val file = convertBitmapToFile(bitmap)
            if (file != null) {
                val uri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, file)
                if (uri != null) {
                    topSnackBinding.topSnackImg.show()
                    topSnackBinding.topSnackImg.setImageURI(uri.toString())
                }
            }
        }

        var returnAnimation: Animator?

        if (binding.mainAppbar.isVisible) {
            topSnackBinding.root.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                topMargin = binding.mainAppbar.measuredHeight
            }

            topSnackBinding.root.translationY = -200f
            topSnackBinding.root.show()
            val anim = topSnackBinding.root.slideReset()

            anim.doOnEnd {
                job = runOnBackgroundThread {
                    delay(6000)
                    runOnUiThread {
                        returnAnimation = topSnackBinding.root.slideUp(200f)
                        returnAnimation?.doOnEnd {
                            binding.root.removeView(topSnackBinding.root)
                        }
                    }
                }
            }
        }


        if (action != null) {

            fun onClick() {
                job?.cancel()
                returnAnimation = topSnackBinding.root.slideUp(200f)
                returnAnimation?.doOnEnd {
                    binding.root.removeView(topSnackBinding.root)
                }
                action()
            }

            if (label != null) {
                topSnackBinding.topSnackAction.text = label
                topSnackBinding.topSnackAction.show()
                topSnackBinding.topSnackAction.setOnClickListener {
                    onClick()
                }
            } else {
                topSnackBinding.root.setOnClickListener {
                    onClick()
                }
            }

        } else {
            topSnackBinding.topSnackAction.hide()
        }

    }

}
