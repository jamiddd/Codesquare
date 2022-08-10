package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserMinimalAdapter
import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal2
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.listeners.UserClickListener
// something simple
class SearchUsersFragment: BaseFragment<FragmentSearchUsersBinding>(), UserClickListener {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentSearchUsersBinding {
        return FragmentSearchUsersBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userAdapter = UserMinimalAdapter(listener = this)

        binding.searchUsersRecycler.apply {
            val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
            addItemDecoration(decoration)
            layoutManager = LinearLayoutManager(activity)
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
        findNavController().navigate(R.id.profileFragment2, bundle)
    }

    override fun onUserClick(userId: String) {
        FireUtility.getUser(userId) { user ->
            if (user != null) {
                onUserClick(user)
            }
        }
    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        FireUtility.getUser(userMinimal.objectID) {
            if (it != null) {
                onUserClick(it)
            }
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