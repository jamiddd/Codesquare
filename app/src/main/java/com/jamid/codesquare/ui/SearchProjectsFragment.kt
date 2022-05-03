package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.PostMinimalAdapter
import com.jamid.codesquare.databinding.FragmentSearchPostsBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

@ExperimentalPagingApi
class SearchPostsFragment: Fragment() {

    private lateinit var binding: FragmentSearchPostsBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var postAdapter: PostMinimalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchPostsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postAdapter = PostMinimalAdapter()

        binding.searchPostsRecycler.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.recentPostSearchList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noSearchedPosts.hide()
                postAdapter.submitList(it)
            } else {
                binding.noSearchedPosts.show()
            }
        }
    }
}