package com.jamid.codesquare.ui.home.ranked

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.viewpager.RankedPagerAdapter
import com.jamid.codesquare.databinding.FragmentRankedBinding

class RankedFragment: Fragment() {

    private lateinit var binding: FragmentRankedBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRankedBinding.inflate(inflater)
        return binding.root
    }

    companion object {
        private const val TAG = "RankedFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.competitions.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                val rankPagerAdapter = RankedPagerAdapter(requireActivity(), it)
                binding.rankedPager.adapter = rankPagerAdapter
                binding.rankedPager.isUserInputEnabled = false

                it.forEachIndexed { index, competition ->
                    val item = View.inflate(requireContext(), R.layout.toggle_button_item, null) as MaterialButton
                    item.text = competition.name
                    item.id = index
                    binding.toggleButton.addView(item)
                }

                if (binding.toggleButton.childCount > 0) {
                    binding.toggleButton.check(binding.toggleButton.getChildAt(0).id)
                }

                binding.toggleButton.addOnButtonCheckedListener { _, checkedId, _ ->
                    binding.rankedPager.currentItem = checkedId
                }

            }
        }

    }


}