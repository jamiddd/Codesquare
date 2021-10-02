package com.jamid.codesquare.ui

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.LocationItemClickListener
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.ActivityMainBinding
import com.jamid.codesquare.listeners.ProjectClickListener
import com.jamid.codesquare.listeners.ProjectRequestListener

class MainActivity: AppCompatActivity(), LocationItemClickListener, ProjectClickListener, ProjectRequestListener {

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
                    userInfoLayout?.updateLayout(marginTop = convertDpToPx(70))
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
            }
        }

        viewModel.currentUser.observe(this) {
            if (it != null) {
                Log.d(TAG, it.toString())
            } else {
                Log.d(TAG, "No user.")
            }
        }

        viewModel.currentError.observe(this) { exception ->
            if (exception != null) {
                toast(exception.localizedMessage.orEmpty())
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
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        selectProjectImageLauncher.launch(intent)
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

    override fun onProjectRequestAccept(projectRequest: ProjectRequest) {
        viewModel.acceptRequest(projectRequest)
    }

    override fun onProjectRequestCancel(projectRequest: ProjectRequest) {
        viewModel.rejectRequest(projectRequest)
    }

}