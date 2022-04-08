package com.jamid.codesquare.ui.home.feed

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.data.AnchorSide
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.TagsContainerBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*
import kotlin.collections.ArrayList


@ExperimentalPagingApi
class FeedFragment: PagerListFragment<Project, PostViewHolder>() {

    init {
        shouldHideRecyclerView = true
    }

    private var tooltipView: View? = null

    @SuppressLint("InflateParams")
    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(PROJECTS)
        getItems { viewModel.getFeedItems(query) }

        binding.root.layoutTransition = LayoutTransition()

        binding.pagerItemsRecycler.itemAnimator = null

        binding.root.removeView(binding.noDataImage)

        (activity as MainActivity).networkManager.networkAvailability.observe(viewLifecycleOwner) { isNetworkAvailable ->
            if (!isNetworkAvailable) {
                binding.pagerNoItemsText.text = getString(R.string.network_problem_warning)
            } else {
                binding.pagerNoItemsText.text = getString(R.string.no_projects_info)
            }
        }

        val tagsContainerView = layoutInflater.inflate(R.layout.tags_container, null, false)
        val tagsContainerBinding = TagsContainerBinding.bind(tagsContainerView)

        OverScrollDecoratorHelper.setUpOverScroll(tagsContainerBinding.tagsContainer)

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
        val genericLength = resources.getDimension(R.dimen.generic_len).toInt()
        binding.pagerItemsRecycler.setPadding(0, touchLength, 0, genericLength)

        setRandomButton(query, tagsContainerBinding.random)

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

    }

    private fun showFilterTagsTooltip(tagsContainer: View) = viewLifecycleOwner.lifecycleScope.launch {
        val container = (activity as MainActivity).binding.root

        delay(1500)

        container.removeView(tooltipView)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val feedTagsDialogFlag = sharedPref.getBoolean(PREF_PROJECT_TAGS, true)
        if (feedTagsDialogFlag) {
            tooltipView = showTooltip("Filter projects by tags", container, tagsContainer, AnchorSide.Bottom)

            val editor = sharedPref.edit()
            editor.putBoolean(PREF_PROJECT_TAGS, false)
            editor.apply()
        }
    }

    private fun setLocationButton(locationBtn: Chip) {

        locationBtn.apply {
            isCheckable = false
            isCloseIconVisible = false
        }

        val a = activity as MainActivity

        locationBtn.setOnClickListener {

            binding.pagerNoItemsText.text = getString(R.string.no_location_projects)

            // firstly always check if the location is on
            if (!LocationProvider.isLocationEnabled(requireContext())) {
                Snackbar.make(binding.root, "Location is not enabled", Snackbar.LENGTH_LONG).setAction("Enable") {
                    LocationProvider.checkForLocationSettings(requireContext(), a.locationStateLauncher, a.fusedLocationProviderClient)
                }.show()
            } else {
                val currentUser = UserManager.currentUser
                val currentUserLocation = currentUser.location
                searchBasedOnLocation(GeoLocation(currentUserLocation.latitude, currentUserLocation.longitude))
            }
        }
    }

    private fun setRandomButton(query: CollectionReference, random: Chip) {

        random.setOnClickListener {

            binding.pagerNoItemsText.text = getString(R.string.no_projects)

            viewModel.disableLocationBasedProjects()
            getItems {
                viewModel.getFeedItems(query)
            }
        }

        random.apply {
            isCheckable = false
            isCloseIconVisible = false
        }


    }

    private fun searchBasedOnLocation(geoLocation: GeoLocation) {
        val center = GeoLocation(geoLocation.latitude, geoLocation.longitude)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val radius = sharedPreferences.getString(LOCATION_RADIUS, ONE)
        val radiusInMeters = if (radius != null && radius != ONE && radius.isDigitsOnly()) {
            radius.toInt() } else { 1 } * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters.toDouble())
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = Firebase.firestore.collection(PROJECTS)
                .orderBy("location.geoHash")
                .startAt(b.startHash)
                .endAt(b.endHash)
                .limit(50)
            tasks.add(q.get())
        }

        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
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

                val projects = Array(matchingDocs.size) { Project() }
                matchingDocs.forEachIndexed { i, d ->
                    val project = d.toObject(Project::class.java)!!
                    project.isNearMe = true
                    projects[i] = project
                }

                viewModel.insertProjects(*projects)
            }

        getItems {
            viewModel.getProjectsNearMe()
        }
    }

    private fun ChipGroup.addUpdateInterestButton() {
        val chip = Chip(requireContext())
        chip.text = requireContext().getString(R.string.update_interests)
        chip.isCheckable = false

        chip.isCloseIconVisible = false

        chip.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_editProfileFragment, null)
        }

        addView(chip)

    }

    private fun ChipGroup.addTag(tag: String) {

        tag.trim()
        val lContext = requireContext()
        val chip = Chip(lContext)

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        chip.apply {
            isCheckable = true
            text = tag
            isCloseIconVisible = false
            addView(this)

            setOnClickListener {
                val noItemsText = "No projects related to $tag"
                binding.pagerNoItemsText.text = noItemsText

                viewModel.disableLocationBasedProjects()

                val query = Firebase.firestore.collection(PROJECTS)
                    .whereArrayContainsAny("tags", listOf(tag, t1, t2, t3))

                getItems {
                    viewModel.getFeedItems(query, tag)
                }
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

}