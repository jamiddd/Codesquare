package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.Fade.IN
import androidx.transition.Fade.OUT
import com.jamid.codesquare.databinding.FragmentSplashBinding

class SplashFragment: Fragment() {

    private lateinit var binding: FragmentSplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade(IN)
        exitTransition = Fade(OUT)
    }

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

       /* UserManager.authState.observe(viewLifecycleOwner) { isSignedIn ->
            if (isSignedIn != null) {
                if (isSignedIn) {
                    if (UserManager.isEmailVerified) {
                        findNavController().navigate(R.id.action_splashFragment_to_navigationHome)
                    } else {
                        findNavController().navigate(R.id.emailVerificationFragment)
                    }
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }*/
    }

}