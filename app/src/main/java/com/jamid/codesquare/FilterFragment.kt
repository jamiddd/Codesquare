package com.jamid.codesquare

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.chip.Chip
import com.jamid.codesquare.data.FeedOption
import com.jamid.codesquare.data.FeedOrder
import com.jamid.codesquare.data.FeedSort
import com.jamid.codesquare.data.FeedSort.*
import com.jamid.codesquare.databinding.FilterLayoutBinding
// something simple
class FilterFragment : BaseBottomFragment<FilterLayoutBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FilterLayoutBinding {
        return FilterLayoutBinding.inflate(inflater)
    }

    private fun setViewsWithPreExistingData(feedOption: FeedOption) {
        val currentTag = feedOption.filter
        if (currentTag != null) {
            for (child in binding.filterInterestTags.children) {
                val chip = child as Chip
                val s = chip.text.toString()

                if (currentTag == s) {
                    chip.isChecked = true
                    break
                }
            }
        } else {
            for (child in binding.filterInterestTags.children) {
                val chip = child as Chip
                val s = chip.text.toString()

                if (s == "Random") {
                    chip.isChecked = true
                    break
                }
            }
        }

        when (feedOption.sort) {
            FeedSort.CONTRIBUTORS -> {
                binding.sortContributors.isChecked = true
            }
            LIKES -> {
                binding.sortLikes.isChecked = true
            }
            MOST_VIEWED -> {
                binding.sortRelevance.isChecked = true
            }
            MOST_RECENT -> {
                binding.sortTime.isChecked = true
            }
            FeedSort.LOCATION -> {
                binding.sortLocation.isChecked = true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser

        if (currentUser.interests.isEmpty()) {
            binding.addInterestsBtn.show()
        } else {
            binding.addInterestsBtn.hide()

            val l = mutableListOf<String>()
            l.add(getString(R.string.random))
            l.addAll(currentUser.interests)

            binding.filterInterestTags.addTagChips(l, checkable = true)

            /*for (tag in currentUser.interests) {
                binding.filterInterestTags.addTag(tag)
            }*/
        }

        binding.addInterestsBtn.setOnClickListener {
            viewModel.setCurrentImage(currentUser.photo.toUri())

            findNavController().navigate(R.id.editProfileFragment)
            dismiss()
        }


        val currentSetting = viewModel.feedOption.value
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val isSettingsRemembered = pref.getBoolean("is_settings_remembered", false)
        if (isSettingsRemembered) {

            binding.rememberFilter.isChecked = true

            val filter = pref.getString("feed_filter", null)
            val sort = pref.getString("feed_sort", getString(R.string.sort_time))!!
            val order = pref.getString("feed_order", "desc")!!

            val oldSetting = FeedOption(filter, getSortFromString(sort), getOrderFromString(order))

            if (oldSetting != currentSetting) {
                // old setting is not the default setting

                Log.d(TAG, "onViewCreated: $oldSetting --- > $currentSetting")
                    
                setViewsWithPreExistingData(oldSetting)
                viewModel.setCurrentFeedOption(oldSetting)
            } else {
                // it's the default setting
                setViewsWithPreExistingData(currentSetting)
            }

        } else {
            binding.rememberFilter.isChecked = false
            setViewsWithPreExistingData(currentSetting!!)
        }


        binding.applyFilterBtn.setOnClickListener {

            val feedOption = FeedOption(null, MOST_RECENT, FeedOrder.DESC)

            val tagChip = binding.filterInterestTags.findViewById<Chip>(binding.filterInterestTags.checkedChipId)
            val tag = tagChip.text.toString()

            feedOption.filter = if (tag == getString(R.string.random)) {
                null
            } else {
                tag
            }

            val sortChip = binding.sortTags.findViewById<Chip>(binding.sortTags.checkedChipId)
            val sort1 = sortChip.text.toString()
            when (sort1) {
                getString(R.string.sort_contributors) -> {
                    feedOption.sort = FeedSort.CONTRIBUTORS
                }
                getString(R.string.sort_likes) -> {
                    feedOption.sort = LIKES
                }
                getString(R.string.sort_relevance) -> {
                    feedOption.sort = MOST_VIEWED
                }
                getString(R.string.sort_time) -> {
                    feedOption.sort = MOST_RECENT
                }
                getString(R.string.sort_location) -> {
                    feedOption.sort = FeedSort.LOCATION
                }
            }

            if (feedOption != viewModel.feedOption.value) {
                viewModel.setCurrentFeedOption(feedOption)
            }

            val pref1 = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor = pref1.edit()
            if (binding.rememberFilter.isChecked) {
                editor.putString("feed_filter", feedOption.filter)
                editor.putString("feed_sort", sort1)
                editor.putString("feed_order", "desc")
                editor.putBoolean("is_settings_remembered", true)

                editor.apply()
            } else {
                editor.putBoolean("is_settings_remembered", false)
                editor.apply()
            }

            dismiss()
        }

    }

/*    private fun ChipGroup.addTag(tag: String) {
        tag.trim()
        val lContext = requireContext()
        val chip = View.inflate(lContext, R.layout.choice_chip, null) as Chip

        chip.apply {
            isCheckable = true
            text = tag
            isCloseIconVisible = false
            addView(this)
        }
    }*/

    companion object {
        const val TAG = "FilterFragment"
    }

}