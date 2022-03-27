package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.R

@ExperimentalPagingApi
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var mScroll = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER

        listView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mScroll += dy
                if (mScroll < 100) {
                    setSubscriptionSetting(listView)
                }
            }
        })

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

//        setSubscriptionSetting()

    }

    private fun removePreference(preference: Preference) {
        val parent = getParent(preferenceScreen, preference)
            ?: throw RuntimeException("Couldn't find preference")
        parent.removePreference(preference)
    }

    private fun getParent(
        groupToSearchIn: PreferenceGroup,
        preference: Preference
    ): PreferenceGroup? {
        for (i in 0 until groupToSearchIn.preferenceCount) {
            val child = groupToSearchIn.getPreference(i)
            if (child === preference) return groupToSearchIn
            if (child is PreferenceGroup) {
                val result = getParent(child, preference)
                if (result != null) return result
            }
        }
        return null
    }

    private fun setSubscriptionSetting(v: RecyclerView) {

        val context = v.context
        val accentColor = context.accentColor()
        val mainActivity = context as MainActivity


        val upgradePlanBtn = v.findViewById<MaterialButton>(R.id.upgrade_plan_btn)
        val currentPlanHeader = v.findViewById<TextView>(R.id.current_plan_header)
        val currentPlanDesc = v.findViewById<TextView>(R.id.current_plan_text)
        val progress = v.findViewById<ProgressBar>(R.id.subscription_setting_progress_bar)

        progress?.hide()

        val currentUser = UserManager.currentUser
        when (currentUser.premiumState.toInt()) {
            -1 -> {
                upgradePlanBtn?.text = getString(R.string.upgrade_plan)

                upgradePlanBtn?.show()

                mainActivity.subscriptionFragment = SubscriptionFragment()

                upgradePlanBtn?.setOnClickListener {
                    mainActivity.subscriptionFragment?.show(
                        mainActivity.supportFragmentManager,
                        "SubscriptionFragment"
                    )
                }
                currentPlanHeader?.text = getString(R.string.empty_subscriptions)
                currentPlanDesc?.text = getString(R.string.empty_subscripitons_desc)
            }
            0 -> {
                // just some changes that needs to be done if the button is visible
                upgradePlanBtn?.rippleColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.lightest_red
                    )
                )
                upgradePlanBtn?.text = getString(R.string.remove_subscription)
                upgradePlanBtn.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.error_color
                    )
                )

                upgradePlanBtn?.hide()

                currentPlanHeader?.text =
                    getString(R.string.base_subscription).uppercase()
                currentPlanHeader?.setTextColor(accentColor)
                currentPlanDesc?.text =
                    getString(R.string.base_subscription_desc)
            }
            1 -> {
                // just some changes that needs to be done if the button is visible
                upgradePlanBtn?.rippleColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.lightest_red
                    )
                )
                upgradePlanBtn?.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.error_color
                    )
                )
                upgradePlanBtn?.text = getString(R.string.remove_subscription)

                upgradePlanBtn?.hide()

                currentPlanHeader?.text =
                    getString(R.string.premium_subscriptions).uppercase()
                currentPlanHeader?.setTextColor(accentColor)
                currentPlanDesc?.text =
                    getString(R.string.premium_subscription_desc)
            }
        }

        currentPlanHeader?.show()
        currentPlanDesc?.show()

    }

    private fun setProjectSection() {
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
        val radiusS = sharedPreference.getString(LOCATION_RADIUS, ONE)
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

        val myRequests = findPreference<Preference>("my_requests")
        myRequests?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.myRequestsFragment, null, slideRightNavOptions())
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
                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment, null, slideRightNavOptions())
                viewModel.signOut {}
            }
            true
        }

        val feedbackItem = findPreference<Preference>("feedback")
        feedbackItem?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_feedbackFragment, null, slideRightNavOptions())
            true
        }
    }

    companion object {
        const val TAG = "SettingsFragment"
    }

}