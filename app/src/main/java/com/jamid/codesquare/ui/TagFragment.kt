package com.jamid.codesquare.ui

import android.os.Bundle
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.PROJECT
import com.jamid.codesquare.PROJECTS
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.data.Project
import java.util.*

@ExperimentalPagingApi
class TagFragment: PagerListFragment<Project, PostViewHolder>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()
        val tag = arguments?.getString("tag", PROJECT).orEmpty()

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        val query = Firebase.firestore.collection(PROJECTS)
            .whereArrayContainsAny("tags", listOf(tag, t1, t2, t3))

        getItems {
            viewModel.getTagProjects(tag, query)
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerItemsRecycler.itemAnimator = null

    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

    companion object {

        const val TAG = "TagFragment"

        fun newInstance(bundle: Bundle) = TagFragment().apply {
            arguments = bundle
        }
    }

}