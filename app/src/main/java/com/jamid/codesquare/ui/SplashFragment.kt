package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentSplashBinding
import com.jamid.codesquare.ui.home.chat.ChatInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment: Fragment() {

    private lateinit var binding: FragmentSplashBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mAuth = Firebase.auth
        val extras = FragmentNavigatorExtras(binding.splashLogo to "logo_transition")
        if (mAuth.currentUser != null) {
            if (mAuth.currentUser!!.isEmailVerified) {
                viewModel.currentUser.observe(viewLifecycleOwner) {
                    if (it != null) {
                        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                    }
                }
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment, null, null, extras)
            }
        } else {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment, null, null, extras)
        }
    }

    companion object {
        private const val TAG = "SplashFrag"
    }

}