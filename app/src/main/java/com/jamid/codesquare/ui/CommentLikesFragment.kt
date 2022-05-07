package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
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
import com.jamid.codesquare.data.LikedBy

@ExperimentalPagingApi
class CommentLikesFragment: PagerListFragment<LikedBy, UserViewHolder>() {

    override fun getAdapter(): PagingDataAdapter<LikedBy, UserViewHolder> {
        return LikedByAdapter()
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val comment = arguments?.getParcelable<Comment>(COMMENT) ?: return

        val query = Firebase.firestore.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)
            .collection("likedBy")

        getItems {
            viewModel.getLikes(query)
        }

        shouldShowImage = false

        binding.pagerNoItemsText.text = getString(R.string.no_users_found)
    }

}