package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.jamid.codesquare.data.OnBoardingData
import com.jamid.codesquare.databinding.FragmentOnBoardingChildBinding

class OnBoardingChildFragment: Fragment() {

    private lateinit var binding: FragmentOnBoardingChildBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnBoardingChildBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val data = arguments?.getParcelable<OnBoardingData>("data") ?: return

        binding.onBoardingImage.setAnimation(data.image)

        binding.onBoardingContent.text = data.content

    }

    companion object {
        fun newInstance(data: OnBoardingData) =
            OnBoardingChildFragment().apply {
                arguments = bundleOf("data" to data)
            }
    }

}