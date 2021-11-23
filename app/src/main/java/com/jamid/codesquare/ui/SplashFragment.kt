package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.CREATED_AT
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment: Fragment() {

    private lateinit var binding: FragmentSplashBinding
//    private val viewModel: MainViewModel by activityViewModels()

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

        /*if (Firebase.auth.currentUser != null) {
            Firebase.firestore.collection("projects")
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Something went wrong when prefetching projects")
                    } else {
                        val projects = it.result.toObjects(Project::class.java)
                        viewModel.insertProjects(projects)
                    }
                }
        }*/
    }

    companion object {
        private const val TAG = "SplashFrag"
    }

}