package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentEmailVerificationBinding

class EmailVerificationFragment: BaseFragment<FragmentEmailVerificationBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentEmailVerificationBinding {
        return FragmentEmailVerificationBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = Firebase.auth.currentUser
        binding.emailVerificationProgress.show()

        if (currentUser != null) {
            currentUser.sendEmailVerification().addOnCompleteListener {

                binding.emailVerificationProgress.hide()

                if (it.isSuccessful) {
                    viewModel.setListenerForEmailVerification()

                    binding.emailVerificationMessage.text = getString(R.string.email_verification_message)
                    binding.emailVerificationMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_round_done_24, 0, 0)
                } else {
                    it.exception?.localizedMessage?.let { it1 -> Log.e(TAG, it1) }
                }
            }
        } else {
            // going back to login fragment
            findNavController().navigate(R.id.loginFragment, null)
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity.finish()
            }
        })

        viewModel.userVerificationResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Error -> {
                    Log.e(
                        TAG,
                        "onViewCreated: Something went wrong while trying to verify email. Cause -> ${result.exception.localizedMessage}")
                }
                is Result.Success -> {
                    val isEmailVerified = result.data
                    if (isEmailVerified) {
                        findNavController().navigate(R.id.profileImageFragment)
                    } else {
                        Log.d(TAG, "onViewCreated: Waiting for email to be verified")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "EmailVerification"
    }


}