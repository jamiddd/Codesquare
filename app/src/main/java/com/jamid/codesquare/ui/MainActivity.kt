package com.jamid.codesquare.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.net.*
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
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
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.listeners.*
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import android.graphics.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.net.URL
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.codesquare.MyFirebaseMessagingService.Companion.NOTIFICATION_ID

class MainActivity: AppCompatActivity(), LocationItemClickListener, ProjectClickListener, ProjectRequestListener, UserClickListener, CommentListener, ChatChannelClickListener, MessageListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private var networkFlag = false
    private var userListenerRegistration: ListenerRegistration? = null
    private var loadingDialog: AlertDialog? = null
    private lateinit var notificationManager: NotificationManager

    private val tokenReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val token = intent?.extras?.getString(MyFirebaseMessagingService.ARG_TOKEN)
            if (token != null) {
                viewModel.sendRegistrationTokenToServer(token)
            }
        }
    }

    private val notificationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val title = intent?.extras?.get(MyFirebaseMessagingService.ARG_NOTIFICATION_TITLE) as String? ?: ""
            val content = intent?.extras?.get(MyFirebaseMessagingService.ARG_NOTIFICATION_BODY) as String? ?: ""

            val notifyBuilder = getNotificationBuilder(title, content)
            notificationManager.notify(NOTIFICATION_ID, notifyBuilder.build())

        }
    }

    private fun getNotificationBuilder(title: String, content: String): NotificationCompat.Builder {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.notificationFragment)
            .setArguments(null)
            .createPendingIntent()

        return NotificationCompat.Builder(this, MyFirebaseMessagingService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
    }

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

    private val selectReportImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

                viewModel.setReportUploadImages(images)

            } else {
                it.data?.data?.let { it1 ->
                    viewModel.setChatUploadImages(listOf(it1))
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

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MyFirebaseMessagingService.NOTIFICATION_CHANNEL_ID,
                "Mascot Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notification from Mascot"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.mainToolbar)

        // must
        createNotificationChannel()

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
                ContextCompat.getDrawable(this, R.drawable.ic_collab_logo)
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

        viewModel.errors.observe(this) {
            if (it != null) {
                Log.d(TAG, it.localizedMessage ?: "Unknown error")
            }
        }

        viewModel.currentError.observe(this) { exception ->
            if (exception != null) {
                loadingDialog?.dismiss()
                Log.e(TAG, exception.localizedMessage.orEmpty())
//                toast(exception.localizedMessage.orEmpty())
            }
        }

        Firebase.auth.addAuthStateListener {
            loadingDialog?.dismiss()
            val firebaseUser = it.currentUser
            if (firebaseUser != null) {

                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        val token = task.result

                        viewModel.sendRegistrationTokenToServer(token)
                    })

                if (firebaseUser.isEmailVerified) {

                    if (navController.currentDestination?.id == R.id.loginFragment) {
                        // changed this line
                        navController.navigate(R.id.action_loginFragment_to_homeFragment)
                    } else if (navController.currentDestination?.id == R.id.splashFragment) {
                        lifecycleScope.launch {
                            delay(4000)
                            navController.navigate(R.id.action_splashFragment_to_homeFragment)
                        }
                    }

                    userListenerRegistration?.remove()
                    userListenerRegistration = Firebase.firestore.collection("users")
                        .document(firebaseUser.uid)
                        .addSnapshotListener { value, error ->
                            if (error != null) {
                                viewModel.setCurrentError(error)
                            }

                            if (value != null && value.exists()) {
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

                                    lifecycleScope.launch (Dispatchers.IO) {
                                        try {
                                            val isNetworkAvailable = viewModel.isNetworkAvailable.value
                                            if (isNetworkAvailable != null && isNetworkAvailable) {
                                                val url = URL(newUser.photo)
                                                val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                                                viewModel.currentUserBitmap = getCircleBitmap(image)
                                            }
                                        } catch (e: IOException) {
                                            viewModel.setCurrentError(e)
                                        }
                                    }


                                    viewModel.insertCurrentUser(newUser)
                                }
                            }
                        }
                } else {
                    when (navController.currentDestination?.id) {
                        R.id.homeFragment -> {
                            navController.navigate(R.id.action_homeFragment_to_emailVerificationFragment, null, slideRightNavOptions())
                        }
                        R.id.createAccountFragment -> {
                            navController.navigate(R.id.action_createAccountFragment_to_emailVerificationFragment, null, slideRightNavOptions())
                        }
                        R.id.splashFragment -> {
                            navController.navigate(R.id.action_splashFragment_to_loginFragment)
                        }
                    }
                }
            } else {
                lifecycleScope.launch {
                    delay(4000)
                    if (navController.currentDestination?.id == R.id.homeFragment) {
                        navController.navigate(R.id.action_homeFragment_to_loginFragment, null, slideRightNavOptions())
                    } else if (navController.currentDestination?.id == R.id.splashFragment) {
                        navController.navigate(R.id.action_splashFragment_to_loginFragment, null, slideRightNavOptions())
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
            } else {
                // check for network here
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver, IntentFilter(MyFirebaseMessagingService.TOKEN_INTENT))
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(MyFirebaseMessagingService.NOTIFICATION_INTENT))

        startNetworkCallback()

    }


    fun onLinkClick(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }


    override fun onResume() {
        super.onResume()
        checkForNetworkPermissions {
            if (it) {
                startNetworkCallback()
            } else {
                //
            }
        }

        if (navController.currentDestination?.id == R.id.emailVerificationFragment) {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val task = currentUser.reload()
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

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModel.setNetworkAvailability(true)
        }

        override fun onLost(network: Network) {
            viewModel.setNetworkAvailability(false)
        }
    }

    private fun startNetworkCallback() {
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
                viewModel.insertChannelWithContributors(chatChannel, users)

                // get messages only after getting all the contributors associated with the project
                getLatestMessagesBaseOnLastMessage(chatChannel)

                // listening for new messages as soon as activity starts
                addChannelListener(chatChannel)
            } else {
                viewModel.setCurrentError(it2.exception)
            }
        }
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
                Log.d(TAG, "Yes the last message is relatively new .. hence downloading all messages after that")
                if (externalImagesDir != null && externalDocumentsDir != null) {
                    viewModel.getLatestMessagesAfter(externalImagesDir, externalDocumentsDir, lastMessage, chatChannel)
                }
            }
        }
    }

    private fun addChannelListener(chatChannel: ChatChannel) = lifecycleScope.launch {

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
                        // insert the new messages
                        viewModel.insertChannelMessages(chatChannel, externalImagesDir, externalDocumentsDir, messages)
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

        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png")

        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
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

    fun selectReportUploadImages() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        selectReportImagesLauncher.launch(intent)
    }

    fun selectMoreProjectImages() {

        val mimeTypes = arrayOf("image/bmp", "image/jpeg", "image/png")

        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        selectMoreProjectImageLauncher.launch(intent)
    }

    companion object {
        private const val TAG = "MainActivityDebugTag"
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

        stopNetworkCallback()
    }

    fun showSnackBar(s: String) {
        Snackbar.make(binding.root, s, Snackbar.LENGTH_LONG).show()
    }

    fun showLoadingDialog(msg: String) {
        val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
        val loadingLayoutBinding = LoadingLayoutBinding.bind(loadingLayout)

        loadingLayoutBinding.loadingText.text = msg

        loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(loadingLayout)
            .setCancelable(false)
            .show()
    }

}
