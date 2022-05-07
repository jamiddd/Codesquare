package com.jamid.codesquare.ui.home.feed

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.IntentSender
import android.location.Location
import androidx.core.animation.doOnEnd
import androidx.core.text.isDigitsOnly
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.FeedOption
import com.jamid.codesquare.data.FeedOrder.ASC
import com.jamid.codesquare.data.FeedOrder.DESC
import com.jamid.codesquare.data.FeedSort
import com.jamid.codesquare.data.FeedSort.*
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.listeners.LocationStateListener
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalPagingApi
class FeedFragment: PagerListFragment<Post, SuperPostViewHolder>(), LocationStateListener {

    init {
        shouldHideRecyclerView = true
    }

    private var searchInProgress = false
    private var locationBasedSnackBars: Snackbar? = null
    private lateinit var query: Query

    private var isLocationListenerSet = false
    private var lastUsedLocation: Location? = null

    private var isLocationReady = false

    @SuppressLint("InflateParams")
    override fun onViewLaidOut() {
        super.onViewLaidOut()

        query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)

        binding.root.layoutTransition = LayoutTransition()

        binding.pagerItemsRecycler.itemAnimator = null

        val largeBottomPadding = resources.getDimension(R.dimen.extra_comfort_len)
        binding.pagerItemsRecycler.setPadding(0, 0, 0, largeBottomPadding.toInt())

        binding.pagerItemsRecycler.doOnLayout {
            if (!activity.initialLoadWaitFinished) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1000)
                    activity.runOnUiThread {
                        activity.initialLoadWaitFinished = true
                        activity.binding.mainPrimaryBtn.show()
                    }
                }
            }
        }

        val defaultSetting = viewModel.feedOption.value

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isSettingsRemembered = pref.getBoolean("is_settings_remembered", false)
        if (isSettingsRemembered) {
            val filter = pref.getString("feed_filter", null)
            val sort = pref.getString("feed_sort", getString(R.string.sort_time))!!
            val order = pref.getString("feed_order", "desc")!!

            val oldSetting = FeedOption(filter, getSortFromString(sort), getOrderFromString(order))
            if (defaultSetting != oldSetting) {
                // we need to use old setting
                viewModel.setCurrentFeedOption(oldSetting)
            } else {
                // if default setting is same as old setting no need to anything
            }
        } else {
            // if there is no old setting no need to do anything
        }

        viewModel.isNewPostCreated.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    binding.notifyChip.show()
                    // 100dp
                    val dy = resources.getDimension(R.dimen.extra_comfort_len)
                    binding.notifyChip.slideReset()

                    binding.notifyChip.setOnClickListener {
                        // scroll to top
                        activity.findViewById<RecyclerView>(R.id.pager_items_recycler)?.smoothScrollToPosition(0)

                        val anim = binding.notifyChip.slideUp(dy)
                        anim.doOnEnd {
                            binding.notifyChip.hide()
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
                    ASC -> Query.Direction.ASCENDING
                    DESC -> Query.Direction.DESCENDING
                }

                var shouldInitiate = true

                when (feedOption.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        query = query.orderBy(CONTRIBUTORS_COUNT, order)
                    }
                    LIKES -> {
                        query = query.orderBy(LIKES_COUNT, order)
                    }
                    MOST_VIEWED -> {
                        query = query.orderBy(VIEWS_COUNT, order)
                    }
                    MOST_RECENT -> {
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

                    getItems {
                        viewModel.getFeedItems(query, s)
                    }

                    binding.pagerRefresher.setOnRefreshListener {
                        getItems {
                            viewModel.getFeedItems(query, s)
                        }
                    }
                }
            }
        }

        /*

        FUTURE IMPLEMENTATION

        viewModel.currentQuery.observe(viewLifecycleOwner){ currentQuery ->
            if (currentQuery != null) {
                getItems {
                    viewModel.getFeedItems(currentQuery)
                }
            }
        }*/

    }

    private fun searchBasedOnLocation(geoLocation: GeoLocation, tag: String? = null) {
        if (!searchInProgress){
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

            getItems {
                viewModel.getPostsNearMe()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Post, SuperPostViewHolder> {
        return PostAdapter()
    }

    override fun onLocationSettingsReady() {
        isLocationReady = true
        binding.pagerRefresher.isRefreshing = true
        /* there is a possibility that onLastLocationReceived be called twice */
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


    override fun onDestroyView() {
        super.onDestroyView()
        activity.detachFragmentFromLocationListener()
    }

}