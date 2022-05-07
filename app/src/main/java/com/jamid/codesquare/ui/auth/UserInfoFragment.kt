package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.InterestItemAdapter
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentUserInfoBinding
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.listeners.SearchItemClickListener
import com.jamid.codesquare.ui.AddTagsFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class UserInfoFragment: BaseFragment<FragmentUserInfoBinding, MainViewModel>(), SearchItemClickListener, InterestItemClickListener {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private var currentList: List<SearchQuery> = emptyList()

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        val interestAdapter = InterestItemAdapter(this)

        val currentUser = UserManager.currentUser

        searchResultsAdapter = SearchResultsAdapter(this).apply {
            shouldShowRightIcon = false
        }

        binding.interestSearchRecycler.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.interestsContainer.apply {
            adapter = interestAdapter
            layoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)
        }

        val query = Firebase.firestore.collection(INTERESTS)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPagedInterestItems(query).collectLatest {
                interestAdapter.submitData(it)
            }
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
                    findNavController().navigate(R.id.homeFragment, null, slideRightNavOptions())
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
                binding.interestSearchTextInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)

                viewModel.searchInterests(it.toString())

                binding.interestSearchTextInputLayout.setEndIconOnClickListener {
                    val tag = binding.interestSearchText.text.toString()
                    insertInterestItem(tag)
                    binding.interestSearchText.text?.clear()
                }

            } else {
                binding.interestSearchTextInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search_24)
                viewModel.setSearchData(emptyList())

                binding.interestSearchTextInputLayout.setEndIconOnClickListener {

                }
            }
        }



        /*val alphabets = "abcdefghijklmnopqrstuvwxyz"

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
        }*/

        binding.userInfoScrollRoot.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50) {
                binding.backBtn.hideWithAnimation()
            } else {
                binding.backBtn.showWithAnimations()
            }
        }
    }

    /*private fun preloadInterests(interests: List<String>){
        val newInterests = interests.shuffled()

        for (i in newInterests) {
            binding.interestsContainer.addTag(i)
        }
    }

    private fun FlexboxLayout.addTag(s: String, checked: Boolean = false) {
        s.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.text = s
        chip.isCheckable = true
        chip.isChecked = checked
        chip.isCloseIconVisible = false
        chip.tag = s

        fun onAdded() {
            chip.updateLayoutParams<FlexboxLayout.LayoutParams> {
                marginEnd = resources.getDimension(R.dimen.generic_len).toInt()
                marginStart = resources.getDimension(R.dimen.zero).toInt()
            }
        }

        if (currentList.find { it.queryString == s } == null) {
            this.addView(chip, 0)
            onAdded()
        } else {
            val oldChip = this.findViewWithTag<Chip>(s)
            if (oldChip != null) {
                oldChip.performClick()
            } else {
                this.addView(chip, 0)
                onAdded()
            }
        }

        chip.setOnClickListener {
            this.removeView(chip)
            if (chip.isChecked) {
                this.addView(chip, 0)
                onAdded()
            }
        }

    }*/

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        val interest = searchQuery.queryString
        insertInterestItem(interest)
        binding.interestSearchText.text?.clear()
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
            binding.interestSearchText.text?.clear()

            if (it.isSuccessful) {
                viewModel.insertInterestItem(interestItem)
            } else {
                Log.e(AddTagsFragment.TAG, "onViewCreated: ${it.exception?.localizedMessage}")
            }
        }
    }

    override fun onRecentSearchClick(searchQuery: SearchQuery) {
        //
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {

    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {

    }


    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setSearchData(emptyList())
    }

    override fun getViewBinding(): FragmentUserInfoBinding {
        return FragmentUserInfoBinding.inflate(layoutInflater)
    }

    override fun onInterestClick(interestItem: InterestItem) {
        if (interestItem.isChecked) {
            viewModel.uncheckInterestItem(interestItem)
        } else {
            viewModel.checkInterestItem(interestItem)
        }
    }

}