package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.SAVED_POSTS
import com.jamid.codesquare.USERS
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.ui.DefaultPagingFragment
import kotlinx.coroutines.flow.map

class SavedPostsFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser

        val query = Firebase.firestore.collection(USERS)
            .document(currentUser.id)
            .collection(SAVED_POSTS)

        getItems(viewLifecycleOwner) {
            viewModel.getSavedPosts(query).map {
                it.map { it1 ->
                    Post2.Collab(it1)
                }
            }
        }
    }


    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(viewLifecycleOwner, activity)
    }

    override fun getDefaultInfoText(): String {
        return getString(R.string.empty_saved_posts_greet)
    }

    companion object {
        const val TAG = "SavedPostsFragment"
    }

}