package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.ARCHIVE
import com.jamid.codesquare.USERS
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.PostAdapter2
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.ReferenceItem

@ExperimentalPagingApi
class ArchiveFragment: PagerListFragment<ReferenceItem, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection("archivedPosts")

        getItems{
            viewModel.getReferenceItems(query)
        }

        binding.pagerRefresher.setOnRefreshListener {
            getItems{
                viewModel.getReferenceItems(query)
            }
        }

    }


    override fun getAdapter(): PagingDataAdapter<ReferenceItem, PostViewHolder> {
        return PostAdapter2()
    }

    companion object {
        const val TAG = "ArchiveFragment"
    }

}