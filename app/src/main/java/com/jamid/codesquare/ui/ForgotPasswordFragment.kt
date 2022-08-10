package com.jamid.codesquare.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentForgotPasswordBinding
// something simple
class ForgotPasswordFragment: BaseFragment<FragmentForgotPasswordBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentForgotPasswordBinding {
        return FragmentForgotPasswordBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prevEmail = arguments?.getString(ARG_EMAIL)
        binding.emailText.editText?.setText(prevEmail)

        binding.forgotPasswordBtn.isEnabled = !prevEmail.isNullOrBlank()

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
                            Snackbar.make(activity.binding.root, "Check your mail for a link to reset your password", Snackbar.LENGTH_INDEFINITE).show()
                            findNavController().navigateUp()
                        } else {
                            binding.forgotPasswordBtn.isEnabled = true
                            binding.forgotPasswordBtn.show()
                            binding.forgotPasswordProgress.hide()

                            Snackbar.make(activity.binding.root, "Something went wrong. Try again later.", Snackbar.LENGTH_LONG).setBackgroundTint(
                                Color.RED).show()
                        }
                    }
            }
        }

        binding.emailText.editText?.doAfterTextChanged {
            binding.forgotPasswordBtn.isEnabled = !it.isNullOrBlank() && it.trim().toString().isValidEmail()
        }

        runDelayed(300) {
            binding.emailText.editText?.requestFocus()

            if (keyboardState.value != true) {
                showKeyboard()
            }

        }

    }

    companion object {
        const val ARG_EMAIL = "ARG_EMAIL"
    }

}