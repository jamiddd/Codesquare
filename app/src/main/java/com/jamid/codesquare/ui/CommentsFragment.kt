package com.jamid.codesquare.ui

import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.CommentAdapter
import com.jamid.codesquare.adapter.recyclerview.CommentViewHolder
import com.jamid.codesquare.data.Comment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class CommentsFragment: PagerListFragment<Comment, CommentViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (viewModel.currentCommentChannelIds.isNotEmpty()) {
                Log.d(TAG, viewModel.currentCommentChannelIds.toString())
                val commentChannelId = viewModel.currentCommentChannelIds.peek()
                val query = Firebase.firestore.collection("commentChannels")
                    .document(commentChannelId)
                    .collection("comments")

                getItems {
                    viewModel.getPagedComments(commentChannelId, query)
                }
            }
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView?.itemAnimator = null
        noItemsText?.text = "No comments. Be the first one to comment."

    }

    override fun getAdapter(): PagingDataAdapter<Comment, CommentViewHolder> {
        return CommentAdapter()
    }

    companion object {
        private const val TAG = "CommentsFragment"
    }

}