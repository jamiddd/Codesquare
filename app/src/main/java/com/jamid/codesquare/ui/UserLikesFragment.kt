package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.LIKED_USERS
import com.jamid.codesquare.R
import com.jamid.codesquare.USERS
import com.jamid.codesquare.USER_ID
import com.jamid.codesquare.adapter.recyclerview.LikedByAdapter
import com.jamid.codesquare.adapter.recyclerview.PagingUserAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.LikedBy
import com.jamid.codesquare.data.User

@ExperimentalPagingApi
class UserLikesFragment: PagerListFragment<LikedBy, UserViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val userId = arguments?.getString(USER_ID) ?: return

        val query = Firebase.firestore.collection(USERS)
            .whereArrayContains(LIKED_USERS, userId)

        getItems {
            viewModel.getLikes(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)

    }

    override fun getAdapter(): PagingDataAdapter<LikedBy, UserViewHolder> {
        return LikedByAdapter()
    }
}