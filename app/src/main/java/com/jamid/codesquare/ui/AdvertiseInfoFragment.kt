package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentAdvertiseInfoBinding

class AdvertiseInfoFragment: BaseFragment<FragmentAdvertiseInfoBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentAdvertiseInfoBinding {
        return FragmentAdvertiseInfoBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.giveFeedbackBtn.setOnClickListener {
            findNavController().navigate(R.id.feedbackFragment)
        }

    }

}