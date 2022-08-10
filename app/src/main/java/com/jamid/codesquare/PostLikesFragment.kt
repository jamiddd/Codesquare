package com.jamid.codesquare

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.LikedByAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.UserMinimal
import com.jamid.codesquare.ui.DefaultPagingFragment

class PostLikesFragment: DefaultPagingFragment<UserMinimal, UserViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val postId = arguments?.getString(POST_ID) ?: return

        val query = Firebase.firestore.collection(POSTS)
            .document(postId)
            .collection("likedBy")

        Log.d(TAG, "onViewCreated: $postId")

        getItems(viewLifecycleOwner) {
            viewModel.getLikes(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)
    }

    override fun getPagingAdapter(): PagingDataAdapter<UserMinimal, UserViewHolder> {
        return LikedByAdapter()
    }

    override fun getDefaultInfoText(): String {
        return "No likes for this post"
    }

}