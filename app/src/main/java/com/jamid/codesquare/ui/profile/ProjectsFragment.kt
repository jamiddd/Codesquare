package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectsFragment: PagerListFragment<Project, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val otherUser = arguments?.getParcelable<User>("user")
        if (otherUser == null) {
            val currentUser = UserManager.currentUser
            val query = Firebase.firestore.collection("projects")
                .whereEqualTo("creator.userId", currentUser.id)

            setIsViewPagerFragment(true)

            binding.pagerNoItemsText.text = getString(R.string.empty_user_projects_greet)

            getItems {
                viewModel.getCurrentUserProjects(query)
            }

            isEmpty.observe(viewLifecycleOwner) {
                if (it != null && it) {
                    binding.pagerActionBtn.show()

                    binding.pagerActionBtn.text = getString(R.string.create_project)
                    binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)

                    binding.pagerActionBtn.setOnClickListener {
                        findNavController().navigate(R.id.action_profileFragment_to_createProjectFragment, null, slideRightNavOptions())
                    }
                } else {
                    binding.pagerActionBtn.hide()
                }
            }

        } else {
            val query = Firebase.firestore.collection("projects")
                .whereEqualTo("creator.userId", otherUser.id)

            getItems {
                viewModel.getOtherUserProjects(query, otherUser)
            }

        }

    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

    companion object {

        fun newInstance(user: User? = null)
            = ProjectsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("user", user)
                }
        }

    }

}