package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentCreateAccountBinding
import com.jamid.codesquare.ui.MessageDialogFragment

@ExperimentalPagingApi
class CreateAccountFragment : Fragment() {

    private lateinit var binding: FragmentCreateAccountBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var loadingFragment: MessageDialogFragment? = null

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

        binding.nameText.editText?.doAfterTextChanged {
            onTextChange()
            binding.nameText.error = null
            binding.nameText.isErrorEnabled = false
        }

        binding.emailText.editText?.doAfterTextChanged {
            onTextChange()
            binding.emailText.error = null
            binding.emailText.isErrorEnabled = false
        }

        binding.passwordText.editText?.doAfterTextChanged {
            onTextChange()
            binding.passwordText.error = null
            binding.passwordText.isErrorEnabled = false
        }

        binding.confirmPasswordText.editText?.doAfterTextChanged {
            onTextChange()
            binding.confirmPasswordText.error = null
            binding.confirmPasswordText.isErrorEnabled = false
        }

        binding.createBtn.setOnClickListener {

            binding.createBtn.isEnabled = false

            val nameText = binding.nameText.editText?.text
            if (nameText.isNullOrBlank()) {
                binding.nameText.isErrorEnabled = true
                binding.nameText.error = "Name cannot be empty"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val name = nameText.trim().toString()

            val emailText = binding.emailText.editText?.text
            if (emailText.isNullOrBlank()) {
                binding.emailText.isErrorEnabled = true
                binding.emailText.error = "Email cannot be empty"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val email = emailText.trim().toString()

            if (!email.isValidEmail()) {
                binding.emailText.isErrorEnabled = true
                binding.emailText.error = "Email is not valid"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val passwordText = binding.passwordText.editText?.text
            if (passwordText.isNullOrBlank()) {
                binding.passwordText.isErrorEnabled = true
                binding.passwordText.error = "Password cannot be empty"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val password = passwordText.trim().toString()

            if (!password.isValidPassword()) {
                binding.passwordText.isErrorEnabled = true
                binding.passwordText.error = "Not a valid password. Must be longer than 8 characters. Must include at least one letter, one number and one symbol"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val confirmPasswordText = binding.confirmPasswordText.editText?.text
            if (confirmPasswordText.isNullOrBlank()) {
                binding.confirmPasswordText.isErrorEnabled = true
                binding.confirmPasswordText.error = "Confirm the given password again."
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            val confirmPassword = confirmPasswordText.trim().toString()

            if (password != confirmPassword) {
                binding.confirmPasswordText.isErrorEnabled = true
                binding.confirmPasswordText.error = "Password does not match"
                binding.createBtn.isEnabled = true
                return@setOnClickListener
            }

            createAccount(name, email, password)
        }

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        UserManager.authState.observe(viewLifecycleOwner) { isSignedIn ->
            loadingFragment?.dismiss()
            binding.createBtn.isEnabled = true
            if (isSignedIn != null && isSignedIn) {
                // Chang here
                findNavController().navigate(
                    R.id.action_createAccountFragment_to_emailVerificationFragment,
                    null,
                    slideRightNavOptions()
                )
            }
        }

        viewModel.currentError.observe(viewLifecycleOwner) { exception ->
            if (exception != null) {
                loadingFragment?.dismiss()
                when (exception) {
                    is FirebaseAuthWeakPasswordException -> {
                        Log.d(TAG, "FirebaseAuthWeakPasswordException")
                    }
                    is FirebaseAuthUserCollisionException -> {
                        Log.d(TAG, "FirebaseAuthUserCollisionException")
                    }
                    is FirebaseAuthInvalidUserException -> {
                        Log.d(TAG, "FirebaseAuthInvalidUserException")
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        Log.d(TAG, "FirebaseAuthInvalidCredentialsException")
                    }
                }
                Log.e(TAG, exception.localizedMessage.orEmpty())
            }
        }

        binding.createBtn.isEnabled = false

    }

    private fun onTextChange() {
        val nameText = binding.nameText.editText?.text
        val emailText = binding.emailText.editText?.text
        val passwordText = binding.passwordText.editText?.text
        val confirmPasswordText = binding.confirmPasswordText.editText?.text

        binding.createBtn.isEnabled = !nameText.isNullOrBlank() && nameText.trim()
            .toString().length >= 4 && !emailText.isNullOrBlank() && emailText.toString()
            .isValidEmail() && !passwordText.isNullOrBlank() && passwordText.toString()
            .isValidPassword() && !confirmPasswordText.isNullOrBlank() && confirmPasswordText.toString() == passwordText.toString()
    }

    private fun showDialog() {
        val msg = "Creating account .. Please wait for a while"
        loadingFragment = MessageDialogFragment.builder(msg)
            .setIsDraggable(false)
            .setIsHideable(false)
            .shouldShowProgress(true)
            .build()

        loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)
    }

    private fun createAccount(name: String, email: String, password: String) {

        showDialog()

        FireUtility.createAccount(email, password) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val localUser = User.newUser(user.uid, name, email)

                    // setting a default image for the user
                    val photo = userImages.random()
                    localUser.photo = photo

                    FireUtility.uploadUser(localUser) { it1 ->
                        if (it1.isSuccessful) {
                            UserManager.updateUser(localUser)
                            viewModel.insertCurrentUser(localUser)
                        } else {
                            viewModel.setCurrentError(it1.exception)
                            Firebase.auth.signOut()
                        }
                    }
                }
            } else {
                binding.createBtn.isEnabled = true
                viewModel.setCurrentError(it.exception)
                Firebase.auth.signOut()
            }
        }
    }

    companion object {
        private const val TAG = "CreateAccountFragment"
    }

}