package com.jamid.codesquare.ui.home.feed

import android.Manifest
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.isDigitsOnly
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentPagerBinding
import com.jamid.codesquare.listeners.LocationStateListener
import com.jamid.codesquare.ui.DefaultPagingFragment
import kotlinx.coroutines.flow.map
import java.util.*


class FeedFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>(), LocationStateListener {

    companion object {
        private const val TAG = "FeedFragment"
    }

    private var searchInProgress = false
    private var locationBasedSnackBars: Snackbar? = null
    private lateinit var query: Query
    private var isLocationListenerSet = false
    private var lastUsedLocation: Location? = null
    private var isLocationReady = false
    private var counter = 0
    private var tooltipView: View? = null

    /* TODO("Get menu here, refine this")*/
    private fun showCreateItemTooltip(){
        val container = activity.binding.root

        container.removeView(tooltipView)

        val createItem = requireActivity().findViewById<View>(R.id.create_post)
        if (createItem != null) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val createProjectDialogFlag = sharedPref.getBoolean(PREF_CREATE_TOOLTIP, true)
            if (createProjectDialogFlag) {
                tooltipView = showTooltip(
                    "Click here to create a new post", container, createItem,
                    AnchorSide.Bottom
                )
                val editor = sharedPref.edit()
                editor.putBoolean(PREF_CREATE_TOOLTIP, false)
                editor.apply()
            }
        }
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPagerBinding {
        setMenu(R.menu.home_menu, {
            // Handle the menu selection
            when (it.itemId) {
                R.id.search -> {
                    findNavController().navigate(R.id.preSearchFragment)
                }
                R.id.create_post -> {
                    checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) { granted ->
                        if (granted) {
                            findNavController().navigate(R.id.createPostFragment)
                        } else {
                            activity.apply {
                                currentRequest = Manifest.permission.READ_EXTERNAL_STORAGE
                                permissionLauncher.launch(currentRequest)
                            }

                            viewModel.readPermission.observe(viewLifecycleOwner) { enabled ->
                                if (enabled) {
                                    findNavController().navigate(R.id.createPostFragment)
                                }
                            }
                        }
                    }
                }
            }
            true
        }) {
            runDelayed(2000) {
                showCreateItemTooltip()
            }
        }
        return super.onCreateBinding(inflater)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPagerBinding.bind(view)

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            findNavController().navigate(R.id.action_feedFragment_to_navigation_auth)
        } else {
            FireUtility.getUser(mAuth.currentUser!!.uid) {
                if (it != null) {
                    UserManager.updateUser(it)
                } else {
                    findNavController().navigate(R.id.action_feedFragment_to_navigation_auth)
                }
            }
        }

        binding.pagerItemsRecycler.apply {
            val bottomPadding = resources.getDimension(R.dimen.large_padding).toInt()
            setPadding(0, 0, 0, bottomPadding * 2)
        }

        var query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)

        if (viewModel.feedOption.value == null) {
            getItems(viewLifecycleOwner) {
                viewModel.getFeedItems(query).map {
                    it.map { p ->
                        if (p.isAd) {
                            Post2.Advertise(p.id)
                        } else {
                            Post2.Collab(p)
                        }
                    }
                }
            }
        }


        viewModel.feedOption.observe(viewLifecycleOwner) { feedOption ->
            if (feedOption != null) {

                query = Firebase.firestore.collection(POSTS)
                    .whereEqualTo(ARCHIVED, false)

                val tag = feedOption.filter
                if (tag != null && tag != getString(R.string.random)) {
                    val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    val t2 = tag.uppercase()
                    val t3 = tag.lowercase()

                    query = query.whereArrayContainsAny(TAGS, listOf(tag, t1, t2, t3))
                }

                val order = when (feedOption.order) {
                    FeedOrder.ASC -> Query.Direction.ASCENDING
                    FeedOrder.DESC -> Query.Direction.DESCENDING
                }

                var shouldInitiate = true

                when (feedOption.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        query = query.orderBy(CONTRIBUTORS_COUNT, order)
                    }
                    FeedSort.LIKES -> {
                        query = query.orderBy(LIKES_COUNT, order)
                    }
                    FeedSort.MOST_VIEWED -> {
                        query = query.orderBy(VIEWS_COUNT, order)
                    }
                    FeedSort.MOST_RECENT -> {
                        //
                    }
                    FeedSort.LOCATION -> {

                        shouldInitiate = false

                        if (!isLocationListenerSet){
                            activity.attachFragmentWithLocationListener(this)
                            binding.pagerRefresher.setOnRefreshListener {
                                if (lastUsedLocation != null) {
                                    searchBasedOnLocation(GeoLocation(lastUsedLocation!!.latitude, lastUsedLocation!!.longitude), tag)
                                }
                            }
                        } else {
                            if (isLocationReady) {
                                if (lastUsedLocation != null) {
                                    searchBasedOnLocation(GeoLocation(lastUsedLocation!!.latitude, lastUsedLocation!!.longitude), tag)

                                    binding.pagerRefresher.setOnRefreshListener {
                                        searchBasedOnLocation(GeoLocation(lastUsedLocation!!.latitude, lastUsedLocation!!.longitude), tag)
                                    }
                                }
                            } else {
                                toast("Error:location not ready")
                            }
                        }
                        binding.pagerNoItemsText.text = getString(R.string.no_location_posts)
                    }
                }

                if (shouldInitiate) {

                    binding.pagerNoItemsText.text = "No posts found"

                    val s = if (tag == "Random" || tag == null) {
                        null
                    } else {
                        tag
                    }

                    getItems(viewLifecycleOwner) {
                        viewModel.getFeedItems(query, s).map {
                            it.map { p ->
                                if (p.isAd) {
                                    Post2.Advertise(p.id)
                                } else {
                                    Post2.Collab(p)
                                }
                            }
                        }
                    }

                    binding.pagerRefresher.setOnRefreshListener {
                        getItems(viewLifecycleOwner) {
                            viewModel.getFeedItems(query, s).map {
                                it.map { p ->
                                    if (p.isAd) {
                                        Post2.Advertise(p.id)
                                    } else {
                                        Post2.Collab(p)
                                    }
                                }
                            }
                        }
                        binding.pagerRefresher.isRefreshing = false
                    }
                }
            }
        }



        viewModel.isNewPostCreated.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    runDelayed(300) {
                        Snackbar.make(
                            activity.binding.root,
                            "Post uploaded successfully.",
                            Snackbar.LENGTH_LONG
                        ).setAnchorView(activity.binding.mainPrimaryBottom).show()
                        binding.pagerItemsRecycler.scrollToPosition(0)
                    }
                    viewModel.setCreatedNewPost(false)
                }
            }
        }

        viewModel.updatedOldPost.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    runDelayed(300) {
                        Snackbar.make(
                            activity.binding.root,
                            "Post updated successfully",
                            Snackbar.LENGTH_LONG
                        ).setAnchorView(activity.binding.mainPrimaryBottom).show()
                    }
                    viewModel.setUpdatedOldPost(false)
                }
            }
        }

        binding.pagerItemsRecycler.post {
            runDelayed(2000) {
                onSplashFinished()
            }
        }

    }

    private fun onSplashFinished() {
        activity.splashFragment?.let {
            activity.supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
        activity.splashFragment = null
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun searchBasedOnLocation(geoLocation: GeoLocation, tag: String? = null) {
        if (!searchInProgress) {
            searchInProgress = true

            val center = GeoLocation(geoLocation.latitude, geoLocation.longitude)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            val radius = sharedPreferences.getString(LOCATION_RADIUS, ONE)
            val radiusInMeters = if (radius != null && radius != ONE && radius.isDigitsOnly()) {
                radius.toInt() } else { 1 } * 1000

            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters.toDouble())
            val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
            for (b in bounds) {
                query = Firebase.firestore.collection(POSTS)

                if (tag != null && tag != getString(R.string.random)) {
                    val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    val t2 = tag.uppercase()
                    val t3 = tag.lowercase()

                    query = query.whereArrayContainsAny(TAGS, listOf(tag, t1, t2, t3))
                }

                query = query.orderBy("location.geoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash)
                    .limit(50)

                tasks.add(query.get())
            }

            // Collect all the query results together into a single list
            Tasks.whenAllComplete(tasks)
                .addOnCompleteListener {

                    binding.pagerRefresher.isRefreshing = false

                    searchInProgress = false

                    val matchingDocs = mutableListOf<DocumentSnapshot>()
                    for (task in tasks) {
                        val snap = task.result
                        for (doc in snap.documents) {
                            val lat = doc.getDouble("location.latitude")!!
                            val lng = doc.getDouble("location.longitude")!!

                            // We have to filter out a few false positives due to GeoHash
                            // accuracy, but most will match
                            val docLocation = GeoLocation(lat, lng)
                            val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                            if (distanceInM <= radiusInMeters) {
                                matchingDocs.add(doc)
                            }
                        }
                    }

                    val posts = Array(matchingDocs.size) { Post() }
                    matchingDocs.forEachIndexed { i, d ->
                        val post = d.toObject(Post::class.java)!!
                        post.isNearMe = true
                        posts[i] = post
                    }

                    viewModel.insertPosts(*posts)
                }

            getItems(viewLifecycleOwner) {
                viewModel.getPostsNearMe().map { it.map { p -> if (p.isAd) {
                    Post2.Advertise(p.id)
                } else {
                    Post2.Collab(p)
                } } }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onSplashFinished()
        activity.detachFragmentFromLocationListener()
    }

    override fun onLocationSettingsReady() {
        isLocationReady = true
        binding.pagerRefresher.isRefreshing = true
    }

    override fun onLocationTurnOnRequestRejected() {
        updateUiOnError("Cannot get posts near you because you have not turned on location. To turn on location go to settings.")
        isLocationReady = false
    }

    override fun onLastLocationReceived(lastLocation: Location) {
        lastUsedLocation = lastLocation
        query = Firebase.firestore.collection(POSTS).whereEqualTo(ARCHIVED, false)
        val tag = viewModel.feedOption.value?.filter
        searchBasedOnLocation(GeoLocation(lastLocation.latitude, lastLocation.longitude), tag)
        isLocationReady = false
    }

    override fun onPromptError(exception: IntentSender.SendIntentException) {
        updateUiOnError("Something went wrong while trying to show location turn on request.")
        isLocationReady = false
    }

    override fun onLocationPermissionRequestRejected() {
        isLocationReady = false
        activity.onLocationPermissionRequestRejected()

        updateUiOnError("Cannot get posts near you because you have denied location permissions.")
    }

    private fun updateUiOnError(msg: String) {
        viewModel.setDefaultFeedOption()
        locationBasedSnackBars?.dismiss()
        locationBasedSnackBars = Snackbar.make(binding.root, msg, Snackbar.LENGTH_INDEFINITE)
        locationBasedSnackBars?.show()
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(viewLifecycleOwner, activity)
    }

    override fun onPagingDataChanged(itemCount: Int) {
        //
    }

    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {
        //
    }

    override fun getDefaultInfoText(): String {
        return "No posts at the moment"
    }

    override fun onPause() {
        super.onPause()
        counter = 0
    }

}