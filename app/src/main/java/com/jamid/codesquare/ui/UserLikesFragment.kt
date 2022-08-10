package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.USERS
import com.jamid.codesquare.USER_ID
import com.jamid.codesquare.adapter.recyclerview.LikedByAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.UserMinimal
// something simple
class UserLikesFragment : DefaultPagingFragment<UserMinimal, UserViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString(USER_ID) ?: return

        val query = Firebase.firestore.collection(USERS)
            .document(userId)
            .collection("likedBy")

        getItems(viewLifecycleOwner) {
            viewModel.getLikes(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)
    }


    override fun getPagingAdapter(): PagingDataAdapter<UserMinimal, UserViewHolder> {
        return LikedByAdapter()
    }

    override fun getDefaultInfoText(): String {
        return "No likes for this user."
    }

}