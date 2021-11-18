package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentCreateAccountBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.ui.MainActivity

class CreateAccountFragment: Fragment() {

    private lateinit var binding: FragmentCreateAccountBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateAccountBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInLinkBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.createBtn.setOnClickListener {

            val nameText = binding.nameText.editText?.text
            if (nameText.isNullOrBlank()) {
                toast("Name cannot be empty")
                return@setOnClickListener
            }

            val emailText = binding.emailText.editText?.text
            if (emailText.isNullOrBlank()) {
                toast("Email cannot be empty")
                return@setOnClickListener
            }

            if (!emailText.toString().isValidEmail()) {
                toast("Email is not valid")
                return@setOnClickListener
            }

            val passwordText = binding.passowrdText.editText?.text
            if (passwordText.isNullOrBlank()) {
                toast("Password cannot be empty")
                return@setOnClickListener
            }

            val password = passwordText.toString()

            if (!password.isValidPassword()) {
                toast("Not a valid password. Must be longer than 8 characters. Must include at least one letter, one number and one symbol")
                return@setOnClickListener
            }

            val confirmPasswordText = binding.confirmPasswordText.editText?.text
            if (confirmPasswordText.isNullOrBlank()) {
                toast("Confirm the given password again.")
                return@setOnClickListener
            }

            if (password != confirmPasswordText.toString()) {
                toast("Password do not match")
                return@setOnClickListener
            }

            val name = nameText.toString()
            val email = emailText.toString()

            createAccount(name, email, password)

        }

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

       /* Firebase.auth.addAuthStateListener {
            if (it.currentUser != null) {
                dialog?.dismiss()
                findNavController().navigate(R.id.action_createAccountFragment_to_emailVerificationFragment, null, slideRightNavOptions())
            }
        }*/

        viewModel.currentError.observe(viewLifecycleOwner) { exception ->
            if (exception != null) {
                Log.e(TAG, exception.localizedMessage.orEmpty())
            }
        }

    }

    private fun showDialog() {
        val msg = "Creating account .. Please wait for a while"
        (activity as MainActivity).showLoadingDialog(msg)
    }

    private fun createAccount(name: String, email: String, password: String) {

        showDialog()

        FireUtility.createAccount(email, password) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val ref = Firebase.firestore.collection("users").document(user.uid)
                    val localUser = User.newUser(user.uid, name, email)
                    FireUtility.uploadDocument(ref, localUser) { it1 ->
                        if (it1.isSuccessful) {
                            viewModel.insertCurrentUser(localUser)
                        } else {
                            viewModel.setCurrentError(it1.exception)
                            Firebase.auth.signOut()
                        }
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
                Firebase.auth.signOut()
            }
        }
    }

    companion object {
        private const val TAG = "CreateAccountFragment"
    }

}