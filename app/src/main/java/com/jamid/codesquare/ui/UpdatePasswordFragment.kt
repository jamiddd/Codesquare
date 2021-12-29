package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentUpdatePasswordBinding

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
                                            binding.primaryAction.text = "Update"
                                            binding.headerText.text = "Enter new password ... "
                                            binding.primaryAction.show()
                                            binding.updatePasswordProgress.hide()
                                            binding.oldPasswordText.hide()
                                            binding.newPasswordText.show()
                                        } else {
                                            // verification gone wrong
                                            binding.primaryAction.text = "Verify"
                                            binding.primaryAction.show()
                                            binding.updatePasswordProgress.hide()
                                            binding.oldPasswordText.editText?.text?.clear()
                                            toast("The password you entered in wrong.")
                                        }
                                    }
                            }
                        }
                    } else {
                        toast("Enter a valid password")
                    }
                } else {
                    toast("Enter your old password")
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

                            if (it1.isSuccessful) {
                                // password changed

                                toast("Password updated successfully.")
                                findNavController().navigateUp()
                            } else {
                                // password didn't change
                                toast("Something went wrong. Try again later.")
                            }
                        }
                    } else {
                        toast("Enter a valid password")
                    }
                } else {
                    toast("Enter a new password")
                }
            }
        }
    }
}