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
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.android.billingclient.api.*
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.jamid.codesquare.*
import com.jamid.codesquare.MainViewModel.Companion.MESSAGE_IDLE
import com.jamid.codesquare.MainViewModel.Companion.MESSAGE_READY
import com.jamid.codesquare.MainViewModel.Companion.MESSAGE_SELECTED
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@ExperimentalPagingApi
class MainActivity : LauncherActivity(), LocationItemClickListener, ProjectInviteListener,
    ProjectClickListener, ProjectRequestListener, UserClickListener, ChatChannelClickListener,
    MessageClickListener, NotificationItemClickListener, CommentListener, ImageClickListener, OptionClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var networkCallback: MyNetworkCallback
    private var previouslyFetchedLocation: Location? = null
    private var currentlyFocusedMessage: Message? = null
    private var currentMessageView: View? = null
    private var currentImageViewer: View? = null
    private var animationStartView: View? = null
    private var animationEndView: View? = null
    lateinit var billingClient: BillingClient

    private var updateJob: Job? = null

    var optionsFragment: OptionsFragment? = null

    var subscriptionFragment: SubscriptionFragment? = null
    private var isImageViewMode = false

    private var currentIndefiniteSnackbar: Snackbar? = null

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
            val uri = FileProvider.getUriForFile(this, FILE_PROV_AUTH, file)
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
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED -> {
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

    private fun createNewFileAndDownload(
        externalFilesDir: File,
        message: Message,
        onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit
    ) {
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
                    Log.d(
                        TAG,
                        "Probably file already exists. Or some other problem for which we are not being able to "
                    )
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
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
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

    fun selectMultipleImages() {
        selectMultipleImagesLauncher.launch(getMultipleImageIntent())
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

    private fun connectToGooglePlayBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                connectToGooglePlayBilling()
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished")
                    getSubscriptionDetails()
                    querySubscriptions()
                }
            }
        })
    }

    private fun getSubscriptionDetails() {
        val subscriptionIds = mutableListOf<String>()

        subscriptionIds.add("remove_ads_subscription")
        subscriptionIds.add("premium_membership_subscription")

        val query = SkuDetailsParams.newBuilder()
            .setSkusList(subscriptionIds)
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient.querySkuDetailsAsync(
            query
        ) { p0, p1 ->
            if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                for (p in p1) {
                    Log.d(TAG, p.toString())
                }
                viewModel.setSubscriptionDetailsList(p1)
            }
        }
    }

    private fun querySubscriptions() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS) { p0, p1 ->
            if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                if (p1.isNotEmpty()) {
                    // check for subscriptions here.
                    onPurchasesFetched(p1)
                } else {
                    subscriptionFragment?.dismiss()
                    updatePremiumState(-1)
                }
            }
        }
    }

    private fun verifyPurchase(purchase: Purchase) {

        val data = mapOf(
            "purchaseToken" to purchase.purchaseToken,
            "purchaseTime" to purchase.purchaseTime,
            "purchaseOrderId" to purchase.orderId,
            "userId" to UserManager.currentUserId,
            "productId" to JSONObject(purchase.originalJson).get("productId")
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyPurchase")
            .call(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    try {
                        val purchaseInfoFromServer = JSONObject(it.result.data.toString())
                        if (purchaseInfoFromServer.getBoolean("isValid")) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams
                                .newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()

                            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { it1 ->
                                if (it1.responseCode == BillingClient.BillingResponseCode.OK) {
                                    subscriptionFragment?.dismiss()
                                    navController.navigate(R.id.subscriberFragment, null, slideRightNavOptions())
                                } else {
                                    viewModel.setCurrentError(Exception("Something went wrong while acknowledging purchase."))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        viewModel.setCurrentError(e)
                    }
                } else {
                    toast("Something went wrong: " + it.exception?.localizedMessage.orEmpty())
                }
            }
    }

    @SuppressLint("UnsafeOptInUsageError", "VisibleForTests")
    override fun onCreate() {
        super.onCreate()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        // initialize all nodes
        MobileAds.initialize(this)
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { p0, p1 ->
            if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                    for (purchase in p1) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                            verifyPurchase(purchase)
                        }
                    }
                }
            }
            .build()

        connectToGooglePlayBilling()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        MyNotificationManager.init(this)
        startNetworkCallback()

        // TODO("What if the user rejects and later on he hates it because i'm asking
        //  for the permission every time the app opens. Check this out")
        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setupNavigation()

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

        viewModel.allUnreadNotifications.observe(this) {
            if (!it.isNullOrEmpty()) {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    val badgeDrawable = BadgeDrawable.create(this)
                    badgeDrawable.number = it.size
                    attachBadgeDrawable(badgeDrawable, binding.mainToolbar, R.id.notifications)
                }
            }
        }

        viewModel.isNetworkAvailable.observe(this) {
            if (it == true) {
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

        UserManager.currentUserLive.observe(this) {
            if (it != null) {
                viewModel.currentUserBitmap = null
                viewModel.insertCurrentUser(it)

                if (it.premiumState.toInt() != -1) {
                    viewModel.deleteAdProjects()
                }

            }
        }

        UserManager.authState.observe(this) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                    if (UserManager.isEmailVerified) {
                        listenForNotifications()
                        viewModel.lateInitChatController(UserManager.currentUserId)

                        downloadAllProjectRequestsByCurrentUser()
                        listenForProjectRequestsByCurrentUser()
                    }
                } else {
                    viewModel.isNetworkAvailable.removeObservers(this)
                }
            }
        }

        onReceiveData()

        // TODO("Not actually working, need to do a whole lot of changes for this")
        clearActiveNotifications()

    }

    private fun downloadAllProjectRequestsByCurrentUser() {
        FireUtility.getProjectRequestsOfCurrentUser { result ->
            when (result) {
                is Result.Error -> viewModel.setCurrentError(result.exception)
                is Result.Success -> {
                    val projectRequests = result.data
                    viewModel.insertProjectRequests(projectRequests)
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
                    val chatChannelId = extras[CHANNEL_ID] as String?
                    if (chatChannelId != null) {
                        FireUtility.getChatChannel(chatChannelId) {
                            when (it) {
                                is Result.Error -> viewModel.setCurrentError(it.exception)
                                is Result.Success -> {
                                    onChannelClick(it.data)
                                }
                                null -> Log.w(TAG, "Something went wrong while trying to get chat channel with id: $chatChannelId")
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

    private fun updatePremiumState(state: Long) {
        FireUtility.updateUser2(mapOf("premiumState" to state)) {
            if (it.isSuccessful) {
                if (state.toInt() == -1) {
                    FireUtility.removeSubscriptions {
                        Log.d(TAG, "Deleted purchases")
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
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
        val bundle = bundleOf(TITLE to project.name, PROJECT to project)
        navController.navigate(R.id.projectFragmentContainer, bundle, slideRightNavOptions())
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
            val notification = Notification.createNotification(
                content,
                currentUser.id,
                project.creator.userId,
                projectId = project.id,
                title = title
            )
            FireUtility.likeProject(project) {
                if (it.isSuccessful) {
                    // check if notification already exists
                    onLike()
                    if (notification.senderId != notification.receiverId) {
                        FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                            if (error != null) {
                                viewModel.setCurrentError(error)
                                return@checkIfNotificationExistsByContent
                            }
                            if (!exists) {
                                FireUtility.sendNotification(notification) {

                                }
                            }
                        }
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onProjectSaveClick(project: Project) {
        if (project.isSaved) {
            // un-save
            FireUtility.unSaveProject(project) {
                if (it.isSuccessful) {
                    project.isSaved = false
                    viewModel.insertProjects(project)
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
                    viewModel.insertProjects(project)
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        }
    }

    override fun onProjectJoinClick(project: Project) {
        if (project.isRequested) {
            // undo request
            FireUtility.getExistingProjectRequest(project.id, UserManager.currentUserId) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        val request = it.data

                        onProjectUndoClick(project, request)
                    }
                    null -> {
                        Log.w(
                            TAG,
                            "Project request could not be found in firestore. " +
                                    "Check the server to see if everything is in place. " +
                                    "Project Id -> ${project.id}, " +
                                    "SenderId -> ${UserManager.currentUserId}"
                        )
                    }
                }
            }
        } else {
            // send request
            val currentUser = UserManager.currentUser
            val content = currentUser.name + " wants to join your project"
            val notification = Notification.createNotification(
                content,
                currentUser.id,
                project.creator.userId,
                type = 1,
                userId = currentUser.id,
                title = project.name
            )

            fun onRequestSent(projectRequest: ProjectRequest) {
                val requestsList = project.requests.addItemToList(projectRequest.requestId)
                project.requests = requestsList
                project.isRequested = true
                viewModel.insertProjectsWithoutProcessing(project)
                viewModel.insertProjectRequests(projectRequest)
            }

            FireUtility.joinProject(notification.id, project) { task, projectRequest ->
                if (task.isSuccessful) {

                    onRequestSent(projectRequest)

                    if (notification.senderId != notification.receiverId) {
                        FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                            if (error != null) {
                                viewModel.setCurrentError(error)
                            } else {
                                if (!exists) {
                                    FireUtility.sendNotification(notification) {
                                        if (!it.isSuccessful) {
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


    }

    // project request must have project included inside it
    override fun onProjectRequestAccept(projectRequest: ProjectRequest) {
        val project = projectRequest.project
        val currentUser = UserManager.currentUser
        if (project != null) {
            if (project.contributors.size < 5 || currentUser.premiumState.toInt() == 1) {
                viewModel.acceptRequest(projectRequest)
            } else {
                val appName = getString(R.string.app_name)
                val upgradeMsg = getString(R.string.upgrade_plan_imsg)
                showDialog(upgradeMsg, appName, posLabel = getString(R.string.upgrade)) {
                    showSubscriptionFragment()
                }
            }
        }
    }

    // project request must have project included inside it
    override fun onProjectRequestCancel(projectRequest: ProjectRequest) {
        val currentUser = UserManager.currentUser
        val content = currentUser.name + " rejected your project request"
        val title = projectRequest.project?.name

        val notification = Notification.createNotification(
            content,
            currentUser.id,
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

                FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                    if (error != null) {
                        viewModel.setCurrentError(error)
                    } else {
                        if (!exists) {
                            FireUtility.sendNotification(notification) {
                                Log.d(TAG, "Project request cancelled")
                            }
                        }
                    }
                }
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
                                    if (it1.isSuccessful) {
                                        // TODO("Notify the users that the request has been deleted and the project is deleted.")
                                        // TODO("Should also delete project invites if there's any but not important.")
                                    } else {
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Collab")
            .setMessage("Remove ads from the app?")
            .setPositiveButton("Yes") { _, _ ->
                showSubscriptionFragment()
            }.setNegativeButton("Cancel") { a, _ ->
                a.dismiss()
            }
            .show()
    }

    override fun onAdError(project: Project) {
        viewModel.deleteLocalProject(project)
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
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectRequestClick(projectRequest: ProjectRequest) {
        projectRequest.project?.let { onProjectClick(it) }
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
    }

    override fun onProjectOptionClick(project: Project, position: Int) {

        viewModel.setCurrentFocusedProject(project)
        viewModel.setCurrentFocusedUser(project.creator.toUser())

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

        optionsFragment = OptionsFragment.newInstance(project.name, choices, icons)
        optionsFragment?.show(supportFragmentManager, OptionsFragment.TAG)

    }

    // TODO("Use const val for holding the long value")
    private fun archive(project: Project, duration: Long = 2592000000L) {
        val currentUserId = UserManager.currentUserId
        FireUtility.archiveProject(project, duration) {
            if (it.isSuccessful) {
                // notify the other contributors that the project has been archived
                val content = "${project.name} has been archived."
                val notification = Notification.createNotification(
                    content,
                    currentUserId,
                    project.chatChannel
                )
                FireUtility.sendNotificationToChannel(notification) { it1 ->
                    if (it1.isSuccessful) {
                        // updating project locally
                        project.expiredAt = System.currentTimeMillis() + duration
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

    override fun onProjectUndoClick(project: Project, projectRequest: ProjectRequest) {
        FireUtility.undoJoinProject(projectRequest) {
            if (it.isSuccessful) {
                project.isRequested = false
                val newList = project.requests.removeItemFromList(projectRequest.requestId)
                project.requests = newList
                viewModel.insertProjectsWithoutProcessing(project)
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onProjectContributorsClick(project: Project) {
        val bundle = bundleOf(
            PROJECT to project,
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
//        viewModel.deleteLocalProject(project)
    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }
        navController.navigate(R.id.profileFragment, bundle, slideRightNavOptions())
    }

    override fun onUserOptionClick(
       user: User
    ) {

        viewModel.setCurrentFocusedUser(user)

        /*val option1 = "Set as admin"
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

            // the user has created the project, hence cannot leave project
            if (currentUser.projects.contains(projectId))
                return

            val choices = arrayOf(option7)

            val alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("You")
                .setItems(choices) { _, index ->
                    when (choices[index]) {
                        option7 -> {

                            // Removing current user from project
                            FireUtility.removeUserFromProject(
                                currentUser,
                                projectId,
                                chatChannelId
                            ) {
                                if (it.isSuccessful) {

                                    navController.popBackStack(R.id.homeFragment, false)

                                    viewModel.getLocalChatChannel(chatChannelId) { channel ->
                                        if (channel != null) {

                                            // Removing the current user from chat channel
                                            FireUtility.removeUserFromChatChannel(
                                                currentUser,
                                                channel
                                            ) { task ->
                                                if (task.isSuccessful) {
                                                    // notifying other users that the current user has left the project
                                                    val content =
                                                        "${currentUser.name} has left the project"
                                                    val notification =
                                                        Notification.createNotification(
                                                            content,
                                                            currentUserId,
                                                            chatChannelId
                                                        )
                                                    FireUtility.sendNotificationToChannel(
                                                        notification
                                                    ) { it1 ->
                                                        if (it1.isSuccessful) {

                                                            // if there is a project in local db, update it
                                                            viewModel.getLocalProject(projectId) { project ->
                                                                if (project != null) {
                                                                    val contributors =
                                                                        project.contributors.removeItemFromList(
                                                                            currentUserId
                                                                        )
                                                                    project.contributors =
                                                                        contributors
                                                                    viewModel.updateLocalProject(
                                                                        project
                                                                    )
                                                                } else {
                                                                    Log.d(
                                                                        TAG,
                                                                        "Tried fetching local project with id: $projectId but received null."
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            viewModel.setCurrentError(it.exception)
                                                        }
                                                    }
                                                } else {
                                                    viewModel.setCurrentError(task.exception)
                                                }
                                            }
                                        } else {
                                            Log.d(
                                                TAG,
                                                "Tried fetching local chat channel with id: $chatChannelId but received null."
                                            )
                                        }
                                    }
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                }
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
                            likeUser(user.id)
                        }
                        option4 -> {
                            dislikeUser(user.id)
                        }
                        option5 -> {
                            navController.navigate(
                                R.id.action_chatDetailFragment_to_reportFragment,
                                bundleOf("contextObject" to user),
                                slideRightNavOptions()
                            )
                        }
                        option6 -> {

                            FireUtility.removeUserFromProject(user, projectId, chatChannelId) {
                                if (it.isSuccessful) {
                                    viewModel.getLocalChatChannel(chatChannelId) { channel ->
                                        if (channel != null) {

                                            // Removing the current user from chat channel
                                            FireUtility.removeUserFromChatChannel(
                                                user,
                                                channel
                                            ) { task ->
                                                if (task.isSuccessful) {
                                                    // notifying other users that the current user has left the project
                                                    val content =
                                                        "${user.name} has left the project"
                                                    val notification =
                                                        Notification.createNotification(
                                                            content,
                                                            currentUserId,
                                                            chatChannelId
                                                        )
                                                    FireUtility.sendNotificationToChannel(
                                                        notification
                                                    ) { it1 ->
                                                        if (it1.isSuccessful) {

                                                            // if there is a project in local db, update it
                                                            viewModel.getLocalProject(projectId) { project ->
                                                                if (project != null) {
                                                                    val contributors =
                                                                        project.contributors.removeItemFromList(
                                                                            user.id
                                                                        )
                                                                    project.contributors =
                                                                        contributors
                                                                    viewModel.updateLocalProject(
                                                                        project
                                                                    )
                                                                } else {
                                                                    Log.d(
                                                                        TAG,
                                                                        "Tried fetching local project with id: $projectId but received null."
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            viewModel.setCurrentError(it.exception)
                                                        }
                                                    }
                                                } else {
                                                    viewModel.setCurrentError(task.exception)
                                                }
                                            }
                                        } else {
                                            Log.d(
                                                TAG,
                                                "Tried fetching local chat channel with id: $chatChannelId but received null."
                                            )
                                        }
                                    }
                                } else {
                                    viewModel.setCurrentError(it.exception)
                                }
                            }

                            *//*viewModel.castVoteToRemoveUser(user, projectId, currentUserId) {
                                if (it.isSuccessful) {
                                    val ad = MaterialAlertDialogBuilder(this)
                                        .setTitle("Collab")
                                        .setMessage("Your vote to remove ${user.name} has been submitted anonymously. No one is notified of this action. As more contributors cast their vote on favor, the decision will be concluded.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK") { a, _ ->
                                            a.dismiss()
                                        }.setNegativeButton("Undo") { _, _ ->
                                            viewModel.undoVoteCast(
                                                user,
                                                projectId,
                                                currentUserId
                                            ) { it1 ->
                                                if (it1.isSuccessful) {
                                                    Snackbar.make(
                                                        binding.root,
                                                        "Your vote has been removed.",
                                                        Snackbar.LENGTH_SHORT
                                                    ).show()
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
                            }*//*
                        }
                    }
                }
                .show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)
        }*/
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
                    currentUser.id,
                    userId,
                    userId = userId,
                    title = currentUser.name
                )

                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (it1.isSuccessful) {
                                        Log.d(TAG, "Sent notification: $notification")
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

    override fun onUserLikeClick(user: User) {
        if (user.isLiked) {
            dislikeUser(user.id)
        } else {
            likeUser(user.id)
        }
    }

    override fun onCommentLikeClicked(comment: Comment) {
        val currentUser = UserManager.currentUser
        if (!comment.isLiked) {
            FireUtility.likeComment(comment) {
                val content = currentUser.name + " liked your comment"
                val notification = Notification.createNotification(
                    content,
                    currentUser.id,
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
        val bundle = bundleOf(
            CONTEXT_OBJECT to comment
        )

        navController.navigate(
            R.id.reportFragment, bundle, slideRightNavOptions()
        )
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

    override fun onStartDownload(
        message: Message,
        onComplete: (Task<FileDownloadTask.TaskSnapshot>, newMessage: Message) -> Unit
    ) {
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

    override fun onDocumentClick(message: Message) {
        val externalDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val destination = File(externalDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file)
    }

    override fun onImageClick(
        view: View,
        message: Message,
        controllerListener: FrescoImageControllerListener
    ) {
        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val name = message.content + message.metadata!!.ext
        val destination = File(imagesDir, message.chatChannelId)
        val file = File(destination, name)
        val uri = Uri.fromFile(file)

        val image = Image(uri.toString(), controllerListener.finalWidth, controllerListener.finalHeight, message.metadata?.ext!!)

        showImageViewFragment(view, image, message)

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

    override fun onForwardClick(view: View, messages: ArrayList<Message>) {

    }

    override fun onMessageLongClick(message: Message) {
        viewModel.updateRestOfTheMessages(message.chatChannelId, MESSAGE_READY)

        lifecycleScope.launch {
            delay(300)
            message.state = MESSAGE_SELECTED
            viewModel.updateMessage(message)
        }

    }

    override fun onMessageLongPress(p0: MotionEvent?) {
        if (currentlyFocusedMessage != null && currentlyFocusedMessage?.isDownloaded == true) {
            if (currentlyFocusedMessage!!.state == MESSAGE_IDLE) {
                currentlyFocusedMessage!!.state = MESSAGE_READY

                viewModel.updateRestOfTheMessages(currentlyFocusedMessage!!.chatChannelId, MESSAGE_READY)

                lifecycleScope.launch {
                    delay(300)
                    viewModel.setSelectMode(true)
                    currentlyFocusedMessage!!.state = MESSAGE_SELECTED
                    viewModel.updateMessage(currentlyFocusedMessage!!)
                }
            } else {
                Log.d(TAG, "${currentlyFocusedMessage!!.state} is the state of the message")
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
                    currentMessageView!!.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.lightest_black
                        )
                    )
                } else {
                    currentMessageView!!.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.lightest_blue
                        )
                    )
                }
                viewModel.updateMessage(currentlyFocusedMessage!!)
            } else {
                currentlyFocusedMessage!!.state = 0
                currentMessageView!!.isSelected = false
                currentMessageView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.transparent
                    )
                )
                viewModel.updateMessage(currentlyFocusedMessage!!)
            }
        }
        return true
    }

    override fun onMessageDoubleTapped(p0: MotionEvent?): Boolean {
        if (viewModel.selectedMessages.value.isNullOrEmpty() && viewModel.singleSelectedMessage.value == null) {

            if (currentlyFocusedMessage?.isDownloaded != true)
                return true

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
                    R.id.popup_forward -> {
                        onForwardClick(currentMessageView!!, arrayListOf(currentlyFocusedMessage!!))
                    }
                    R.id.popup_details -> {
                        val bundle = bundleOf("message" to currentlyFocusedMessage)
                        navController.navigate(
                            R.id.action_chatFragment_to_messageDetailFragment,
                            bundle,
                            slideRightNavOptions()
                        )
                    }
                    R.id.popup_sender -> {
                        onUserClick(currentlyFocusedMessage!!)
                    }
                }
                true
            }

            if (currentlyFocusedMessage?.senderId != UserManager.currentUserId) {
                popupMenu.menu.getItem(2).isVisible = false
            } else {
                popupMenu.menu.getItem(3).isVisible = false
            }

            popupMenu.show()
        }
        return true
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

        Log.d(TAG, notification.toString())

        val userId = notification.userId
        if (userId != null) {
            FireUtility.getUser(userId) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        val user = it.data
                        val formattedUser = processUsers(user)[0]
                        viewModel.insertUsers(user)
                        onUserClick(formattedUser)
                    }
                    null -> {
                        Log.w(TAG, "Something went wrong while trying to get user data with id: $userId")
                    }
                }
            }
        } else {
            Log.i(TAG, "Notification with notification id: ${notification.id} doesn't have user Id")
        }

        val projectId = notification.projectId
        if (projectId != null) {
            FireUtility.getProject(projectId) {
                when (it) {
                    is Result.Error -> viewModel.setCurrentError(it.exception)
                    is Result.Success -> {
                        val project = it.data
                        navController.navigate(
                            R.id.projectContributorsFragment,
                            bundleOf(TITLE to project.name, PROJECT to project),
                            slideRightNavOptions()
                        )
                    }
                    null -> {
                        Log.w(TAG, "Something went wrong while trying to get project with id: $projectId")
                    }
                }
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
                            FireUtility.getProject(comment.projectId) { projectResult ->
                                when (projectResult) {
                                    is Result.Error -> viewModel.setCurrentError(projectResult.exception)
                                    is Result.Success -> {
                                        val project1 = projectResult.data
                                        onProjectCommentClick(project1)
                                    }
                                    null -> {
                                        Log.w(TAG, "Something went wrong while trying to get project with project id ${comment.projectId}")
                                        FireUtility.deleteNotification(notification) { it1 ->
                                            if (it1.isSuccessful) {
                                                viewModel.deleteNotification(notification)
                                            } else {
                                                viewModel.setCurrentError(it1.exception)
                                            }
                                        }
                                    }
                                }
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
                        viewModel.getLocalUser(replyMessage.senderId) { sender ->
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

    override fun onGetMessageReplyUser(senderId: String, onComplete: (User?) -> Unit) {
        viewModel.getLocalUser(senderId) {
            onComplete(it)
        }
    }

    override fun onMessageStateChanged(message: Message) {
        viewModel.updateMessage(message)
    }

    override fun onMessageFocused(message: Message, parent: View) {
        currentlyFocusedMessage = message
        currentMessageView = parent
    }

    override fun onPause() {
        super.onPause()

        Log.d(TAG, "Paused main activity")

        if (UserManager.isInitialized) {
            val currentUser = UserManager.currentUser
            val batch = Firebase.firestore.batch()

            for (channel in currentUser.chatChannels) {
                viewModel.updateRestOfTheMessages(channel, MESSAGE_IDLE)

                for (token in currentUser.registrationTokens) {
                    val ref = Firebase.firestore.collection("chatChannels").document(channel)
                    batch.update(ref, "registrationTokens", FieldValue.arrayUnion(token))
                }
            }

            batch.commit().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(
                        TAG,
                        "Successfully changed offline status to online in chat channel document."
                    )
                } else {
                    Log.e(TAG, "1109 - ${it.exception?.localizedMessage}")
                }
            }
        }

        stopNetworkCallback()
    }

    private fun onPurchasesFetched(purchases: List<Purchase>) {
        try {
            Log.d(TAG, purchases.toString())
            if (purchases.size > 1) {
                throw IllegalStateException("There cannot be more than one subscription at a time.")
            } else {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        verifyPurchase(purchase)
                    } else {
                        when (JSONObject(purchase.originalJson).get("productId")) {
                            "remove_ads_subscription" -> {
                                updatePremiumState(0)
                            }
                            "premium_membership_subscription" -> {
                                updatePremiumState(1)
                            }
                            else -> {
                                //
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            viewModel.setCurrentError(e)
        }
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
                    Log.d(
                        TAG,
                        "Successfully set the online status to offline in chat channel document"
                    )
                } else {
                    Log.e(TAG, "1142 - ${it.exception?.localizedMessage}")
                }
            }

            if (::billingClient.isInitialized) {
                querySubscriptions()
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
        val notification = Notification.createNotification(
            content,
            currentUser.id,
            projectInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        FireUtility.acceptProjectInvite(currentUser, projectInvite) {
            if (it.isSuccessful) {

                // current user was added to project successfully
                viewModel.addCurrentUserToProject(projectInvite)
                viewModel.deleteNotificationById(projectInvite.notificationId)

                // extra
                val otherUser = projectInvite.sender
                if (otherUser != null) {
                    val newList =
                        otherUser.projectInvites.removeItemFromList(
                            projectInvite.id
                        )
                    otherUser.projectInvites = newList
                    viewModel.updateOtherUserLocally(otherUser)
                }

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

        val notification = Notification.createNotification(
            content,
            currentUser.id,
            projectInvite.senderId,
            userId = currentUser.id,
            title = title
        )

        FireUtility.cancelProjectInvite(projectInvite) {
            if (it.isSuccessful) {
                viewModel.deleteProjectInvite(projectInvite)
                viewModel.deleteNotificationById(projectInvite.notificationId)

                // extra
                val otherUser = projectInvite.sender
                if (otherUser != null) {
                    val newList =
                        otherUser.projectInvites.removeItemFromList(
                            projectInvite.id
                        )
                    otherUser.projectInvites = newList
                    viewModel.updateOtherUserLocally(otherUser)
                }

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


    private fun listenForProjectRequestsByCurrentUser() {
        // deleting all existing project requests because we will get new ones here
        viewModel.deleteAllProjectRequests {

            // this line requires that the UserManager must be initialized first
            val currentUser = UserManager.currentUser

            for (requestId in currentUser.projectRequests) {
                Firebase.firestore.collection(PROJECT_REQUESTS)
                    .document(requestId)
                    .addSnapshotListener { documentSnapshot, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                            return@addSnapshotListener
                        }

                        if (documentSnapshot != null) {
                            if (documentSnapshot.exists()) {
                                // we need the project request in future in case we need to revoke out request or if the request gets accepted
                                viewModel.insertProjectRequests(documentSnapshot.toObject(ProjectRequest::class.java)!!)
                            } else {
                                /*

                                document got deleted
                                * the request was revoked
                                * the request was accepted

                                whichever it maybe we are only concerned that the latest data of
                                the project associated with this request needs to be localized

                                */

                                viewModel.getLocalProjectRequest(requestId) { existingRequest ->
                                    if (existingRequest != null) {
                                        val projectId = existingRequest.projectId
                                        FireUtility.getProject(projectId) {
                                            when (it) {
                                                is Result.Error -> viewModel.setCurrentError(it.exception)
                                                is Result.Success -> {
                                                    val project = it.data
                                                    viewModel.insertProjects(project)
                                                }
                                                null -> {
                                                    Log.w(TAG, "Something went wrong while looking for project with project id: $projectId through request with id: ${existingRequest.requestId}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    override fun onImageClick(view: View, image: Image) {
        showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        viewModel.deleteChatUploadImageAtPosition(position)
    }

    override fun onOptionClick(option: Option) {

        optionsFragment?.dismiss()

        val focusedUser = viewModel.currentFocusedUser.value
        val focusedChatChannel = viewModel.currentFocusedChatChannel.value
        val focusedProject = viewModel.currentFocusedProject.value
        val focusedMessage = viewModel.currentFocusedMessage.value

        val option3 = OPTION_3 + focusedUser?.name
        val option4 = OPTION_4 + focusedUser?.name
        val option5 = OPTION_5 + focusedUser?.name

        when (option.item) {
            OPTION_1 -> {
                if (focusedChatChannel != null && focusedUser != null) {
                    viewModel.setOtherUserAsAdmin(focusedChatChannel.chatChannelId, focusedUser.id) {
                        if (!it.isSuccessful) {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_2 -> {
                if (focusedChatChannel != null && focusedUser != null) {
                    viewModel.removeOtherUserFromAdmin(focusedChatChannel.chatChannelId, focusedUser.id) {
                        if (!it.isSuccessful) {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            option3 -> {
                if (focusedUser != null) {
                    likeUser(focusedUser.id)
                }
            }
            option4 -> {
                if (focusedUser != null) {
                    dislikeUser(focusedUser.id)
                }
            }
            option5 -> {
                navController.navigate(
                    R.id.action_chatDetailFragment_to_reportFragment,
                    bundleOf("contextObject" to focusedUser),
                    slideRightNavOptions()
                )
            }
            OPTION_6 -> {
                if (focusedChatChannel != null && focusedUser != null) {
                    FireUtility.removeUserFromProject(focusedUser, focusedChatChannel.projectId, focusedChatChannel.chatChannelId) {
                        if (it.isSuccessful) {
                            viewModel.getLocalChatChannel(focusedChatChannel.chatChannelId) { channel ->
                                if (channel != null) {
                                    FireUtility.removeUserFromChatChannel(
                                        focusedUser,
                                        channel
                                    ) { task ->
                                        if (task.isSuccessful) {
                                            val content =
                                                "${focusedUser.name} has left the project"
                                            val notification =
                                                Notification.createNotification(
                                                    content,
                                                    UserManager.currentUserId,
                                                    focusedChatChannel.chatChannelId
                                                )
                                            FireUtility.sendNotificationToChannel(
                                                notification
                                            ) { it1 ->
                                                if (it1.isSuccessful) {
                                                    viewModel.getLocalProject(focusedChatChannel.projectId) { project ->
                                                        if (project != null) {
                                                            val contributors =
                                                                project.contributors.removeItemFromList(
                                                                    focusedUser.id
                                                                )
                                                            project.contributors =
                                                                contributors
                                                            viewModel.updateLocalProject(
                                                                project
                                                            )
                                                        } else {
                                                            Log.d(
                                                                TAG,
                                                                "Tried fetching local project with id: ${focusedChatChannel.projectId} but received null."
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    viewModel.setCurrentError(it.exception)
                                                }
                                            }
                                        } else {
                                            viewModel.setCurrentError(task.exception)
                                        }
                                    }
                                } else {
                                    Log.d(
                                        TAG,
                                        "Tried fetching local chat channel with id: ${focusedChatChannel.chatChannelId} but received null."
                                    )
                                }
                            }
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_7 -> {
                if (focusedChatChannel != null && focusedUser != null) {
                    FireUtility.removeUserFromProject(
                        focusedUser,
                        focusedChatChannel.projectId,
                        focusedChatChannel.chatChannelId
                    ) {
                        if (it.isSuccessful) {

                            navController.popBackStack(R.id.homeFragment, false)

                            viewModel.getLocalChatChannel(focusedChatChannel.chatChannelId) { channel ->
                                if (channel != null) {

                                    // Removing the current user from chat channel
                                    FireUtility.removeUserFromChatChannel(
                                        UserManager.currentUser,
                                        channel
                                    ) { task ->
                                        if (task.isSuccessful) {
                                            // notifying other users that the current user has left the project
                                            val content =
                                                "${UserManager.currentUser.name} has left the project"
                                            val notification =
                                                Notification.createNotification(
                                                    content,
                                                    UserManager.currentUserId,
                                                    focusedChatChannel.chatChannelId
                                                )
                                            FireUtility.sendNotificationToChannel(
                                                notification
                                            ) { it1 ->
                                                if (it1.isSuccessful) {

                                                    // if there is a project in local db, update it
                                                    viewModel.getLocalProject(focusedChatChannel.projectId) { project ->
                                                        if (project != null) {
                                                            val contributors =
                                                                project.contributors.removeItemFromList(
                                                                    UserManager.currentUserId
                                                                )
                                                            project.contributors =
                                                                contributors
                                                            viewModel.updateLocalProject(
                                                                project
                                                            )
                                                        } else {
                                                            Log.d(
                                                                TAG,
                                                                "Tried fetching local project with id: ${focusedChatChannel.projectId} but received null."
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    viewModel.setCurrentError(it.exception)
                                                }
                                            }
                                        } else {
                                            viewModel.setCurrentError(task.exception)
                                        }
                                    }
                                } else {
                                    Log.d(
                                        TAG,
                                        "Tried fetching local chat channel with id: ${focusedChatChannel.chatChannelId} but received null."
                                    )
                                }
                            }
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_8 -> {
                selectChatUploadImages()
            }
            OPTION_9 -> {
                selectChatUploadDocuments()
            }
            OPTION_10, OPTION_11 -> {
                if (focusedProject != null) {
                    onProjectSaveClick(focusedProject)
                }
            }
            OPTION_12 -> {
                if (focusedProject != null) {
                    val sharedPreference = PreferenceManager.getDefaultSharedPreferences(this)
                    if (sharedPreference.contains(PROJECT_EXPIRY)) {
                        val durationS = sharedPreference.getString(PROJECT_EXPIRY, "30")
                        if (durationS.isNullOrBlank()) {
                            archive(focusedProject)
                        } else {
                            if (durationS.isDigitsOnly()) {
                                // durationS = number of days for expiry
                                val duration = durationS.toLong() * 24 * 60 * 60 * 1000
                                archive(focusedProject, duration)
                            }
                        }
                    } else {
                        archive(focusedProject)
                    }
                }
            }
            OPTION_13 -> {
                if (focusedProject != null) {
                    FireUtility.unArchiveProject(focusedProject) {
                        if (it.isSuccessful) {
                            focusedProject.expiredAt = -1
                            focusedProject.isArchived = false
                            viewModel.updateLocalProject(focusedProject)
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
            OPTION_14 -> {
                val bundle = bundleOf(CONTEXT_OBJECT to focusedProject)
                navController.navigate(R.id.reportFragment, bundle, slideRightNavOptions())
            }
            OPTION_15 -> {
                val bundle = bundleOf(PROJECT to focusedProject)
                navController.navigate(R.id.updateProjectFragment, bundle, slideRightNavOptions())
            }
            OPTION_16 -> {
                navController.navigate(R.id.projectFragment, bundleOf(PROJECT to focusedProject, TITLE to focusedProject?.name), slideRightNavOptions())
            }
            OPTION_17 -> {
                selectImage()
            }
            OPTION_18 -> {
                viewModel.setCurrentImage(null)
            }
            OPTION_19 -> {

            }
            OPTION_20 -> {

            }
            OPTION_21 -> {

            }
            OPTION_22 -> {

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
        }
    }

}
