package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserMinimalAdapter
import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal2
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.listeners.UserClickListener

@ExperimentalPagingApi
class SearchUsersFragment: Fragment(), UserClickListener {

    private lateinit var binding: FragmentSearchUsersBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchUsersBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userAdapter = UserMinimalAdapter(listener = this)

        binding.searchUsersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        viewModel.recentUserSearchList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noSearchedUsers.hide()
                userAdapter.submitList(it)
            } else {
                binding.noSearchedUsers.show()
            }
        }

    }

    override fun onUserClick(user: User) {
        val bundle = if (user.isCurrentUser) {
            null
        } else {
            bundleOf("user" to user)
        }
        findNavController().navigate(R.id.profileFragment, bundle, slideRightNavOptions())
    }

    override fun onUserClick(userId: String) {
        (activity as MainActivity).getUserImpulsive(userId) {
            onUserClick(it)
        }
    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        (activity as MainActivity).getUserImpulsive(userMinimal.objectID) {
            onUserClick(it)
        }
    }

    override fun onUserOptionClick(user: User) {

    }

    override fun onUserOptionClick(userMinimal: UserMinimal2) {

    }

    override fun onUserLikeClick(user: User) {

    }

    override fun onUserLikeClick(userId: String) {

    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {

    }

}