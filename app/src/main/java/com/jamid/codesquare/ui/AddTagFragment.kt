package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentAddTagBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

@ExperimentalPagingApi
class AddTagFragment: RoundedBottomSheetDialogFragment(), SearchItemClickListener {

    private lateinit var binding: FragmentAddTagBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var currentList: List<SearchQuery> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTagBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = dialog!!
        val frame = dialog.window!!.decorView.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(frame)
        val totalHeight = getWindowHeight()
        binding.root.updateLayoutParams<ViewGroup.LayoutParams> {
            height = totalHeight
        }

        val offset = totalHeight * 0.15

        behavior.maxHeight = totalHeight - offset.toInt()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.projectTagsCloseBtn.setOnClickListener {
            dismiss()
        }
        
        val searchResultsAdapter = SearchResultsAdapter(this).apply {
            shouldShowRightIcon = false
        }
        
        binding.projectTagSearchRecycler.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    currentList = it.distinctBy { it1 ->
                        it1.queryString
                    }

                    binding.projectTagSearchRecycler.show()

                    searchResultsAdapter.submitList(currentList)
                } else {
                    searchResultsAdapter.submitList(emptyList())
                }
            } else {

                binding.projectTagSearchRecycler.hide()

                searchResultsAdapter.submitList(emptyList())
            }
        }
        
        binding.projectTagSearchInput.editText?.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) {

                viewModel.searchInterests(text.toString())
                binding.projectTagSearchInput.setEndIconDrawable(R.drawable.ic_round_add_24)

                binding.projectTagSearchInput.setEndIconOnClickListener {
                    val tag = text.toString()
                    binding.projectTags.addTag(tag, true)
                    binding.projectTagSearchInput.editText?.text?.clear()
                }

            } else {
                viewModel.setSearchData(emptyList())
                
                binding.projectTagSearchInput.setEndIconDrawable(R.drawable.ic_round_search_24)
                
                binding.projectTagSearchInput.setEndIconOnClickListener {

                }
            }
        }

        val alphabets = getString(R.string.alphabets)
        FireUtility.getRandomInterests {
            if (it.isSuccessful) {
                val snapshot = it.result
                for (l in alphabets) {
                    @Suppress("UNCHECKED_CAST") val tags = snapshot[l.toString()] as List<String>
                    preloadTags(tags)
                }
            } else {
                Log.d(TAG, it.exception?.localizedMessage.orEmpty())
            }
        }

        binding.projectTagsAddBtn.setOnClickListener {
            val tags = mutableListOf<String>()

            for (c in binding.projectTags.children) {
                val chip = c as Chip
                if (chip.isChecked) {
                    tags.add(chip.text.toString())
                }
            }

            viewModel.addTagsToCurrentProject(tags)

            dismiss()
        }

    }

    private fun preloadTags(tags: List<String>){
        val newTags = tags.shuffled()

        for (i in newTags) {
            binding.projectTags.addTag(i)
        }
    }

    private fun FlexboxLayout.addTag(s: String, checked: Boolean = false) {
        s.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.text = s
        chip.isCheckable = true
        chip.isChecked = checked
        chip.isCheckedIconVisible = false
        chip.isCloseIconVisible = false
        chip.tag = s


        if (currentList.find { it.queryString == s } == null) {
            this.addView(chip, 0)

        } else {
            val oldChip = this.findViewWithTag<Chip>(s)
            if (oldChip != null) {
                oldChip.performClick()
            } else {
                this.addView(chip, 0)
            }
        }

        chip.updateLayoutParams<FlexboxLayout.LayoutParams> {
            marginEnd = resources.getDimension(R.dimen.generic_len).toInt()
            marginStart = resources.getDimension(R.dimen.zero).toInt()
        }

        chip.setOnClickListener {
            this.removeView(chip)
            if (chip.isChecked) {
                this.addView(chip, 0)
            }
        }
        
    }

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        val tag = searchQuery.queryString
        binding.projectTags.addTag(tag, true)
        binding.projectTagSearchInput.editText?.text?.clear()
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
        private const val TAG = "AddTagFragment"
    }

}