package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.adapter.recyclerview.PostMinimalAdapter
import com.jamid.codesquare.databinding.FragmentSearchPostsBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

class SearchPostsFragment: BaseFragment<FragmentSearchPostsBinding>() {

    private lateinit var postAdapter: PostMinimalAdapter

    override fun onCreateBinding(inflater: LayoutInflater): FragmentSearchPostsBinding {
        return FragmentSearchPostsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postAdapter = PostMinimalAdapter()

        binding.searchPostsRecycler.apply {
            val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(decoration)
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