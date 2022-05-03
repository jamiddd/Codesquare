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

        /*binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(View.FOCUS_DOWN)) {

                    if (!recyclerView.canScrollVertically(View.FOCUS_UP)) {
                        // either there's no posts, or 1 post
                    } else {
                        activity.binding.mainPrimaryBtn.hide()
                    }

                } else {
                    activity.binding.mainPrimaryBtn.show()
                }
            }
        })*/

        val largeBottomPadding = resources.getDimension(R.dimen.extra_comfort_len)
        binding.pagerItemsRecycler.setPadding(0, 0, 0, largeBottomPadding.toInt())

//        binding.root.removeView(binding.noDataImage)

       /* val tagsContainerView = layoutInflater.inflate(R.layout.tags_container, null, false)
        tagsContainerBinding = TagsContainerBinding.bind(tagsContainerView)*/

       /* OverScrollDecoratorHelper.setUpOverScroll(tagsContainerBinding.tagsContainer)

        tagsContainerBinding.tagsContainer.setOnScrollChangeListener { _, _, _, _, _ ->
            if (tagsContainerBinding.tagsContainer.canScrollHorizontally(1)) {
                tagsContainerBinding.nextTagBtn.show()
            } else {
                tagsContainerBinding.nextTagBtn.hide()
            }

            if (tagsContainerBinding.tagsContainer.canScrollHorizontally(-1)) {
                tagsContainerBinding.prevTagBtn.show()
            } else {
                tagsContainerBinding.prevTagBtn.hide()
            }
        }

        tagsContainerBinding.nextTagBtn.setOnClickListener {
            tagsContainerBinding.tagsContainer.smoothScrollTo(tagsContainerBinding.tagsContainer.scrollX + 380, tagsContainerBinding.tagsContainer.scrollY)
        }

        tagsContainerBinding.prevTagBtn.setOnClickListener {
            tagsContainerBinding.tagsContainer.smoothScrollTo(tagsContainerBinding.tagsContainer.scrollX - 380, tagsContainerBinding.tagsContainer.scrollY)
        }

        binding.pagerRoot.addView(tagsContainerBinding.root)

        tagsContainerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToStart = binding.pagerRoot.id
            topToTop = binding.pagerRoot.id
            endToEnd = binding.pagerRoot.id
            height = ConstraintLayout.LayoutParams.WRAP_CONTENT
            width = ConstraintLayout.LayoutParams.MATCH_PARENT
        }

        val touchLength = resources.getDimension(R.dimen.touch_len).toInt()
        val bottomPadding = resources.getDimension(R.dimen.generic_len).toInt() * 10
        binding.pagerItemsRecycler.setPadding(0, touchLength, 0, bottomPadding)

        setRandomButton(tagsContainerBinding.random)

        setLocationButton(tagsContainerBinding.nearMe)

        viewModel.currentUser.observe(viewLifecycleOwner) {
            if (it != null) {

                tagsContainerBinding.tagsHolder.removeViews(2, tagsContainerBinding.tagsHolder.childCount - 2)

                if (it.interests.isEmpty()) {
                    tagsContainerBinding.nextTagBtn.hide()
                    tagsContainerBinding.prevTagBtn.hide()
                    tagsContainerBinding.tagsHolder.addUpdateInterestButton()
                } else {
                    tagsContainerBinding.prevTagBtn.hide()
                    tagsContainerBinding.nextTagBtn.show()
                    for (interest in it.interests) {
                        tagsContainerBinding.tagsHolder.addTag(interest)
                    }
                }

                if (isEmpty.value == true) {
                    pagingAdapter.refresh()
                }

            }
        }

        showFilterTagsTooltip(tagsContainerView)

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)

        val screenWidth = getWindowWidth()

        binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = (recyclerView.layoutManager as LinearLayoutManager)
                    val pos = layoutManager.findFirstCompletelyVisibleItemPosition()

                    if (pos > 4) {
                        // that means the user has scrolled way bottom for the first time
                        if (toolbar != null)
                            showToolbarClickTooltip(toolbar)

                    }

                    val checkedId = tagsContainerBinding.tagsHolder.checkedChipId
                    val v = tagsContainerBinding.tagsHolder.findViewById<View>(checkedId)
                    if (v != null) {
                        val location = intArrayOf(0, 0)
                        v.getLocationInWindow(location)

                        val x1 = location[0]
                        val y1 = location[1]
                        when {
                            x1 < 0 -> {
                                // left side of the screen
                                tagsContainerBinding.tagsContainer.smoothScrollTo(x1, y1)
                            }
                            x1 > screenWidth -> {
                                // right side of the screen
                                tagsContainerBinding.tagsContainer.smoothScrollTo(x1, y1)
                            }
                            else -> {
                                // on the screen
                            }
                        }

                    }
                }
            }
        })*/

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

   /* private fun showToolbarClickTooltip(toolbar: View) {

        val container = activity.binding.root
        container.removeView(tooltipView)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val scrollToTopDialogFlag = sharedPref.getBoolean(PREF_SCROLL_TOP, true)

        if (scrollToTopDialogFlag) {
            tooltipView = showTooltip("Click on toolbar to scroll to top again", container, toolbar, AnchorSide.Bottom)

            val editor = sharedPref.edit()
            editor.putBoolean(PREF_SCROLL_TOP, false)
            editor.apply()
        }

    }

    private fun showFilterTagsTooltip(tagsContainer: View) = viewLifecycleOwner.lifecycleScope.launch {

        delay(1500)

        requireActivity().runOnUiThread {
            val container = activity.binding.root
            container.removeView(tooltipView)

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val feedTagsDialogFlag = sharedPref.getBoolean(PREF_POST_TAGS, true)
            if (feedTagsDialogFlag) {
                tooltipView = showTooltip("Filter posts by tags", container, tagsContainer, AnchorSide.Bottom)

                val editor = sharedPref.edit()
                editor.putBoolean(PREF_POST_TAGS, false)
                editor.apply()
            }
        }
    }*/

   /* private fun setLocationButton(locationBtn: Chip) {

        locationBtn.apply {
            isCloseIconVisible = false
        }

        locationBtn.setOnClickListener {
            if (!isLocationListenerSet) {
                isLocationListenerSet = true
                activity.attachFragmentWithLocationListener(this)
            }
            binding.pagerNoItemsText.text = getString(R.string.no_location_posts)
        }
    }

    private fun setRandomButton(random: Chip) {

        random.setOnClickListener {

            binding.pagerNoItemsText.text = getString(R.string.no_posts)

            viewModel.disableLocationBasedPosts()

            getItems {
                viewModel.getFeedItems(query)
            }
        }

        random.apply {
            isCloseIconVisible = false
        }


    }*/

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

   /* private fun ChipGroup.addUpdateInterestButton() {
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.text = requireContext().getString(R.string.update_interests)
        chip.isCheckable = false
        chip.isCloseIconVisible = false

        chip.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment, null)
        }

        addView(chip)

    }*/

   /* private fun ChipGroup.addTag(tag: String) {

        tag.trim()
        val lContext = requireContext()
        val chip = View.inflate(lContext, R.layout.choice_chip, null) as Chip

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        chip.apply {
            isCheckable = true
            text = tag
            isCloseIconVisible = false
            addView(this)

            setOnClickListener {
                val noItemsText = "No posts related to $tag"
                binding.pagerNoItemsText.text = noItemsText

                viewModel.disableLocationBasedPosts()

                val query = Firebase.firestore.collection(POSTS)
                    .whereEqualTo(ARCHIVED, false)
                    .whereArrayContainsAny("tags", listOf(tag, t1, t2, t3))
                    .orderBy("viewsCount", Query.Direction.DESCENDING)

                getItems {
                    viewModel.getFeedItems(query, tag)
                }
            }
        }
    }*/

    companion object {

        private const val TAG = "FeedFragment"

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
        // fallback from location based search
//        tagsContainerBinding.random.performClick()
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