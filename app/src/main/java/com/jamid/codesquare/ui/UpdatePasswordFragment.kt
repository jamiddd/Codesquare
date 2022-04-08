package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentUpdatePasswordBinding

@OptIn(ExperimentalPagingApi::class)
class UpdatePasswordFragment: Fragment() {

    private lateinit var binding: FragmentUpdatePasswordBinding
    private var actionModeDone: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUpdatePasswordBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.primaryAction.disable()

        binding.oldPasswordText.editText?.doAfterTextChanged {
            binding.oldPasswordText.isErrorEnabled = false
            binding.oldPasswordText.error = null
            binding.primaryAction.isEnabled = !it.isNullOrBlank() && it.toString().isValidPassword()
        }

        binding.newPasswordText.editText?.doAfterTextChanged {
            binding.newPasswordText.isErrorEnabled = false
            binding.newPasswordText.error = null
        }

        binding.primaryAction.setOnClickListener {

            binding.primaryAction.disappear()
            binding.updatePasswordProgress.show()

            val currentUser = Firebase.auth.currentUser
            if (!actionModeDone) {
                // verify
                val oldPasswordText = binding.oldPasswordText.editText?.text
                if (oldPasswordText != null) {
                    val oldPassword = oldPasswordText.toString()

                    if (oldPassword.isValidPassword()) {
                        if (currentUser != null) {
                            val currentUserEmail = currentUser.email
                            if (currentUserEmail != null) {
                                val credential = EmailAuthProvider.getCredential(currentUserEmail, oldPassword)

                                currentUser.reauthenticate(credential)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            // verification successful
                                            actionModeDone = true
                                            binding.primaryAction.text = getString(R.string.update)
                                            binding.headerText.text = getString(R.string.enter_new_password)
                                            binding.primaryAction.show()
                                            binding.updatePasswordProgress.hide()
                                            binding.oldPasswordText.hide()
                                            binding.newPasswordText.show()
                                        } else {
                                            // verification gone wrong
                                            binding.primaryAction.text = getString(R.string.verify)
                                            binding.primaryAction.show()
                                            binding.updatePasswordProgress.hide()
                                            binding.oldPasswordText.editText?.text?.clear()

                                            binding.oldPasswordText.isErrorEnabled = true
                                            binding.oldPasswordText.error = "The password you entered in wrong."
                                        }
                                    }
                            }
                        }
                    } else {
                        binding.oldPasswordText.isErrorEnabled = true
                        binding.oldPasswordText.error = "Enter a valid password"
                        binding.primaryAction.show()
                        binding.updatePasswordProgress.hide()
                    }
                } else {
                    binding.oldPasswordText.isErrorEnabled = true
                    binding.oldPasswordText.error = "Enter your old password"
                }
            } else {
                // update
                val newPasswordText = binding.newPasswordText.editText?.text
                if (newPasswordText != null) {
                    val newPassword = newPasswordText.toString()
                    if (newPassword.isValidPassword()) {
                        currentUser?.updatePassword(newPassword)?.addOnCompleteListener { it1 ->
                            binding.updatePasswordProgress.hide()
                            binding.primaryAction.show()
                            binding.newPasswordText.editText?.text?.clear()

                            val activity = activity as MainActivity

                            if (it1.isSuccessful) {
                                // password changed
                                Snackbar.make(activity.binding.root, "Password updated successfully.", Snackbar.LENGTH_LONG).show()
                                findNavController().navigateUp()
                            } else {
                                // password didn't change
                                Snackbar.make(activity.binding.root, "Something went wrong. Try again later.", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                                    .show()
                            }
                        }
                    } else {
                        binding.newPasswordText.isErrorEnabled = true
                        binding.newPasswordText.error = "Enter a valid password"
                    }
                } else {
                    binding.newPasswordText.isHelperTextEnabled = true
                    binding.newPasswordText.helperText = "Enter a new password"
                }
            }
        }
    }
}