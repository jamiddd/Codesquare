package com.jamid.codesquare.ui.home.feed

import android.annotation.SuppressLint
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.TagsContainerBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

@ExperimentalPagingApi
class FeedFragment: PagerListFragment<Project, PostViewHolder>() {

    init {
        shouldHideRecyclerView = true
    }

    @SuppressLint("InflateParams")
    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(PROJECTS)
        val currentUser = UserManager.currentUser
        getItems { viewModel.getFeedItems(query) }

        binding.pagerItemsRecycler.itemAnimator = null

        val tagsContainerView = layoutInflater.inflate(R.layout.tags_container, null, false)
        val tagsContainerBinding = TagsContainerBinding.bind(tagsContainerView)

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
        val params = tagsContainerBinding.root.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.pagerRoot.id
        params.topToTop = binding.pagerRoot.id
        params.endToEnd = binding.pagerRoot.id
        tagsContainerBinding.root.layoutParams = params

        tagsContainerView.updateLayout(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        recyclerView?.setPadding(0, convertDpToPx(48), 0, convertDpToPx(8))

        tagsContainerBinding.random.setOnClickListener {
            getItems {
                viewModel.getFeedItems(query)
            }
        }

        tagsContainerBinding.tagsHolder.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.near_me) {
                val currentUserLocation = currentUser.location
                if (currentUserLocation != null) {
                    searchBasedOnLocation(GeoLocation(currentUserLocation.latitude, currentUserLocation.longitude))
                } else {
                    val tempLocation = LocationProvider.currentLocation
                    if (tempLocation != null) {
                        searchBasedOnLocation(GeoLocation(tempLocation.latitude, tempLocation.longitude))
                    } else {
                        LocationProvider.getLastLocation((activity as MainActivity).fusedLocationProviderClient)
                    }
                }
            }
        }

        var oldPosition = 0

        binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val newPosition = oldPosition + dy

                if (newPosition > oldPosition) {
                    if (tagsContainerView.isVisible) {
                        tagsContainerView.hide()
                    }
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(500)
                        if (!tagsContainerView.isVisible) {
                            tagsContainerView.show()
                        }
                    }
                }

                oldPosition = newPosition
            }
        })

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

    }

    private fun searchBasedOnLocation(geoLocation: GeoLocation) {
        toast("Showing projects near you...")

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
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCheckable = true
        chip.isCloseIconVisible = false

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        chip.setOnClickListener {
            val query = Firebase.firestore.collection("projects")
                .whereArrayContainsAny("tags", listOf(tag, t1, t2, t3))

            getItems {
                viewModel.getFeedItems(query, tag)
            }
        }

        addView(chip)

    }

    companion object {
        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

}