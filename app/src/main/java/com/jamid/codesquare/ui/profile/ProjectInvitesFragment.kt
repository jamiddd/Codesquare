package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ProjectInviteAdapter
import com.jamid.codesquare.data.ProjectInvite
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectInvitesFragment: PagerListFragment<ProjectInvite, ProjectInviteAdapter.ProjectInviteViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        getItems {
            viewModel.getProjectInvites()
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerNoItemsText.text = getString(R.string.empty_project_invites_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)

    }

    override fun getAdapter(): PagingDataAdapter<ProjectInvite, ProjectInviteAdapter.ProjectInviteViewHolder> {
        return ProjectInviteAdapter()
    }

}