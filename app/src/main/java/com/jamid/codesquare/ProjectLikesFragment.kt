package com.jamid.codesquare

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.PagingUserAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectLikesFragment: PagerListFragment<User, UserViewHolder>() {
    override fun getAdapter(): PagingDataAdapter<User, UserViewHolder> {
        return PagingUserAdapter(true)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val projectId = arguments?.getString(PROJECT_ID) ?: return

        val query = Firebase.firestore.collection(USERS)
            .whereArrayContains(LIKED_PROJECTS, projectId)

        getItems {
            viewModel.getProjectSupporters(query, projectId)
        }

        shouldShowImage = false

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)

    }
}