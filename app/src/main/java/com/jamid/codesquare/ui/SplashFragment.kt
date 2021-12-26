package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.databinding.FragmentSplashBinding

@ExperimentalPagingApi
class SplashFragment: Fragment() {

    private lateinit var binding: FragmentSplashBinding

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

        UserManager.authState.observe(viewLifecycleOwner) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                    if (UserManager.isEmailVerified) {
                        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                    } else {
                        findNavController().navigate(R.id.action_splashFragment_to_emailVerificationFragment)
                    }
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }
        }
    }

}