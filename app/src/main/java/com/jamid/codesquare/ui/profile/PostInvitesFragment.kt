package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.PostInviteAdapter
import com.jamid.codesquare.data.PostInvite
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class PostInvitesFragment: PagerListFragment<PostInvite, PostInviteAdapter.PostInviteViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        getItems {
            viewModel.getPostInvites()
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerNoItemsText.text = getString(R.string.empty_post_invites_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)

    }

    override fun getAdapter(): PagingDataAdapter<PostInvite, PostInviteAdapter.PostInviteViewHolder> {
        return PostInviteAdapter()
    }

}