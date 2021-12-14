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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentEmailVerificationBinding
import com.jamid.codesquare.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        binding.verifyEmailBtn.setOnClickListener {

            viewLifecycleOwner.lifecycleScope.launch {
                for (i in 1..20) {
                    delay(5000)
                    val currentUser = Firebase.auth.currentUser
                    if (currentUser != null) {
                        val task = currentUser.reload()
                        task.addOnCompleteListener {
                            if (it.isSuccessful) {
                                if (Firebase.auth.currentUser?.isEmailVerified == true) {
                                    findNavController().navigate(R.id.action_emailVerificationFragment_to_homeFragment)
                                }
                            } else {
                                Log.d(TAG, it.exception?.localizedMessage.orEmpty())
                            }
                        }
                    }
                }
            }

            binding.verifyEmailBtn.hide()
            binding.emailVerificationProgress.show()

            val currentUser = Firebase.auth.currentUser!!
            currentUser.sendEmailVerification().addOnCompleteListener {

                if (it.isSuccessful) {
                    binding.emailVerificationMessage.text = "Verification email sent. Check your mail."
                    binding.emailVerificationMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_round_done_24, 0, 0)
                    binding.verifyEmailBtn.hide()
                } else {
                    toast(it.exception?.localizedMessage.orEmpty())
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

    }

    companion object {
        private const val TAG = "EmailVerification"
    }

}