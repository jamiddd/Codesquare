package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.adapter.recyclerview.UserMinimalAdapter
import com.jamid.codesquare.data.QUERY_TYPE_USER
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

@ExperimentalPagingApi
class SearchUsersFragment: Fragment() {

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

        val userAdapter = UserMinimalAdapter()

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

    companion object {
        private const val TAG = "SearchUsersFragment"
    }

}