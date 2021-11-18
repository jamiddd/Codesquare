package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentLoginBinding
import com.jamid.codesquare.databinding.InputLayoutBinding
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginFragment: Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.hide()

        val toolbar = activity.findViewById<MaterialToolbar>(R.id.main_toolbar)
        toolbar.hide()

        binding.signUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_createAccountFragment, null, slideRightNavOptions())
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        binding.googleSignInBtn.setSize(SignInButton.SIZE_WIDE)

        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.signInBtn.setOnClickListener {
            hideKeyboard()
            val emailText = binding.emailText.editText?.text
            if (emailText.isNullOrBlank()) {
                toast("Email cannot be empty")
                return@setOnClickListener
            }

            val email = emailText.toString()

            if (!email.isValidEmail()) {
                toast("Email must be valid")
                return@setOnClickListener
            }

            val passwordText = binding.passwordText.editText?.text
            if (passwordText.isNullOrBlank()) {
                toast("Password cannot be empty")
                return@setOnClickListener
            }

            val password = passwordText.toString()

            if (!password.isValidPassword()) {
                toast("Not a valid password. Must be longer than 8 characters. Must include at least one letter, one number and one symbol")
                return@setOnClickListener
            }

            signIn(email, password)

        }

       /* Firebase.auth.addAuthStateListener {
            if (it.currentUser != null) {
                dialog?.dismiss()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment, null, slideRightNavOptions())
            }
        }*/

        viewModel.currentError.observe(viewLifecycleOwner) {
            if (it != null) {
                Log.e(TAG, it.localizedMessage.orEmpty())
            }
        }


        val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
        val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

        inputLayoutBinding.inputTextLayout.hint = "Write your email ..."

        binding.forgotPasswordBtn.setOnClickListener {

            MaterialAlertDialogBuilder(activity)
                .setTitle("Forgot password ? ")
                .setMessage("We will send you a mail on this address with the link for new password.")
                .setView(inputLayoutBinding.root)
                .setPositiveButton("Send") { a, b ->
                    val emailText = inputLayoutBinding.inputTextLayout.text
                    if (!emailText.isNullOrBlank()) {
                        val email = emailText.trim().toString()
                        Firebase.auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    toast("Check your mail for a link to reset your password", Toast.LENGTH_LONG)
                                } else {
                                    toast(it.exception?.localizedMessage.orEmpty())
                                }
                            }
                    } else {
                        toast("Write an email address so that we can send a link for renewing your password.")
                    }
                }.setNegativeButton("Cancel") { a, b ->
                    a.dismiss()
                }
                .show()
        }

    }

    private fun showDialog() {
        (activity as MainActivity).showLoadingDialog("Signing in .. Please wait for a while")
    }

    private fun signIn(email: String, password: String) {

        showDialog()

        FireUtility.signIn(email, password) {
            if (it.isSuccessful) {
                val user = it.result.user
                if (user != null) {
                    val ref = Firebase.firestore.collection("users").document(user.uid)
                    FireUtility.getDocument(ref) { it1 ->
                        if (it1.isSuccessful && it1.result.exists()) {
                            val localUser = it1.result.toObject(User::class.java)!!
                            viewModel.insertCurrentUser(localUser)
//                            viewModel.getChannelUsers(localUser.chatChannels)
                        } else {
                            Firebase.auth.signOut()
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun signInWithGoogle() {

        showDialog()

        val signInIntent = googleSignInClient.signInIntent
        (activity as MainActivity).requestGoogleSingInLauncher.launch(signInIntent)
    }

    companion object {
        private const val TAG = "LoginFragment"

    }

}