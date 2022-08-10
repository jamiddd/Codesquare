package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.adapter.recyclerview.RankedRulesAdapter
import com.jamid.codesquare.data.RankedRule
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentRankedRulesBinding
import com.jamid.codesquare.listeners.RankedRuleClickListener
// something simple
class RankedRulesFragment: FullscreenBottomSheetFragment(), RankedRuleClickListener {

    private lateinit var binding: FragmentRankedRulesBinding
    private lateinit var rankedRulesAdapter: RankedRulesAdapter

    private val savedList = mutableListOf<RankedRule>()
    private var lastOpenedPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRankedRulesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDraggable(false)
        setFullHeight()
        rankedRulesAdapter = RankedRulesAdapter(this)

        binding.rulesList.apply {
            adapter = rankedRulesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        FireUtility.getRankedRules { result: Result<List<RankedRule>> ->
            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "onViewCreated: ${result.exception}")
                }
                is Result.Success -> {
                    savedList.clear()
                    savedList.addAll(result.data)

                    savedList[0].isOpened = true

                    rankedRulesAdapter.submitList(savedList)
                }
                else -> {

                }
            }
        }

    }

    companion object {
        private const val TAG = "RankedRulesFragment"
    }

    override fun onToggleButtonClick(rankedRule: RankedRule, position: Int) {
        if (lastOpenedPosition != position)
            savedList[lastOpenedPosition].isOpened = false

        savedList[position].isOpened = !rankedRule.isOpened
        rankedRulesAdapter.submitList(savedList)

        if (lastOpenedPosition != position)
            rankedRulesAdapter.notifyItemChanged(lastOpenedPosition)

        rankedRulesAdapter.notifyItemChanged(position)

        lastOpenedPosition = position
    }

}