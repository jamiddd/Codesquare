package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
// something simple
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        sharedPreference = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        editor = sharedPreference.edit()

        setProfileSection()

        setPostSection()

        setLocationSection()

        setChatSection()

        setAccountSection()

        setOtherFunctions()

        val addInterest = findPreference<Preference>("admin_functions")
        val acceptedEmails = listOf("jamiddeka1@gmail.com", "lunabarua1@gmail.com", "lunabarua1@icloud.com")
        if (!acceptedEmails.contains(UserManager.currentUser.email)) {
            val adminSection = findPreference<PreferenceCategory>("admin_section")
            adminSection?.isVisible = false
        } else {
            addInterest?.setOnPreferenceClickListener {
                findNavController().navigate(R.id.extraFragment, null)
                true
            }
        }

        if (UserManager.currentUser.premiumState.toInt() == 1) {
            val pref = findPreference<PreferenceCategory>("subscription_category")
            pref?.isVisible = false
        }

    }

//    private fun setSubscriptionSetting(v: RecyclerView) {
//
//        val context = v.context
//        val accentColor = context.accentColor()
//        val mainActivity = context as MainActivity
//
//
//        val parent = v.findViewById<ConstraintLayout>(R.id.upgrade_plan_layout)
//        val upgradePlanBtn = v.findViewById<MaterialButton>(R.id.upgrade_plan_btn)
//        val currentPlanHeader = v.findViewById<TextView>(R.id.current_plan_header)
//        val currentPlanDesc = v.findViewById<TextView>(R.id.current_plan_text)
//        val progress = v.findViewById<ProgressBar>(R.id.subscription_setting_progress_bar)
//
//        progress?.hide()
//
//        val currentUser = UserManager.currentUser
//        when (currentUser.premiumState.toInt()) {
//            -1 -> {
//                upgradePlanBtn?.text = getString(R.string.upgrade_plan)
//
//                upgradePlanBtn?.show()
//
//                mainActivity.subscriptionFragment = SubscriptionFragment()
//
//                upgradePlanBtn?.setOnClickListener {
//                    mainActivity.subscriptionFragment?.show(
//                        mainActivity.supportFragmentManager,
//                        "SubscriptionFragment"
//                    )
//                }
//                currentPlanHeader?.text = getString(R.string.empty_subscriptions)
//                currentPlanDesc?.text = getString(R.string.empty_subscriptions_desc)
//            }
//            0 -> {
//                // just some changes that needs to be done if the button is visible
//                upgradePlanBtn?.rippleColor = ColorStateList.valueOf(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.lightest_red
//                    )
//                )
//                upgradePlanBtn?.text = getString(R.string.remove_subscription)
//                upgradePlanBtn.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.error_color
//                    )
//                )
//
//                upgradePlanBtn?.hide()
//
//                currentPlanHeader?.text =
//                    getString(R.string.base_subscription).uppercase()
//                currentPlanHeader?.setTextColor(accentColor)
//                currentPlanDesc?.text =
//                    getString(R.string.base_subscription_desc)
//            }
//            1 -> {
//
//                val pref = findPreference<PreferenceCategory>("subscription_category")
//                pref?.isVisible = false
//
//                /*// just some changes that needs to be done if the button is visible
//                upgradePlanBtn?.rippleColor = ColorStateList.valueOf(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.lightest_red
//                    )
//                )
//                upgradePlanBtn?.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.error_color
//                    )
//                )
//                upgradePlanBtn?.text = getString(R.string.remove_subscription)
//
//                upgradePlanBtn?.hide()
//
//                currentPlanHeader?.text =
//                    getString(R.string.premium_subscriptions).uppercase()
//                currentPlanHeader?.setTextColor(accentColor)
//                currentPlanDesc?.text =
//                    getString(R.string.premium_subscription_desc)*/
//            }
//        }
//
//        currentPlanHeader?.show()
//        currentPlanDesc?.show()
//
//    }

    private fun setPostSection() {
        /*val expiryS = sharedPreference.getString(PROJECT_EXPIRY, "0")
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
        }*/
    }

    private fun setLocationSection() {
        /*val radiusS = sharedPreference.getString(LOCATION_RADIUS, ONE)
        val locationRadius = findPreference<EditTextPreference>(LOCATION_RADIUS)
        if (radiusS != ONE) {
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
        }*/

    }

    private fun setProfileSection() {

        val updateProfile = findPreference<Preference>(PROFILE_UPDATE)
        updateProfile?.setOnPreferenceClickListener {

            val currentUser = UserManager.currentUser
            viewModel.setCurrentImage(currentUser.photo.toUri())

            findNavController().navigate(R.id.editProfileFragment)
            true
        }

        val savedPosts = findPreference<Preference>(PROFILE_SAVED)
        savedPosts?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.savedPostsFragment)
            true
        }

        val archivedPosts = findPreference<Preference>(PROFILE_ARCHIVED)
        archivedPosts?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.archiveFragment)
            true
        }

        val myRequests = findPreference<Preference>("my_requests")
        myRequests?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.myRequestsFragment)
            true
        }

        val sentInvites = findPreference<Preference>("profile_sent_invites")
        sentInvites?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.invitesFragment, null)
            true
        }

        val blockedAccounts = findPreference<Preference>("profile_blocked_accounts")
        blockedAccounts?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.blockedAccountsFragment, null)
            true
        }
    }

    @SuppressLint("InflateParams")
    private fun setAccountSection() {

        val updatePasswordItem = findPreference<Preference>(ACCOUNT_UPDATE_PASSWORD)
        updatePasswordItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.updatePasswordFragment)
            true
        }

        val forgotPasswordItem = findPreference<Preference>(ACCOUNT_FORGOT_PASSWORD)
        forgotPasswordItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
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
       /* val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val showChatTime = findPreference<SwitchPreferenceCompat>("chat_time")
        showChatTime?.setOnPreferenceChangeListener { _, newValue ->
            val editor = sharedPreferences?.edit()
            if (newValue is Boolean) {
                editor?.putBoolean("chat_time", newValue)
                editor?.apply()
            }
            true
        }

        showChatTime?.setDefaultValue(sharedPreferences?.getBoolean("chat_time", true))

        /////////////////////////

        val automaticDownload = findPreference<SwitchPreferenceCompat>("chat_download")
        automaticDownload?.setOnPreferenceChangeListener { _, newValue ->
            val editor = sharedPreferences?.edit()
            if (newValue is Boolean) {
                editor?.putBoolean("chat_download", newValue)
                editor?.apply()
            }
            true
        }

        automaticDownload?.setDefaultValue(sharedPreferences?.getBoolean("chat_download", false))*/

    }

    private fun setOtherFunctions() {
        val logoutItem = findPreference<Preference>("logout")
        logoutItem?.setOnPreferenceClickListener {
            UserManager.logOut(requireContext()) {
                findNavController().navigate(R.id.action_settingsFragment_to_navigation_auth)
                viewModel.signOut {}
            }
            true
        }

        val feedbackItem = findPreference<Preference>("feedback")
        feedbackItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.feedbackFragment)
            true
        }

        val termsAndConditions = findPreference<Preference>("terms_and_conditions")
        termsAndConditions?.setOnPreferenceClickListener {
            (activity as MainActivity).onLinkClick("https://www.collabmee.com/terms")
            true
        }

        val privacyPolicy =findPreference<Preference>("privacy_policy")
        privacyPolicy?.setOnPreferenceClickListener {
            (activity as MainActivity).onLinkClick("https://www.collabmee.com/privacy")
            true
        }

        val version = findPreference<Preference>("version")
        version?.summary = BuildConfig.VERSION_NAME

    }

    companion object {
        const val TAG = "SettingsFragment"
    }

}