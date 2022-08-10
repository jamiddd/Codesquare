package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.ARCHIVED
import com.jamid.codesquare.BaseBottomFragment
import com.jamid.codesquare.POSTS
import com.jamid.codesquare.adapter.recyclerview.PostSelectionAdapter
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.PostWrapper
import com.jamid.codesquare.databinding.FragmentPostSelectorBinding
import com.jamid.codesquare.listeners.PostSelectListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostSelectorFragment(private val onDone: (list: List<Post>) -> Unit) : BaseBottomFragment<FragmentPostSelectorBinding>(), PostSelectListener {

    private var isCurrentUserPostSelection = true
    var isSingleSelection = false
    private var lastSelectedPos = -1
    private lateinit var postSelectAdapter: PostSelectionAdapter
    private val selectedPostsList = mutableMapOf<String, Post>()

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPostSelectorBinding {
        return FragmentPostSelectorBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postSelectAdapter = PostSelectionAdapter(this)

        binding.postSelectRecycler.apply {
            adapter = postSelectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.bottomSheetToolbarComp.bottomSheetToolbar.setNavigationOnClickListener {
            onDone(emptyList())
            dismiss()
        }

        binding.bottomSheetToolbarComp.bottomSheetDoneBtn.setOnClickListener {
            onDone(selectedPostsList.map { it.value })
            dismiss()
        }

        val query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)
            .whereGreaterThan("likesCount", 5)

        job?.cancel()
        job = getItems(query)

    }

    private var job: Job? = null
    @OptIn(ExperimentalPagingApi::class)
    private fun getItems(query: Query) = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.getPostsTest(query).collectLatest {
            postSelectAdapter.submitData(it.map { it1-> PostWrapper(it1.id, it1, false) })
        }

    }

    override fun onPostSelectItemClick(postWrapper: PostWrapper, position: Int, onChange: (newPostWrapper: PostWrapper) -> Unit) {

        if (isSingleSelection) {
            // clear previous selections
            if (lastSelectedPos != -1) {
                val vh = binding.postSelectRecycler.findViewHolderForAdapterPosition(lastSelectedPos)
                if (vh != null && vh is PostSelectionAdapter.PostSelectViewHolder) {
                    vh.reset()
                }
                postSelectAdapter.notifyItemChanged(lastSelectedPos)
            }

            for (item in selectedPostsList) {
                if (item.key != postWrapper.id) {
                    selectedPostsList.remove(item.key)
                }
            }

            lastSelectedPos = position
        }

        if (postWrapper.isSelected) {
            // deselect
            selectedPostsList.remove(postWrapper.id)
            postWrapper.isSelected = false
        } else {
            // select
            selectedPostsList[postWrapper.id] = postWrapper.post
            postWrapper.isSelected = true
        }

        onChange(postWrapper)

        // TODO("Remove, only for logging purposes")
        if (selectedPostsList.isEmpty()) {
            Log.d(TAG, "onPostSelectItemClick: Selected nothing")
        } else {
            for (item in selectedPostsList) {
                Log.d(TAG, "onPostSelectItemClick: ${item.key} -- ${item.value.name}")
            }
        }

    }

}