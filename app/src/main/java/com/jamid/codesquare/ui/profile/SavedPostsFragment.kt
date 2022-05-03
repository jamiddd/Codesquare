package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.SAVED_POSTS
import com.jamid.codesquare.USERS
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.PostAdapter2
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.ReferenceItem
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class SavedPostsFragment: PagerListFragment<ReferenceItem, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection(USERS)
            .document(currentUser.id)
            .collection(SAVED_POSTS)

        getItems {
            viewModel.getReferenceItems(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.empty_saved_posts_greet)

    }

    override fun getAdapter(): PagingDataAdapter<ReferenceItem, PostViewHolder> {
        return PostAdapter2()
    }

    companion object {
        const val TAG = "SavedPostsFragment"
    }

}