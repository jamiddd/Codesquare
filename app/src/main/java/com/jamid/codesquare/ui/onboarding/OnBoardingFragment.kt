package com.jamid.codesquare.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.viewpager.OnBoardingViewPager
import com.jamid.codesquare.databinding.FragmentOnBoardingBinding
import com.jamid.codesquare.hideKeyboard
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

// something simple
class OnBoardingFragment: BaseFragment<FragmentOnBoardingBinding>() {
    override fun onCreateBinding(inflater: LayoutInflater): FragmentOnBoardingBinding {
        return FragmentOnBoardingBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.onBoardingPager.adapter = OnBoardingViewPager(requireActivity())

        OverScrollDecoratorHelper.setUpOverScroll((binding.onBoardingPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        runDelayed(400) {
            hideKeyboard()
        }

        TabLayoutMediator(binding.onBoardingTabs, binding.onBoardingPager) { _, _ -> }.attach()

        binding.nextBtn.setOnClickListener {
            val currentItem = binding.onBoardingPager.currentItem
            if (currentItem != 2) {
                binding.onBoardingPager.setCurrentItem(currentItem + 1, true)
            } else {
                findNavController().navigate(R.id.loginFragment, null)
            }
        }

        binding.prevBtn.setOnClickListener {
            val currentItem = binding.onBoardingPager.currentItem
            if (currentItem != 0) {
                binding.onBoardingPager.setCurrentItem(currentItem - 1, true)
            } else {
                findNavController().navigate(R.id.loginFragment, null)
            }
        }

        binding.onBoardingPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.prevBtn.text = getString(R.string.skip)
                        binding.nextBtn.text = getString(R.string.next)
                    }
                    1 -> {
                        binding.prevBtn.text = getString(R.string.back)
                        binding.nextBtn.text = getString(R.string.next)
                    }
                    2 -> {
                        binding.prevBtn.text = getString(R.string.back)
                        binding.nextBtn.text = getString(R.string.login)
                    }
                }
            }
        })

    }

}