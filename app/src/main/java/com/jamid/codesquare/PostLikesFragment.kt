package com.jamid.codesquare

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.LikedByAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.LikedBy
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class PostLikesFragment: PagerListFragment<LikedBy, UserViewHolder>() {

    override fun getAdapter(): PagingDataAdapter<LikedBy, UserViewHolder> {
        return LikedByAdapter()
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val postId = arguments?.getString(POST_ID) ?: return

        val query = Firebase.firestore.collection(POSTS)
            .document(postId)
            .collection("likedBy")

        getItems {
            viewModel.getLikes(query)
        }

        shouldShowImage = false

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)

    }
}