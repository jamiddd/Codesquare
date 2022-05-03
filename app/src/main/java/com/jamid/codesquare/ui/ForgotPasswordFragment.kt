package com.jamid.codesquare.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentForgotPasswordBinding

@OptIn(ExperimentalPagingApi::class)
class ForgotPasswordFragment: BaseFragment<FragmentForgotPasswordBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()

    override fun getViewBinding(): FragmentForgotPasswordBinding {
        return FragmentForgotPasswordBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prevEmail = arguments?.getString("email")
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

    }

}