package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class CollaborationsFragment: PagerListFragment<Post, SuperPostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val otherUser = arguments?.getParcelable<User>(USER)

        setIsViewPagerFragment(true)

        var query: Query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)

        val currentUser = UserManager.currentUser

        if (otherUser == null) {
            query = query.whereNotEqualTo("creator.userId", currentUser.id)
                .whereArrayContains(CONTRIBUTORS, currentUser.id)
                .orderBy("creator.userId")

            getItems {
                viewModel.getCollaborations(query)
            }
        } else {
            query = query.whereArrayContains(CONTRIBUTORS, otherUser.id)

            getItems {
                viewModel.getOtherUserCollaborations(query, otherUser)
            }
        }

        isEmpty.observe(viewLifecycleOwner) {
            if (it != null && it) {
                if (otherUser == null) {
                    binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search_24)
                    binding.pagerActionBtn.text = getString(R.string.search_for_posts)
                    binding.pagerActionBtn.show()

                    binding.pagerActionBtn.setOnClickListener {
                        findNavController().navigate(R.id.preSearchFragment, null, slideRightNavOptions())
                    }
                    binding.pagerNoItemsText.text = getString(R.string.empty_collaborations_greet)
                } else {
                    if (otherUser.id == currentUser.id) {
                        binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search_24)
                        binding.pagerActionBtn.text = getString(R.string.search_for_posts)
                        binding.pagerActionBtn.show()

                        binding.pagerActionBtn.setOnClickListener {
                            findNavController().navigate(R.id.preSearchFragment, null, slideRightNavOptions())
                        }
                        binding.pagerNoItemsText.text = getString(R.string.empty_collaborations_greet)
                    } else {
                        binding.pagerNoItemsText.text = getString(R.string.no_collaborations)
                    }
                }
            } else {
                binding.pagerActionBtn.hide()
            }
        }


        binding.noDataImage.setAnimation(R.raw.no_collaborations)

    }

    override fun getAdapter(): PagingDataAdapter<Post, SuperPostViewHolder> {
        return PostAdapter()
    }

    companion object {

        fun newInstance(user: User? = null) = CollaborationsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(USER, user)
            }
        }

    }

}