package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class CollaborationsFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val otherUser = arguments?.getParcelable<User>("user")

        setIsViewPagerFragment(true)

        if (otherUser == null) {
            val currentUser = viewModel.currentUser.value!!

            val query = Firebase.firestore.collection("projects")
                .whereArrayContains("contributors", currentUser.id)

            getItems {
                viewModel.getCollaborations(query)
            }
        } else {

            val query = Firebase.firestore.collection("projects")
                .whereArrayContains("contributors", otherUser.id)

            getItems {
                viewModel.getOtherUserCollaborations(query, otherUser)
            }

        }

        binding.pagerNoItemsText.text = "No collaborations"

//        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

    companion object {

        fun newInstance(user: User? = null) = CollaborationsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("user", user)
            }
        }

    }

}