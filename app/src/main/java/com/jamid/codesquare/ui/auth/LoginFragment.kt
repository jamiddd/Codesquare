package com.jamid.codesquare.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentLoginBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageDialogFragment

@ExperimentalPagingApi
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var loadingFragment: MessageDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isInitiatedOnce = sharedPreferences.getBoolean("is_initiated_once", false)
        if (!isInitiatedOnce) {
            findNavController().navigate(
                R.id.action_loginFragment_to_onBoardingFragment,
                null,
                slideRightNavOptions()
            )
            val editor = sharedPreferences.edit()
            editor.putBoolean("is_initiated_once", true)
            editor.apply()
        }

        binding.signUpBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_createAccountFragment,
                null,
                slideRightNavOptions()
            )
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
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

        binding.signInBtn.setOnClickListener {

            hideKeyboard()

            binding.signInBtn.isEnabled = false

            val emailText = binding.emailText.editText?.text
            if (emailText.isNullOrBlank()) {
                binding.emailText.isErrorEnabled = true
                binding.emailText.error = "Email cannot be empty"
                binding.signInBtn.isEnabled = true
                return@setOnClickListener
            }

            val email = emailText.trim().toString()

            if (!email.isValidEmail()) {
                binding.emailText.isErrorEnabled = true
                binding.emailText.error = "Email must be valid"
                binding.signInBtn.isEnabled = true
                return@setOnClickListener
            }

            val passwordText = binding.passwordText.editText?.text
            if (passwordText.isNullOrBlank()) {
                binding.passwordText.isErrorEnabled = true
                binding.passwordText.error = "Password cannot be empty"
                binding.signInBtn.isEnabled = true
                return@setOnClickListener
            }

            val password = passwordText.trim().toString()
            if (!password.isValidPassword()) {
                binding.passwordText.isErrorEnabled = true
                binding.passwordText.error = "Not a valid password. Must be longer than 8 characters. Must include at least one letter, one number and one symbol"
                binding.signInBtn.isEnabled = true
                return@setOnClickListener
            }

            signIn(email, password)

        }

        UserManager.currentUserLive.observe(viewLifecycleOwner) {
            val currentUser = it ?: return@observe

            binding.signInBtn.isEnabled = true

            if (currentUser.interests.isEmpty()) {
                findNavController().navigate(R.id.profileImageFragment, null, slideRightNavOptions())
            } else {
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    slideRightNavOptions()
                )
            }
        }

        binding.forgotPasswordBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_forgotPasswordFragment,
                null,
                slideRightNavOptions()
            )
        }

        binding.signInBtn.isEnabled = false

    }

    private fun onTextChange() {
        val emailText = binding.emailText.editText?.text
        val passwordText = binding.passwordText.editText?.text

        binding.signInBtn.isEnabled =
            emailText.isNullOrBlank() == false && emailText.trim().toString()
                .isValidEmail() && passwordText.isNullOrBlank() == false && passwordText.toString()
                .isValidPassword()
    }

    private fun showDialog() {
        loadingFragment = MessageDialogFragment.builder("Signing in \uD83D\uDD13. Please wait for a while ... ")
            .shouldShowProgress(true)
            .setIsDraggable(false)
            .setIsHideable(false)
            .build()

        loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)
    }

    private fun signIn(email: String, password: String) {

        showDialog()

        FireUtility.signIn(email, password) {
            if (it.isSuccessful) {
                Log.d(TAG, "Login successful")
            } else {
                binding.signInBtn.isEnabled = true
                loadingFragment?.dismiss()
                viewModel.setCurrentError(it.exception)

                when (it.exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        binding.passwordText.isErrorEnabled = true
                        binding.passwordText.error = "Either email or password do not match"
                    }
                    is FirebaseAuthInvalidUserException -> {
                        binding.passwordText.isErrorEnabled = true
                        binding.passwordText.error = "No user account exists with this email"
                    }
                    else -> {
                        binding.passwordText.isErrorEnabled = true
                        binding.passwordText.error = "Unknown error occurred. Maybe check your internet connection."
                    }
                }

            }
        }
    }

    private fun signInWithGoogle() {

        showDialog()

        val signInIntent = googleSignInClient.signInIntent
        (activity as MainActivity).requestGoogleSignInLauncher.launch(signInIntent)

    }

}
