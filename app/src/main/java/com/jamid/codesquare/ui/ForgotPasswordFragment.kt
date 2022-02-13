package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment: Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.forgotPasswordBtn.setOnClickListener {
            val emailText = binding.emailText.editText?.text ?: return@setOnClickListener

            binding.forgotPasswordBtn.disappear()
            binding.forgotPasswordProgress.show()

            val email = emailText.trim().toString()

            if (email.isValidEmail()) {
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {

                        binding.forgotPasswordProgress.hide()
                        binding.forgotPasswordBtn.show()

                        if (it.isSuccessful) {
                            toast("Check your mail for a link to reset your password", Toast.LENGTH_LONG)
                            findNavController().navigateUp()
                        } else {
                            toast("Something went wrong. Try again later.")
                            toast(it.exception?.localizedMessage.orEmpty())
                        }
                    }
            }
        }

        binding.emailText.editText?.doAfterTextChanged {
            binding.forgotPasswordBtn.isEnabled = !it.isNullOrBlank() && it.trim().toString().isValidEmail()
        }

    }

}