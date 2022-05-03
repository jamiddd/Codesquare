package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.data.AdLimit
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class PostsFragment: PagerListFragment<Post, SuperPostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val subFieldName = "$CREATOR.$USER_ID"

        val otherUser = arguments?.getParcelable<User>(USER)
        if (otherUser == null) {
            val currentUser = UserManager.currentUser
            val query = Firebase.firestore.collection(POSTS)
                .whereEqualTo(ARCHIVED, false)
                .whereEqualTo(subFieldName, currentUser.id)

            setIsViewPagerFragment(true)

            binding.pagerNoItemsText.text = getString(R.string.empty_user_posts_greet)

            getItems {
                viewModel.getCurrentUserPosts(query)
            }

            isEmpty.observe(viewLifecycleOwner) {
                if (it != null && it) {
                    binding.pagerActionBtn.show()

                    binding.pagerActionBtn.text = getString(R.string.create_post)
                    binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)

                    binding.pagerActionBtn.setOnClickListener {
                        if (activity.isEligibleToCreateProject()) {
                            findNavController().navigate(R.id.createPostFragment, null, slideRightNavOptions())
                        } else {
                            activity.showLimitDialog(AdLimit.MAX_POSTS)
                        }
                    }
                } else {
                    binding.pagerActionBtn.hide()
                }
            }

        } else {
            val query = Firebase.firestore
                .collection(POSTS)
                .whereEqualTo(ARCHIVED, false)
                .whereEqualTo(subFieldName, otherUser.id)

            getItems {
                viewModel.getOtherUserPosts(query, otherUser)
            }

        }

    }

    override fun getAdapter(): PagingDataAdapter<Post, SuperPostViewHolder> {
        return PostAdapter()
    }

    companion object {

        fun newInstance(user: User? = null)
            = PostsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(USER, user)
                }
        }

    }

}