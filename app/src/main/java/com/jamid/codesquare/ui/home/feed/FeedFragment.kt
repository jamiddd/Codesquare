package com.jamid.codesquare.ui.home.feed

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class FeedFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection("projects")

        getItems {
            viewModel.getFeedItems(query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView?.itemAnimator = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

}