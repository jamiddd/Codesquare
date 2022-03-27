package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentEmailVerificationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class EmailVerificationFragment: Fragment() {

    private lateinit var binding: FragmentEmailVerificationBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmailVerificationBinding.inflate(inflater)
        return binding.root
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
            findNavController().navigate(R.id.loginFragment, null, slideRightNavOptions())
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

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
                        findNavController().navigate(R.id.action_emailVerificationFragment_to_profileImageFragment, null, slideRightNavOptions())
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