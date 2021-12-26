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
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class CollaborationsFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val otherUser = arguments?.getParcelable<User>(USER)

        setIsViewPagerFragment(true)

        var query: Query = Firebase.firestore.collection(PROJECTS)

        if (otherUser == null) {
            val currentUser = UserManager.currentUser
            query = query.whereArrayContains(CONTRIBUTORS, currentUser.id)

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
                binding.pagerActionBtn.show()

                binding.pagerActionBtn.text = getString(R.string.search_for_projects)
                binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search_24)

                binding.pagerActionBtn.setOnClickListener {
                    findNavController().navigate(R.id.action_profileFragment_to_preSearchFragment, null, slideRightNavOptions())
                }
            } else {
                binding.pagerActionBtn.hide()
            }
        }

        binding.pagerNoItemsText.text = getString(R.string.empty_collaborations_greet)
        binding.noDataImage.setAnimation(R.raw.no_collaborations)

    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

    companion object {

        fun newInstance(user: User? = null) = CollaborationsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(USER, user)
            }
        }

    }

}