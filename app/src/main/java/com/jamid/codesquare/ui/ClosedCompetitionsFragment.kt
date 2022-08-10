package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentClosedCompetitionsBinding
import com.jamid.codesquare.isNightMode
// something simple
class ClosedCompetitionsFragment: BaseFragment<FragmentClosedCompetitionsBinding>() {


    override fun onCreateBinding(inflater: LayoutInflater): FragmentClosedCompetitionsBinding {
        return FragmentClosedCompetitionsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isNightMode()) {
            binding.lottieAnimationView2.setAnimation(R.raw.nothing_night)
        } else {
            binding.lottieAnimationView2.setAnimation(R.raw.nothing)
        }

        binding.button3.setOnClickListener {
            val frag = RankedRulesFragment()
            frag.show(activity.supportFragmentManager, "RankedRules")
        }

    }

}