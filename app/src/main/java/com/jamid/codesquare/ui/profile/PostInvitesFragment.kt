package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.View
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.PostInviteAdapter
import com.jamid.codesquare.data.PostInvite
import com.jamid.codesquare.ui.DefaultPagingFragment
// something simple
class PostInvitesFragment: DefaultPagingFragment<PostInvite, PostInviteAdapter.PostInviteViewHolder>() {

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.noDataImage.setAnimation(R.raw.empty_notification)

        getItems(viewLifecycleOwner) {
            viewModel.getPostInvites()
        }

        val padding = resources.getDimension(R.dimen.large_padding).toInt()
        binding.pagerItemsRecycler.setPadding(0, 0, 0, padding)

    }

    override fun getDefaultInfoText(): String {
        return getString(R.string.empty_post_invites_greet)
    }

    override fun getPagingAdapter(): PagingDataAdapter<PostInvite, PostInviteAdapter.PostInviteViewHolder> {
        return PostInviteAdapter()
    }

}