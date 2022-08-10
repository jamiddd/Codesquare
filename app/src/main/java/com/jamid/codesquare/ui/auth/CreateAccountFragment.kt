package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentCreateAccountBinding
import com.jamid.codesquare.ui.MessageDialogFragment
import kotlinx.coroutines.Job

@ExperimentalPagingApi
class CreateAccountFragment : BaseFragment<FragmentCreateAccountBinding>() {

    private var loadingFragment: MessageDialogFragment? = null

    private var onChangeJob: Job? = null

    private val inputs = mutableListOf<TextInputLayout>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInLinkBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        inputs.addAll(
            listOf(
                binding.nameText,
                binding.emailText,
                binding.passwordText,
                binding.confirmPasswordText
            )
        )
        inputs.forEach { i ->
            i.onChange()
        }

        viewModel.registerFormState.observe(viewLifecycleOwner) {
            val state = it ?: return@observe

            binding.createBtn.isEnabled = state.isDataValid

            if (state.nameError != null) {
                if (name().isNotBlank()) {
                    binding.nameText.showError(getString(state.nameError))
                }
            }

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

            if (state.confirmPasswordError != null) {
                if (confirmPassword().isNotBlank()) {
                    binding.confirmPasswordText.showError(getString(state.confirmPasswordError))
                }
            }

        }


        binding.createBtn.setOnClickListener {
            createAccount()
        }

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        /*UserManager.authState.observe(viewLifecycleOwner) { isSignedIn ->
            loadingFragment?.dismiss()
            binding.createBtn.isEnabled = true
            if (isSignedIn != null && isSignedIn) {
                // Change here
                findNavController().navigate(
                    R.id.emailVerificationFragment
                )
            }
        }
*/

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

    private fun name(): String {
        return binding.nameText.editText?.text?.trim().toString()
    }

    private fun email(): String {
        return binding.emailText.editText?.text?.trim().toString()
    }

    private fun password(): String {
        return binding.passwordText.editText?.text?.trim().toString()
    }

    private fun confirmPassword(): String {
        return binding.confirmPasswordText.editText?.text?.trim().toString()
    }

    fun TextInputLayout.onChange() {
        editText?.doAfterTextChanged {
            inputs.forEach {
                it.removeError()
            }
            onChangeJob?.cancel()
            onChangeJob =
                viewModel.registerDataChanged(name(), email(), password(), confirmPassword())
        }
    }

    private fun showDialog() {
        val msg = "Creating account .. Please wait for a while"
        loadingFragment = MessageDialogFragment.builder(msg)
            .setProgress()
            .build()

        loadingFragment?.show(childFragmentManager, MessageDialogFragment.TAG)
    }

    private fun createAccount() {

        binding.createBtn.isEnabled = false

        showDialog()

        val name = name()
        val email = email()
        val password = password()

        FireUtility.createAccount2(name, email, password) { result ->
            when (result) {
                is Result.Error -> {
                    binding.createBtn.isEnabled = true
                    Log.e(TAG, "createAccount: ${result.exception.localizedMessage}")
                }
                is Result.Success -> {
                    UserManager.updateUser(result.data)
                    viewModel.insertCurrentUser(result.data)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CreateAccountFragment"
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentCreateAccountBinding {
        return FragmentCreateAccountBinding.inflate(inflater)
    }

}