package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.USERS
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.PostAdapter2
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.data.ReferenceItem
// something simple
class ArchiveFragment: DefaultPagingFragment<ReferenceItem, PostViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection("archivedPosts")

        getItems(viewLifecycleOwner) {
            viewModel.getReferenceItems(query)
        }

        binding.pagerRefresher.setOnRefreshListener {
            getItems(viewLifecycleOwner) {
                viewModel.getReferenceItems(query)
            }
        }

    }

    override fun getPagingAdapter(): PagingDataAdapter<ReferenceItem, PostViewHolder> {
        return PostAdapter2(viewLifecycleOwner, activity)
    }

    override fun getDefaultInfoText(): String {
        return "No posts has been archived."
    }

    companion object {
        const val TAG = "ArchiveFragment"
    }

}