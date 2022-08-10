package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.View
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.PostRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.PostRequestViewHolder
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.ui.DefaultPagingFragment
// something simple
class PostRequestFragment: DefaultPagingFragment<PostRequest, PostRequestViewHolder>() {

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerNoItemsText.text = getString(R.string.empty_post_requests_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)

        getItems(viewLifecycleOwner) {
            viewModel.getPagedPostRequests()
        }

        val padding = resources.getDimension(R.dimen.large_padding).toInt()
        binding.pagerItemsRecycler.setPadding(0, 0, 0, padding)

    }

    override fun getPagingAdapter(): PagingDataAdapter<PostRequest, PostRequestViewHolder> {
        return PostRequestAdapter()
    }

    override fun getDefaultInfoText(): String {
        return getString(R.string.empty_post_requests_greet)
    }

}