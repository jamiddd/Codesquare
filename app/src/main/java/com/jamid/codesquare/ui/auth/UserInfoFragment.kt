package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.data.UserUpdate
import com.jamid.codesquare.databinding.FragmentUserInfoBinding
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.listeners.SearchItemClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
// something simple
class UserInfoFragment: BaseFragment<FragmentUserInfoBinding>(), SearchItemClickListener, InterestItemClickListener {

    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private var currentList: List<SearchQuery> = emptyList()

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
                about = aboutText.trim().toString()
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

            val photo = if (currentUser.photo.isNotBlank()) {
                currentUser.photo.toUri()
            } else {
                null
            }
            val userUpdate = UserUpdate(
                null,
                null,
                photo,
                false,
                null,
                about,
                interests
            )

            runOnBackgroundThread {
                when (val result = FireUtility.updateUser3(userUpdate)) {
                    is Result.Error -> {
                        runOnMainThread {
                            binding.doneBtn.show()
                            Log.e(TAG, "onViewCreated: ${result.exception.localizedMessage}")
                        }
                    }
                    is Result.Success -> {
                        runOnMainThread {
                            findNavController().navigate(R.id.action_userInfoFragment_to_navigationHome)
                        }
                    }
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
                    val tag = binding.interestSearchText.text?.trim().toString()
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
                Log.e(TAG, "onViewCreated: ${it.exception?.localizedMessage}")
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

    override fun onInterestClick(interestItem: InterestItem) {
        if (interestItem.isChecked) {
            viewModel.uncheckInterestItem(interestItem)
        } else {
            viewModel.checkInterestItem(interestItem)
        }
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentUserInfoBinding {
        return FragmentUserInfoBinding.inflate(inflater)
    }

}