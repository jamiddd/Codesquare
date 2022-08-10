package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jamid.codesquare.databinding.FragmentRankIntroBinding
// something simple
class RankIntroFragment : FullscreenBottomSheetFragment() {

    private lateinit var binding: FragmentRankIntroBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRankIntroBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFullHeight()

        binding.rankedGetStarted.setOnClickListener {
            dismiss()
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.knowMoreBtn.setOnClickListener {
            val rules = RankedRulesFragment()
            rules.show(requireActivity().supportFragmentManager, "Rules")
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            RankIntroFragment().apply {

            }
    }
}