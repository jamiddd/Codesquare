package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.InterestItemAdapter
import com.jamid.codesquare.adapter.recyclerview.InterestItemViewHolder
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentAddTagBinding
import com.jamid.codesquare.listeners.AddTagsListener
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.listeners.SearchItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// something simple
class AddTagsFragment :
    BottomSheetPagingFragment<FragmentAddTagBinding, InterestItem, InterestItemViewHolder>(),
    SearchItemClickListener, InterestItemClickListener {

    private var currentSearchList: List<SearchQuery> = emptyList()
    private var addTagsListener: AddTagsListener? = null
    private var selectedTempList = mutableListOf<String>()
    private var title: String = "Add tags"

    private val prefillList = mutableListOf<String>()
    private val selectedList = mutableListOf<String>()
    private var shouldPrefill = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topComp.bottomSheetText.show()

        binding.postTags.apply {
            layoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)
            adapter = myPagingAdapter
        }

        binding.topComp.bottomSheetToolbar.title = title

        binding.topComp.bottomSheetToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val searchResultsAdapter = SearchResultsAdapter(this).apply {
            shouldShowRightIcon = false
        }

        binding.postTagSearchRecycler.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    currentSearchList = it.distinctBy { it1 ->
                        it1.queryString
                    }

                    binding.postTagSearchRecycler.show()

                    searchResultsAdapter.submitList(currentSearchList)
                } else {
                    searchResultsAdapter.submitList(emptyList())
                }
            } else {
                binding.postTagSearchRecycler.hide()
                searchResultsAdapter.submitList(emptyList())
            }
        }

        val query = Firebase.firestore.collection(INTERESTS)

        getItems(viewLifecycleOwner) {
            viewModel.getPagedInterestItems(query)
        }


        // for end icon click
        binding.topComp.bottomSheetText.editText?.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) {

                viewModel.searchInterests(text.trim().toString())
                binding.topComp.bottomSheetText.setEndIconDrawable(R.drawable.ic_round_add_24)

                binding.topComp.bottomSheetText.setEndIconOnClickListener {
                    val tag = text.toString()
                    // insert in database
                    insertInterestItem(tag)
                }

            } else {
                viewModel.setSearchData(emptyList())

                binding.topComp.bottomSheetText.setEndIconDrawable(R.drawable.ic_round_search_24)

                binding.topComp.bottomSheetText.setEndIconOnClickListener {

                }
            }
        }

        binding.topComp.bottomSheetDoneBtn.text = "Add"

        binding.topComp.bottomSheetDoneBtn.setOnClickListener {
            saveChanges()
            dismiss()
        }

        runDelayed(300) {
            binding.topComp.bottomSheetText.requestFocus()
        }

    }

    private fun prefillItems() = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {

        prefillList.forEach {
            val id = randomId()
            val now = System.currentTimeMillis()

            val localItem = viewModel.getInterestItem(it)
            if (localItem != null) {
                localItem.isChecked = true
                viewModel.insertInterestItem(localItem)
            } else {
                viewModel.insertInterestItem(InterestItem(id, id, it, now, emptyList(), now, 0.0, true))
            }

        }

    }

    private fun onBackPressed() {
        if (selectedTempList.isNotEmpty()) {

            val word = if (title.contains("tag")) {
                "tags"
            } else {
                "interests"
            }

            val frag = MessageDialogFragment.builder("You have selected some $word!")
                .setTitle("Add $word")
                .setPositiveButton("Save changes") { a, _ ->
                    a.dismiss()
                    saveChanges()
                    this.dismiss()
                }.setNegativeButton("Discard changes") { a, _ ->
                    a.dismiss()
                    this.dismiss()
                }.build()

            frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
        } else {
            dismiss()
        }
    }

    private fun saveChanges() {
        val tags = mutableListOf<String>()

        for (c in binding.postTags.children) {
            val chip = c as Chip
            if (chip.isChecked) {
                tags.add(chip.text.toString())
            }
        }

        addTagsListener?.onTagsSelected(tags)
    }

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        val tag = searchQuery.queryString
        insertInterestItem(tag)
        binding.topComp.bottomSheetText.editText?.text?.clear()
    }

    private fun insertInterestItem(tag: String) {
        // insert in database
        val id = tag.trim().lowercase().replace(" ", "_")
        val now = System.currentTimeMillis()
        val interestItem = InterestItem(
            id,
            id,
            tag,
            now,
            emptyList(),
            now,
            0.001,
            true
        )

        FireUtility.insertInterestItem(interestItem) {
            binding.topComp.bottomSheetText.editText?.text?.clear()

            if (it.isSuccessful) {
                viewModel.insertInterestItem(interestItem)
            } else {
                Log.e(TAG, "onViewCreated: ${it.exception?.localizedMessage}")
            }
        }
    }

    override fun onRecentSearchClick(searchQuery: SearchQuery) {

    }

    override fun onSearchItemForwardClick(query: SearchQuery) {

    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setSearchData(emptyList())
    }

    companion object {

        const val TAG = "AddTagFragment"
        const val ARG_PREV_LIST = "ARG_PREV_LIST"

        class Builder {

            private val addTagsFragment = AddTagsFragment()

            fun setListener(mListener: AddTagsListener): Builder {
                addTagsFragment.addTagsListener = mListener
                return this
            }

            fun setPrefill(items: List<String>): Builder {
                addTagsFragment.prefillList.clear()
                addTagsFragment.prefillList.addAll(items)
                addTagsFragment.shouldPrefill = true
                return this
            }

            fun setTitle(title: String): Builder {
                addTagsFragment.title = title
                return this
            }

            fun build(): AddTagsFragment {
                return addTagsFragment
            }

        }

        fun builder(): Builder {
            return Builder()
        }

        fun newInstance(): AddTagsFragment {
            return AddTagsFragment()
        }

    }

    override fun onInterestClick(interestItem: InterestItem) {
        if (interestItem.isChecked) {
            selectedTempList.remove(interestItem.content)
            viewModel.uncheckInterestItem(interestItem)
        } else {
            selectedTempList.add(interestItem.content)
            viewModel.checkInterestItem(interestItem)
        }
    }

    override fun getPagingAdapter(): PagingDataAdapter<InterestItem, InterestItemViewHolder> {
        return InterestItemAdapter(this)
    }

    override fun onPagingDataChanged(itemCount: Int) {
        if (itemCount == 0) {
            binding.addTagsInfoText.text = "No tags found!"
        } else {
            binding.addTagsProgressbar.hide()
        }
    }

    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {
        binding.addTagsProgressbar.hide()
    }

    override fun onAdapterStateChanged(state: AdapterState, error: Throwable?) {
        setDefaultPagingLayoutBehavior(
            state,
            error,
            null,
            binding.addTagsInfoText,
            binding.postTags,
            null
        )

        when (state) {
            AdapterState.LOAD_FINISHED -> {
                if (prefillList.isNotEmpty() && shouldPrefill) {
                    shouldPrefill = false
                    prefillItems()
                }
            }
            else -> {

            }
        }

    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentAddTagBinding {
        return FragmentAddTagBinding.inflate(inflater)
    }

}