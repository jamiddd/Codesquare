package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.PostRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.PostRequestViewHolder
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class PostRequestFragment: PagerListFragment<PostRequest, PostRequestViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        getItems {
            viewModel.getPagedPostRequests()
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerNoItemsText.text = getString(R.string.empty_post_requests_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)

    }

    override fun getAdapter(): PagingDataAdapter<PostRequest, PostRequestViewHolder> {
        return PostRequestAdapter()
    }
}