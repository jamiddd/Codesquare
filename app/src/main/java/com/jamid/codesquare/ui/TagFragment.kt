package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import java.util.*

@ExperimentalPagingApi
class TagFragment: PagerListFragment<Project, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()
        val tag = arguments?.getString("tag", "project").orEmpty()

        val t1 = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag.uppercase()
        val t3 = tag.lowercase()

        val query = Firebase.firestore.collection("projects")
            .whereArrayContainsAny("tags", listOf(tag, t1, t2, t3))

        getItems {
            viewModel.getTagProjects(tag, query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView?.itemAnimator = null

    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

}