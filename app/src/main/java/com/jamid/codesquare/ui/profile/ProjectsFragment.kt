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
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageDialogFragment
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectsFragment: PagerListFragment<Project, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val subFieldName = "$CREATOR.$USER_ID"

        val otherUser = arguments?.getParcelable<User>(USER)
        if (otherUser == null) {
            val currentUser = UserManager.currentUser
            val query = Firebase.firestore.collection(PROJECTS)
                .whereEqualTo(subFieldName, currentUser.id)

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
                        if (currentUser.premiumState.toInt() == 1 || currentUser.projects.size < 2) {
                            findNavController().navigate(R.id.createProjectFragment, null, slideRightNavOptions())
                        } else {
                            val frag = MessageDialogFragment.builder("You have already created 2 projects. To create more, upgrade your subscription plan!")
                                .setPositiveButton("Upgrade") { _, _ ->
                                    val act = activity as MainActivity
                                    act.showSubscriptionFragment()
                                }.setNegativeButton("Cancel") { a, _ ->
                                    a.dismiss()
                                }.build()

                            frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)

                        }
                    }
                } else {
                    binding.pagerActionBtn.hide()
                }
            }

        } else {
            val query = Firebase.firestore.collection(PROJECTS)
                .whereEqualTo(subFieldName, otherUser.id)

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
                    putParcelable(USER, user)
                }
        }

    }

}