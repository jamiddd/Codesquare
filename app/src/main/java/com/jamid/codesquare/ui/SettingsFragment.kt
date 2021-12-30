package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R

@ExperimentalPagingApi
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView?.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        editor = sharedPreference.edit()

        setProfileSection()

        setProjectSection()

        setLocationSection()

        setChatSection()

        setAccountSection()

        setOtherFunctions()

    }

    private fun setProjectSection() {
        val expiryS = sharedPreference.getString(PROJECT_EXPIRY, "0")
        val projectExpiry = findPreference<EditTextPreference>(PROJECT_EXPIRY)
        if (expiryS != "0") {
            projectExpiry?.setDefaultValue(expiryS)
            projectExpiry?.summary = "$expiryS Days"
        }
        projectExpiry?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }

        projectExpiry?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                if (newValue.isDigitsOnly()) {
                    editor.putString(PROJECT_EXPIRY, newValue)
                    editor.apply()

                    projectExpiry.summary = "$newValue Days"
                }
            }
            true
        }

    }

    private fun setLocationSection() {

        val radiusS = sharedPreference.getString(LOCATION_RADIUS, "1")
        val locationRadius = findPreference<EditTextPreference>(LOCATION_RADIUS)
        if (radiusS != "1") {
            locationRadius?.setDefaultValue(radiusS)
            locationRadius?.summary = "$radiusS KM"
        }

        locationRadius?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }

        locationRadius?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                if (newValue.isDigitsOnly()) {
                    editor.putString(LOCATION_RADIUS, newValue)
                    editor.apply()

                    locationRadius.summary = "$newValue KM"
                }
            }
            true
        }

    }

    private fun setProfileSection() {

        val updateProfile = findPreference<Preference>(PROFILE_UPDATE)
        updateProfile?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment, null, slideRightNavOptions())
            true
        }

        val savedProjects = findPreference<Preference>(PROFILE_SAVED)
        savedProjects?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_savedProjectsFragment, null, slideRightNavOptions())
            true
        }

        val archivedProjects = findPreference<Preference>(PROFILE_ARCHIVED)
        archivedProjects?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_archiveFragment, null, slideRightNavOptions())
            true
        }
    }

    @SuppressLint("InflateParams")
    private fun setAccountSection() {

        val updatePasswordItem = findPreference<Preference>(ACCOUNT_UPDATE_PASSWORD)
        updatePasswordItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_updatePasswordFragment, null, slideRightNavOptions())
            true
        }

        val forgotPasswordItem = findPreference<Preference>(ACCOUNT_FORGOT_PASSWORD)
        forgotPasswordItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_forgotPasswordFragment, null, slideRightNavOptions())
            true
        }


        val providerData = Firebase.auth.currentUser?.providerData
        if (providerData != null) {
            if (providerData.size == 1 && providerData.first().providerId == "google.com") {
                updatePasswordItem?.isEnabled = false
                forgotPasswordItem?.isEnabled = false
            }
        }
    }

    private fun setChatSection() {
        val showChatTime = findPreference<SwitchPreferenceCompat>("show_chat_time")
        val sharedPreferences = activity?.getSharedPreferences("codesquare_shared", AppCompatActivity.MODE_PRIVATE)
        showChatTime?.setOnPreferenceChangeListener { _, newValue ->
            val editor = sharedPreferences?.edit()
            if (newValue is Boolean) {
                editor?.putBoolean("show_chat_time", newValue)
                editor?.apply()
            }
            true
        }

        showChatTime?.setDefaultValue(sharedPreferences?.getBoolean("show_chat_time", true))

    }

    private fun setOtherFunctions() {
        val logoutItem = findPreference<Preference>("logout")
        logoutItem?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logging out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out") { _, _ ->
                    Firebase.auth.signOut()
                    UserManager.setAuthStateForceful(false)
                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment, null, slideRightNavOptions())
                    viewModel.signOut {}
                }.setNegativeButton("Cancel") { d, _ ->
                    d.dismiss()
                }
                .show()

            true
        }

        val feedbackItem = findPreference<Preference>("feedback")
        feedbackItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_feedbackFragment, null, slideRightNavOptions())
            true
        }
    }

}