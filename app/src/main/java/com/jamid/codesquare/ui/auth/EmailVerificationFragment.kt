package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentEmailVerificationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class EmailVerificationFragment: Fragment() {

    private lateinit var binding: FragmentEmailVerificationBinding

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

        Log.d(TAG, "Started email verification")

        binding.emailVerificationProgress.show()
        currentUser?.sendEmailVerification()?.addOnCompleteListener {
            binding.emailVerificationProgress.hide()
            if (it.isSuccessful) {
                Log.d(TAG, "Sent email for verification. Waiting for confirmation ...")

                setListenerForEmailVerification()
                binding.emailVerificationMessage.text = getString(R.string.email_verification_message)
                binding.emailVerificationMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_round_done_24, 0, 0)
            } else {
                it.exception?.localizedMessage?.let { it1 -> Log.e(TAG, it1) }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        UserManager.authState.observe(viewLifecycleOwner) { isSignedIn ->
            if (isSignedIn != null && isSignedIn) {
                if (UserManager.isEmailVerified) {
                    Log.d(TAG, "User is signed in and email is verified.")
                    findNavController().navigate(R.id.action_emailVerificationFragment_to_profileImageFragment, null, slideRightNavOptions())
                }
            }
        }

    }

    private fun setListenerForEmailVerification() = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        Log.d(TAG, "Setting listener to listen for email verification changes.")
        UserManager.listenForUserVerification(20, 5)
    }

    companion object {
        private const val TAG = "EmailVerification"
    }

}