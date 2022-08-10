package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserDocumentNotFoundException
import com.jamid.codesquare.databinding.FragmentLoginBinding
import com.jamid.codesquare.listeners.GoogleSignInListener
import com.jamid.codesquare.ui.ForgotPasswordFragment
import com.jamid.codesquare.ui.MessageDialogFragment
import kotlinx.coroutines.Job

class LoginFragment : BaseFragment<FragmentLoginBinding>(), GoogleSignInListener {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var loadingFragment: MessageDialogFragment? = null

    override fun onCreateBinding(inflater: LayoutInflater): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater)
    }

    private var onChangeJob: Job? = null
    private val inputs = mutableListOf<TextInputLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.attachFragmentWithGoogleSignInLauncher(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInBtn.isEnabled = false

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isInitiatedOnce = sharedPreferences.getBoolean(PreferenceConstants.HAS_INITIATED, false)
        if (!isInitiatedOnce) {

            onSplashFinish()

            findNavController().navigate(R.id.onBoardingFragment)
            val editor = sharedPreferences.edit()
            editor.putBoolean(PreferenceConstants.HAS_INITIATED, true)
            editor.apply()
        }

        binding.signUpBtn.setOnClickListener {
            findNavController().navigate(R.id.createAccountFragment)
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity.finish()
            }
        })

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        /*binding.emailText.editText?.doAfterTextChanged {
            onTextChange()
            binding.emailText.error = null
            binding.emailText.isErrorEnabled = false
        }

        binding.passwordText.editText?.doAfterTextChanged {
            onTextChange()
            binding.passwordText.error = null
            binding.passwordText.isErrorEnabled = false
        }*/
        

        binding.signInBtn.setOnClickListener {
            signIn()
        }

        /*UserManager.currentUserLive.observe(viewLifecycleOwner) {
            val currentUser = it ?: return@observe

            binding.signInBtn.isEnabled = true

            if (currentUser.interests.isEmpty()) {
                findNavController().navigate(R.id.profileImageFragment)
            } else {
                findNavController().navigate(R.id.action_global_feedFragment)
            }
        }*/

        binding.forgotPasswordBtn.setOnClickListener {
            val emailText = binding.emailText.editText?.text
            var email: String? = null
            if (!emailText.isNullOrBlank()) {
                email = emailText.trim().toString()
            }

            findNavController().navigate(
                R.id.forgotPasswordFragment2,
                bundleOf(ForgotPasswordFragment.ARG_EMAIL to email)
            )
        }

        binding.signInBtn.isEnabled = false

        viewModel.googleSignInError.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it) {
                    0 -> {
                        // api exception
                        Snackbar.make(binding.root, "Something went wrong while trying to sign in with google. Try again later or user email sign in.", Snackbar.LENGTH_LONG).show()
                    }
                    1 -> {
                        // activity result not ok
                        Snackbar.make(binding.root, "Make sure you have internet connection available.", Snackbar.LENGTH_LONG).show()
                    }
                }
                loadingFragment?.dismiss()
            }
        }

        val sp = SpannableString(binding.termsPrivacy.text)

        val cs = object: ClickableSpan() {
            override fun onClick(p0: View) {
                activity.onLinkClick(getString(R.string.default_terms_url))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val greyColor = getColorResource(R.color.darker_grey)
                ds.color = greyColor
            }
        }

        val cs1 = object: ClickableSpan() {
            override fun onClick(p0: View) {
                activity.onLinkClick(getString(R.string.default_privacy_url))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                val greyColor = getColorResource(R.color.darker_grey)
                ds.color = greyColor
            }
        }

        sp.setSpan(cs, 0, 20, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(cs1, 23, binding.termsPrivacy.text.length - 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.termsPrivacy.movementMethod = LinkMovementMethod.getInstance()

        binding.termsPrivacy.text = sp

        /* Experimental */
        viewModel.loginFormState.observe(viewLifecycleOwner) {
            val state = it ?: return@observe

            binding.signInBtn.isEnabled = state.isDataValid

            if (state.emailError != null) {
                if (email().isNotBlank()) {
                    binding.emailText.showError(getString(state.emailError))
                }
            }

            if (state.passwordError != null) {
                if (password().isNotBlank()) {
                    binding.passwordText.showError(getString(state.passwordError))
                }
            }
        }

        inputs.addAll(listOf(binding.emailText, binding.passwordText))
        inputs.forEach {
            it.onChange()
        }


        activity.shouldDelay = false

        binding.root.post {
            onSplashFinish()
        }

    }

    private fun onSplashFinish() {
        activity.splashFragment?.let {
            activity.supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
        activity.splashFragment = null
    }

    private fun TextInputLayout.onChange() {
        editText?.doAfterTextChanged {
            inputs.forEach {
                it.removeError()
            }
            onChangeJob?.cancel()
            onChangeJob = viewModel.loginDataChanged(email(), password())
        }
    }

    private fun email(): String {
        return binding.emailText.editText?.text?.trim().toString()
    }

    private fun password(): String {
        return binding.passwordText.editText?.text?.trim().toString()
    }

    private fun showDialog() {
        loadingFragment = MessageDialogFragment.builder("Signing in \uD83D\uDD13. Please wait for a while ... ")
            .setProgress()
            .build()

        loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)
    }

    private fun signIn() {

        hideKeyboard()

        binding.signInBtn.isEnabled = false

        showDialog()

        val email = email()
        val password = password()

        FireUtility.signIn2(email, password) { signInResult ->

            loadingFragment?.dismiss()
            binding.signInBtn.isEnabled = true

            when (signInResult){
                is Result.Error -> {
                    when (signInResult.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            binding.passwordText.showError("Either email or password do not match")
                        }
                        is FirebaseAuthInvalidUserException -> {
                            binding.passwordText.showError("No user account exists with this email")
                        }
                        is UserDocumentNotFoundException -> {
                            toast("This user is either deleted or has been blocked by the system. Create another account to start using this app.")
                        }
                        else -> {
                            binding.passwordText.showError("Unknown error occurred. Maybe check your internet connection.")
                        }
                    }
                }
                is Result.Success -> {
                    val currentUser = signInResult.data

                    onFinish(currentUser)
                }
            }
        }

        /*FireUtility.signIn(email, password) {
            if (it.isSuccessful) {
                Log.d(TAG, "Login successful")
            } else {
                binding.signInBtn.isEnabled = true
                loadingFragment?.dismiss()
                viewModel.setCurrentError(it.exception)

                when (it.exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        binding.passwordText.showError("Either email or password do not match")
                    }
                    is FirebaseAuthInvalidUserException -> {
                        binding.passwordText.showError("No user account exists with this email")
                    }
                    else -> {
                        binding.passwordText.showError("Unknown error occurred. Maybe check your internet connection.")
                    }
                }
            }
        }*/
    }

    private fun onFinish(currentUser: User) {
        if (currentUser.interests.isEmpty()) {
            findNavController().navigate(R.id.profileImageFragment)
        } else {
            findNavController().navigate(R.id.action_loginFragment_to_navigationHome)
        }
    }

    private fun signInWithGoogle() {

        showDialog()

        val signInIntent = googleSignInClient.signInIntent
        activity.requestGoogleSignInLauncher.launch(signInIntent)

    }

    override fun onSignedIn(user: FirebaseUser) {
        FireUtility.getUser(user.uid) { mUser ->
            if (mUser != null) {
                UserManager.updateUser(mUser)
                onFinish(mUser)
            } else {
                val localUser = User.newUser(user.uid, user.displayName!!, user.email!!, user.photoUrl)
                FireUtility.uploadUser(localUser) {
                    if (it.isSuccessful) {
                        UserManager.updateUser(localUser)
                        onFinish(localUser)
                    }
                }
            }
        }
    }

    override fun onError(throwable: Throwable) {
        loadingFragment?.dismiss()
        throwable.localizedMessage?.let {
            toast(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.detachFragmentWithGoogleSignInLauncher()
    }

}
