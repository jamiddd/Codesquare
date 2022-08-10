package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.core.text.isDigitsOnly
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.LOCATION_RADIUS
import com.jamid.codesquare.ONE
import com.jamid.codesquare.POSTS
import com.jamid.codesquare.SUB_TITLE
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Post2
import kotlinx.coroutines.flow.map
// something simple
class LocationPostsFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val location = arguments?.getParcelable<Location>("location") ?: return

        activity.binding.mainToolbar.subtitle = arguments?.getString(SUB_TITLE)

        searchBasedOnLocation(GeoLocation(location.latitude, location.longitude))

        getLocationPosts()
    }

    private fun searchBasedOnLocation(geoLocation: GeoLocation) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val radius = sharedPreferences.getString(LOCATION_RADIUS, ONE)
        val radiusInMeters = if (radius != null && radius != ONE && radius.isDigitsOnly()) {
            radius.toInt() } else { 1 } * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(geoLocation, radiusInMeters.toDouble())
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = Firebase.firestore.collection(POSTS)
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
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, geoLocation)
                        if (distanceInM <= radiusInMeters) {
                            matchingDocs.add(doc)
                        }
                    }
                }

                val projects = Array(matchingDocs.size) { Post() }
                matchingDocs.forEachIndexed { i, d ->
                    val project = d.toObject(Post::class.java)!!
                    project.isNearMe = true
                    projects[i] = project
                }

                viewModel.insertPosts(*projects)
            }

        getLocationPosts()

    }

    @OptIn(ExperimentalPagingApi::class)
    private fun getLocationPosts() {
        getItems(viewLifecycleOwner) {
            viewModel.getPostsNearMe().map {
                it.map { it1 ->
                    Post2.Collab(it1)
                }
            }
        }
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(viewLifecycleOwner, activity)
    }

    override fun getDefaultInfoText(): String {
        return "No posts near this location"
    }

    companion object {
        private const val TAG = "LocationPostsFrag"
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disableLocationBasedPosts()
    }
}