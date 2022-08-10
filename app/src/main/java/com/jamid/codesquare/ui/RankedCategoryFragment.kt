package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.POSTS
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.Post2

class RankedCategoryFragment(private val category: String): DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = Firebase.firestore.collection(POSTS)
            .whereEqualTo("rankCategory", category)
            .orderBy("rank", Query.Direction.ASCENDING)

        getItems(viewLifecycleOwner) {
            viewModel.getRankedCategoryItems(category, query)
        }

        val topPadding = resources.getDimension(R.dimen.action_bar_height).toInt()
        binding.pagerItemsRecycler.setPadding(0, topPadding, 0, 0)

    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(viewLifecycleOwner, activity)
    }

    companion object {
        private const val TAG = "RankedCategory"
    }

    override fun getDefaultInfoText(): String {
        return "No ranked posts at this moment"
    }

}