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
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
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
import com.jamid.codesquare.adapter.LocationItemClickListener
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.databinding.FragmentImageViewBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.home.chat.ChatInterface
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import com.jamid.codesquare.ui.zoomableView.DoubleTapGestureListener
import com.jamid.codesquare.ui.zoomableView.MultiGestureListener
import com.jamid.codesquare.ui.zoomableView.TapListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat

class MainActivity: LauncherActivity(), LocationItemClickListener, ProjectClickListener, ProjectRequestListener, UserClickListener, CommentListener, ChatChannelClickListener, MessageListener, NotificationItemClickListener {

    private var networkFlag = false
    private var loadingDialog: AlertDialog? = null
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

    private fun setCurrentUserPhotoAsDrawable(photo: String) = lifecycleScope.launch (Dispatchers.IO) {
        val currentSavedBitmap = viewModel.currentUserBitmap
        if (currentSavedBitmap != null) {
            val d = RoundedBitmapDrawableFactory.create(resources, currentSavedBitmap)
            setBitmapDrawable(d)
        } else {
            try {
                val isNetworkAvailable = viewModel.isNetworkAvailable.value
                if (isNetworkAvailable != null && isNetworkAvailable) {
                    val url = URL(photo)
                    val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    viewModel.currentUserBitmap = image
                    val d = RoundedBitmapDrawableFactory.create(resources, viewModel.currentUserBitmap)
                    setBitmapDrawable(d)
                }
            } catch (e: IOException) {
                viewModel.setCurrentError(e)
            }
        }
    }

    private fun setBitmapDrawable(drawable: RoundedBitmapDrawable) = runOnUiThread {
        if (binding.mainToolbar.menu.size() > 1) {
            drawable.cornerRadius = convertDpToPx(24, this).toFloat()
            binding.mainToolbar.menu.getItem(3).icon = drawable
        }
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
        LocationProvider.initialize(fusedLocationProviderClient, this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        binding.mainToolbar.setupWithNavController(navController, appBarConfiguration)

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

                    hideKeyboard(binding.root)
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
                    hideKeyboard(binding.root)

                    binding.mainToolbar.isTitleCentered = true

                    if (isNightMode()) {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
                    }

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

                    binding.mainToolbar.isTitleCentered = true
                    if (isNightMode()) {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
                    }

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
                R.id.preSearchFragment -> {
                    userInfoLayout?.hide()
                    binding.mainToolbar.show()
                    binding.mainTabLayout.hide()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                    binding.mainToolbar.isTitleCentered = true
                    if (isNightMode()) {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
                    }
                }
                R.id.searchFragment -> {
                    userInfoLayout?.hide()
                    binding.mainToolbar.show()
                    binding.mainTabLayout.show()
                    binding.mainPrimaryAction.slideDown(convertDpToPx(100).toFloat())
                    binding.mainToolbar.isTitleCentered = false
                    binding.mainToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.normal_grey))
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

                if (previouslyFetchedLocation != null) {
                    it.location = previouslyFetchedLocation
                    viewModel.insertCurrentUser(it)
                    return@observe
                }

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

        if (intent != null) {
            Log.d(TAG, intent.data.toString())
        }

        Firebase.auth.addAuthStateListener {
            loadingDialog?.dismiss()
            val firebaseUser = it.currentUser
            val currentDestination = navController.currentDestination?.id
            if (firebaseUser != null) {
                if (firebaseUser.isEmailVerified) {
                    // some important stuff
                    UserManager.init(firebaseUser.uid)

                    lifecycleScope.launch {
                        ChatInterface.initialize(firebaseUser.uid)
                    }
                } else {
                    if (currentDestination == R.id.createAccountFragment) {
                        navController.navigate(R.id.action_createAccountFragment_to_emailVerificationFragment, null, slideRightNavOptions())
                    }
                }
            }
        }

        viewModel.isNetworkAvailable.observe(this) { isNetworkAvailable ->
            if (isNetworkAvailable != null) {
                if (isNetworkAvailable) {
                    if (LocationProvider.isLocationPermissionAvailable) {
                        if (LocationProvider.isLocationEnabled) {
                            val currentLocation = LocationProvider.currentLocation
                            if (currentLocation != null) {
                                val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(currentLocation.latitude, currentLocation.longitude))
                                if (LocationProvider.nearbyAddresses.isEmpty()) {
                                    return@observe
                                } else {
                                    val locationName = LocationProvider.nearbyAddresses.first().getAddressLine(0)
                                    val location = Location(currentLocation.latitude, currentLocation.longitude, locationName, hash)
                                    val currentUser = viewModel.currentUser.value
                                    if (currentUser != null) {
                                        currentUser.location = location
                                    } else {
                                        // update when current user is not null
                                        previouslyFetchedLocation = location
                                    }
                                }
                            } else {
                                LocationProvider.getLastLocation(fusedLocationProviderClient)
                            }
                        } else {
                            LocationProvider.checkForLocationSettings(this, locationStateLauncher, fusedLocationProviderClient)
                        }
                    } else {
                        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

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
                    val type = extras["type"] as String?
                    val notificationId = extras["notificationId"] as String?
                    val receiverId = extras["receiverId"] as String?
                    val contextId = extras["contextId"] as String?
                    if (notificationId != null && receiverId != null && type != null && contextId != null) {
                        if (type == "request") {
                            val query = Firebase.firestore.collection("projectRequests")
                                .whereEqualTo("projectId", contextId)
                                .limit(1)

                            query.get()
                                .addOnSuccessListener {
                                    if (!it.isEmpty) {
                                        val projectRequest = it.toObjects(ProjectRequest::class.java).first()
                                        viewModel.insertProjectRequests(listOf(projectRequest))
                                        navController.navigate(R.id.action_homeFragment_to_projectRequestFragment)
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
                                        viewModel.insertNotifications(listOf(notification))
                                        navController.navigate(R.id.action_homeFragment_to_notificationFragment)
                                    }
                                }
                            }.addOnFailureListener {
                                Log.e(TAG, "710 => " + it.localizedMessage.orEmpty())
                            }

                        }
                    }
                }
                else -> {
                    // nothing
                }
            }
        }

        clearActiveNotifications()

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
                        TODO("Must be implemented")
                    }
                    option6 -> {
                        val bundle = bundleOf("contextObject" to project)

                        if (navController.currentDestination?.id == R.id.homeFragment) {
                            navController.navigate(R.id.action_homeFragment_to_reportFragment, bundle)
                        } else if (navController.currentDestination?.id == R.id.profileFragment) {
                            navController.navigate(R.id.action_profileFragment_to_reportFragment, bundle)
                        }
                    }
                }
               /* if (isCurrentUser) {
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
                }*/
            }
            .show()

        alertDialog.window?.setGravity(Gravity.BOTTOM)

    }

    override fun onProjectUndoClick(project: Project, projectRequest: ProjectRequest) {
        viewModel.onUndoProject(project, projectRequest)
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
            R.id.commentsFragment -> {
                navController.navigate(R.id.action_commentsFragment_to_profileFragment, bundle)
            }
            R.id.searchFragment -> {
                navController.navigate(R.id.action_searchFragment_to_profileFragment, bundle)
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

        val currentUser = viewModel.currentUser.value!!
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
                            viewModel.likeUser(user = user)
                        }
                        option4 -> {
                            viewModel.dislikeUser(user = user)
                        }
                        option5 -> {
                            navController.navigate(R.id.action_chatDetailFragment_to_reportFragment, bundleOf("contextObject" to user))
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

    override fun onCommentUserClick(user: User) {
        onUserClick(user)
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

            imageViewBinding.userTimeInfo.text = "Sent by ${message.sender.name} • " + SimpleDateFormat("hh:mm a, dd/MM/yyyy").format(message.createdAt)
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
            val flag = currentlyFocusedMessage?.senderId != viewModel.currentUser.value?.id

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

            if (currentlyFocusedMessage?.senderId != viewModel.currentUser.value?.id) {
                popupMenu.menu.getItem(2).isVisible = false
            }

            popupMenu.show()
        } else {
            //
        }
        return true
    }

    override fun onNotificationRead(notification: Notification) {
        notification.read = true
        viewModel.updateNotification(notification)
    }


    @ExperimentalPagingApi
    override fun onNotificationClick(notification: Notification, contextObj: Any) {
        when (contextObj) {
            is Project -> {
                if (notification.type == NOTIFICATION_JOIN_PROJECT) {
                    navController.navigate(R.id.action_notificationFragment_to_projectRequestFragment)
                } else {
                    onProjectClick(contextObj)
                }
            }
            is Comment -> {
                if (contextObj.commentLevel == 0.toLong()) {
                    val projectRef = Firebase.firestore.collection("projects").document(contextObj.projectId)
                    FireUtility.getDocument(projectRef) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val project = it1.result.toObject(Project::class.java)!!
                            onProjectCommentClick(project)
                        } else {
                            Log.d(TAG, "Something went wrong" + it1.exception?.localizedMessage)
                        }
                    }
                } else {
                    onClick(contextObj)
                }
            }
            is User -> {
                onUserClick(contextObj)
            }
            else -> throw IllegalArgumentException("Only object of type Project, Comment and User is allowed.")
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
        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
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
        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
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

        if (::navController.isInitialized) {
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
    }

    override fun onStop() {
        super.onStop()
        LocationProvider.stopLocationUpdates(fusedLocationProviderClient)
    }

    companion object {
        private const val TAG = "MainActivityDebugTag"
    }

}
