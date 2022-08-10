package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.COMMENT
import com.jamid.codesquare.COMMENTS
import com.jamid.codesquare.COMMENT_CHANNELS
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.LikedByAdapter
import com.jamid.codesquare.adapter.recyclerview.UserViewHolder
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.UserMinimal

class CommentLikesFragment: DefaultPagingFragment<UserMinimal, UserViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val comment = arguments?.getParcelable<Comment>(COMMENT) ?: return

        val query = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)
            .collection("likedBy")

        getItems(viewLifecycleOwner) {
            viewModel.getLikes(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)

    }

    override fun getDefaultInfoText(): String {
        return "No one liked this comment"
    }

    override fun getPagingAdapter(): PagingDataAdapter<UserMinimal, UserViewHolder> {
        return LikedByAdapter()
    }

}