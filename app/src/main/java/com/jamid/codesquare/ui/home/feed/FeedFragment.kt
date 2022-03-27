package com.jamid.codesquare.ui.home.feed

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
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
import com.jamid.codesquare.ui.home.HomeFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

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

        var oldPosition = 0

        binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val newPosition = oldPosition + dy

                val diff = abs(newPosition - oldPosition)

                if (diff > SCROLL_THRESHOLD) {
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

        val container = (activity as MainActivity).binding.root

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            container.removeView(tooltipView)

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val feedTagsDialogFlag = sharedPref.getBoolean("feed_fragment_tags", true)
            if (feedTagsDialogFlag) {
                tooltipView = showTooltip("Filter projects by tags", container, tagsContainerView, HomeFragment.AnchorSide.Bottom)

                val editor = sharedPref.edit()
                editor.putBoolean("feed_fragment_tags", false)
                editor.apply()
            }

        }


    }

    private fun setLocationButton(locationBtn: Chip) {

        locationBtn.apply {
            isCheckable = false
            isCloseIconVisible = false
        }

        val a = activity as MainActivity

        locationBtn.setOnClickListener {

            binding.pagerNoItemsText.text = "No projects near you"

            // firstly always check if the location is on
            if (!LocationProvider.isLocationEnabled(requireContext())) {
                Snackbar.make(binding.root, "Location is not enabled", Snackbar.LENGTH_LONG).setAction("Enable") {
                    LocationProvider.checkForLocationSettings(requireContext(), a.locationStateLauncher, a.fusedLocationProviderClient)
                }.show()
            } else {
                val currentUser = UserManager.currentUser
                val currentUserLocation = currentUser.location
                if (currentUserLocation != null) {
                    searchBasedOnLocation(GeoLocation(currentUserLocation.latitude, currentUserLocation.longitude))
                } else {
                    val tempLocation = LocationProvider.currentLocation
                    if (tempLocation != null) {
                        searchBasedOnLocation(GeoLocation(tempLocation.latitude, tempLocation.longitude))
                    } else {
                        LocationProvider.getLastLocation(a.fusedLocationProviderClient)
                    }
                }
            }
        }
    }

    private fun setRandomButton(query: CollectionReference, random: Chip) {

        random.setOnClickListener {

            binding.pagerNoItemsText.text = "No projects"

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

      /*  val (backgroundColor, textColor) = if (isNightMode()) {
            val colorPair = colorPalettesNight.random()
            ContextCompat.getColor(lContext, colorPair.first) to
                    ContextCompat.getColor(lContext, colorPair.second)
        } else {
            val colorPair = colorPalettesDay.random()
            ContextCompat.getColor(lContext, colorPair.first) to
                    ContextCompat.getColor(lContext, colorPair.second)
        }*/

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        chip.apply {
         /*   val length = resources.getDimension(R.dimen.unit_len) / 4
            chipStrokeWidth = length*/
            isCheckable = true
            text = tag
//            chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            isCloseIconVisible = false
            addView(this)

//            setTextColor(textColor)

            setOnClickListener {

                binding.pagerNoItemsText.text = "No projects related to $tag"

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

        private const val SCROLL_THRESHOLD = 60

        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

}