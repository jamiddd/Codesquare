package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentAdvertiseInfoBinding

// something simple
class AdvertiseInfoFragment: BaseFragment<FragmentAdvertiseInfoBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentAdvertiseInfoBinding {
        return FragmentAdvertiseInfoBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.giveFeedbackBtn.setOnClickListener {
            findNavController().navigate(R.id.feedbackFragment)
        }

        /*binding.rateUsBtn.setOnClickListener {
            val manager = if (BuildConfig.DEBUG) {
                FakeReviewManager(activity)
            } else {
                ReviewManagerFactory.create(activity)
            }

            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // We got the ReviewInfo object
                    val reviewInfo = task.result
                    Log.d(TAG, "onViewCreated: $reviewInfo")
                    manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                        if (!it.isSuccessful) {
                            Log.d(TAG, "onViewCreated: ${it.exception?.localizedMessage}")
                        } else {
                            Log.d(TAG, "onViewCreated: SUCCESS")
                        }
                    }
                } else {
                    // There was some problem, log or handle the error code.
                    @ReviewErrorCode
                    val errorCode = when (val exception = task.exception) {
                        is ReviewException -> {
                            exception.errorCode
                        }
                        else -> {
                            9999
                        }
                    }
                    Log.d(TAG, "onViewCreated: $errorCode")
                }
            }

        }*/

    }

}