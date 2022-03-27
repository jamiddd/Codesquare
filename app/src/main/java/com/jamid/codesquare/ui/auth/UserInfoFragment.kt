package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.search.saas.Client
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentUserInfoBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

@ExperimentalPagingApi
class UserInfoFragment: Fragment(), SearchItemClickListener {

    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var client: Client
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private var currentList: List<SearchQuery> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserInfoBinding.inflate(inflater)
        client = Client(BuildConfig.ALGOLIA_ID, BuildConfig.ALGOLIA_SECRET)
        return binding.root
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        val currentUser = UserManager.currentUser

        searchResultsAdapter = SearchResultsAdapter(this).apply {
            shouldShowRightIcon = false
        }

        binding.interestSearchRecycler.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.doneBtn.setOnClickListener {

            binding.setupCompleteProgress.show()
            binding.doneBtn.disappear()

            var about = ""
            val aboutText = binding.aboutText.text
            if (!aboutText.isNullOrBlank()) {
                about = aboutText.toString()
                currentUser.about = about
            }

            val interests = mutableListOf<String>()

            for (c in binding.interestsContainer.children) {
                val chip = c as Chip
                if (chip.isChecked) {
                    interests.add(chip.text.toString())
                }
            }

            currentUser.interests = interests

            FireUtility.updateUser2(mapOf("interests" to interests, "about" to about)) {
                binding.setupCompleteProgress.hide()
                if (it.isSuccessful) {
                    findNavController().navigate(R.id.action_userInfoFragment_to_homeFragment, null, slideRightNavOptions())
                } else {
                    binding.doneBtn.show()
                    viewModel.setCurrentError(it.exception)
                }
            }

        }

        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    currentList = it.distinctBy { it1 ->
                        it1.queryString
                    }
                    searchResultsAdapter.submitList(currentList)
                } else {
                    searchResultsAdapter.submitList(emptyList())
                }
            } else {
                searchResultsAdapter.submitList(emptyList())
            }
        }

        binding.interestSearchText.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.searchActionBtn.isEnabled = true
                binding.searchActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)
                viewModel.searchInterests(it.toString())
            } else {
                binding.searchActionBtn.isEnabled = false
                binding.searchActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search_24)
                viewModel.setSearchData(emptyList())
            }
        }

        binding.searchActionBtn.setOnClickListener {
            val tag = binding.interestSearchText.text.toString()
            binding.interestsContainer.addTag(tag, true)
            binding.interestSearchText.text.clear()
        }

        val alphabets = "abcdefghijklmnopqrstuvwxyz"

        FireUtility.getRandomInterests {
            if (it.isSuccessful) {
                val snapshot = it.result
                for (l in alphabets) {
                    val interests = snapshot[l.toString()] as List<String>
                    preloadInterests(interests)
                }
            } else {
                Log.d(TAG, it.exception?.localizedMessage.orEmpty())
            }
        }

        binding.userInfoScrollRoot.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50) {
                binding.backBtn.hideWithAnimation()
            } else {
                binding.backBtn.showWithAnimations()
            }
        }
    }

    private fun preloadInterests(interests: List<String>){
        val newInterests = interests.shuffled()

        for (i in newInterests) {
            binding.interestsContainer.addTag(i)
        }
    }

    private fun ChipGroup.addTag(s: String, checked: Boolean = false) {
        s.trim()
        val chip = Chip(requireContext())
        chip.text = s
        chip.isCheckable = true
        chip.isChecked = checked
        chip.isCheckedIconVisible = true
        chip.checkedIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_done_24)
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

        chip.setOnClickListener {
            this.removeView(chip)
            if (chip.isChecked) {
                this.addView(chip, 0)
            }
        }


    }

    companion object {
        private const val TAG = "UserInfoFragment"
    }

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        val interest = searchQuery.queryString
        binding.interestsContainer.addTag(interest, true)
        binding.interestSearchText.text.clear()
    }

    override fun onRecentSearchClick(searchQuery: SearchQuery) {
        //
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {

    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setSearchData(emptyList())
    }

}