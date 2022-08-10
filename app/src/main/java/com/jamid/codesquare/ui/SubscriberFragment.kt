package com.jamid.codesquare.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentSubscriberBinding
import com.jamid.codesquare.doOnAnimationEnd
import com.jamid.codesquare.isNightMode

class SubscriberFragment: Fragment() {

    private lateinit var binding: FragmentSubscriberBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscriberBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = if (isNightMode()) {
            R.raw.confetti_night
        } else {
            R.raw.confetti
        }

        val image1 = if (isNightMode()) {
            R.raw.premium_night
        } else {
            R.raw.premium
        }

        binding.successAnimationView.setAnimation(image)
        binding.premiumAnimationView.setAnimation(image1)

        binding.successAnimationView.doOnAnimationEnd {
            val a1 = ObjectAnimator.ofFloat(binding.successfulSubMessage, View.TRANSLATION_Y, 100f, 0f)
            val a2 = ObjectAnimator.ofFloat(binding.successfulSubMessage, View.ALPHA, 1f)
            val ap = ObjectAnimator.ofFloat(binding.premiumAnimationView, View.ALPHA, 1f)

            val a3 = ObjectAnimator.ofFloat(binding.backNavigationBtn, View.ALPHA, 1f)
            val a4 = ObjectAnimator.ofFloat(binding.backNavigationBtn, View.TRANSLATION_Y, 50f, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.play(a1)
                .with(a2)
                .with(ap)
                .before(a3)
                .with(a4)

            animatorSet.start()
        }

        binding.backNavigationBtn.setOnClickListener {
            findNavController().navigateUp()
            (activity as MainActivity).recreate()
        }

    }

}