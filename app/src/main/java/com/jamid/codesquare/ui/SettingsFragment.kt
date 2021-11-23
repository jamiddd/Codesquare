package com.jamid.codesquare.ui

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.InputLayoutBinding

class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)


        val updateProfileItem = findPreference<Preference>("update_profile")
        updateProfileItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            true
        }

        val savedProjectsItem = findPreference<Preference>("saved_projects")
        savedProjectsItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_savedProjectsFragment)
            true
        }

        val updatePasswordItem = findPreference<Preference>("update_password")
        updatePasswordItem?.setOnPreferenceClickListener {

            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Enter old password ..."
            inputLayoutBinding.inputTextLayout.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update password")
                .setMessage("Enter your old password to set new password")
                .setView(inputLayoutBinding.root)
                .setPositiveButton("Next") { d, w ->
                    val text = inputLayoutBinding.inputTextLayout.text
                    if (!text.isNullOrBlank()) {
                        val oldPassword = text.toString()
                        if (oldPassword.isValidPassword()) {

                            val currentUser = Firebase.auth.currentUser
                            if (currentUser != null) {

                                val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)

                                currentUser.reauthenticate(credential)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {

                                            val inputLayout1 = layoutInflater.inflate(R.layout.input_layout, null, false)
                                            val inputLayoutBinding1 = InputLayoutBinding.bind(inputLayout1)

                                            inputLayoutBinding1.inputTextLayout.hint = "Enter new password ... "
                                            inputLayoutBinding1.inputTextLayout.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

                                            MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Updating password .. ")
                                                .setMessage("Enter a new password to update your password. The password must contain 1 number, 1 capital letter, 1 small letter and 1 symbol.")
                                                .setView(inputLayoutBinding1.root)
                                                .setPositiveButton("Update") { d, w ->

                                                    val text1 = inputLayoutBinding1.inputTextLayout.text
                                                    if (!text1.isNullOrBlank()) {

                                                        val newPassword = text1.trim().toString()
                                                        if (newPassword.isValidPassword()) {

                                                            currentUser.updatePassword(newPassword)
                                                                .addOnCompleteListener { it1 ->
                                                                    if (it1.isSuccessful) {
                                                                        toast("Password updated successfully.")
                                                                    } else {
                                                                        toast("Something went wrong. Try again later.")
                                                                    }
                                                                }

                                                        } else {
                                                            toast("Not a valid password. Try again from the start.")
                                                        }
                                                    } else {
                                                        toast("No changes made")
                                                    }

                                                }.setNegativeButton("Cancel") {d, w ->
                                                    d.dismiss()
                                                }
                                                .show()

                                        } else {
                                            toast("The password you entered in wrong.")
                                        }
                                    }

                            }

                        } else {
                            toast("Not a valid password. Try again")
                        }
                    } else {
                        toast("You did not enter anything")
                    }
                }.setNegativeButton("Cancel") {d, w ->
                    d.dismiss()
                }
                .show()


            true
        }

        val forgotPasswordItem = findPreference<Preference>("forgot_password")
        forgotPasswordItem?.setOnPreferenceClickListener {

            val inputLayout = layoutInflater.inflate(R.layout.input_layout, null, false)
            val inputLayoutBinding = InputLayoutBinding.bind(inputLayout)

            inputLayoutBinding.inputTextLayout.hint = "Write your email ... "

            MaterialAlertDialogBuilder(requireContext())
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
            true
        }

        val logoutItem = findPreference<Preference>("logout")
        logoutItem?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logging out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out") { d, w ->
                    Firebase.auth.signOut()
                    viewModel.signOut()
                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment, null, slideRightNavOptions())
                }.setNegativeButton("Cancel") { d, w ->
                    d.dismiss()
                }
                .show()

            true
        }

        val providerData = Firebase.auth.currentUser?.providerData
        if (providerData != null) {
            for (data in providerData) {
                if (data.providerId == "google.com") {
                    updatePasswordItem?.isEnabled = false
                    forgotPasswordItem?.isEnabled = false
                }
            }
        }

        val feedbackItem = findPreference<Preference>("feedback")
        feedbackItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_feedbackFragment)
            true
        }

    }

    companion object {
        private const val TAG = "SettingsFragment"
    }

}