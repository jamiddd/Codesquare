package com.jamid.codesquare.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.jamid.codesquare.databinding.LoadingLayoutBinding
import com.jamid.codesquare.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginFragment: Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var dialog: AlertDialog? = null

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

            viewModel.currentUser.observe(viewLifecycleOwner) {
                if (it != null) {
                    dialog?.dismiss()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment, null, slideRightNavOptions())
                }
            }
        }

        viewModel.currentError.observe(viewLifecycleOwner) {
            if (it != null) {
                Log.e(TAG, it.localizedMessage.orEmpty())
                dialog?.dismiss()
            }
        }

    }

    private fun showDialog() {

        val loadingLayout = layoutInflater.inflate(R.layout.loading_layout, null, false)
        val loadingLayoutBinding = LoadingLayoutBinding.bind(loadingLayout)

        loadingLayoutBinding.loadingText.text = "Signing in .. Please wait for a while"

        dialog =  MaterialAlertDialogBuilder(requireContext())
            .setView(loadingLayout)
            .setCancelable(false)
            .show()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(8000)
            dialog?.dismiss()
            if (viewModel.currentUser.value != null) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment, null, slideRightNavOptions())
            } else {
                toast("Sign in again")
            }
        }

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