package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProjectContributorsBinding

@ExperimentalPagingApi
class ProjectContributorsFragment: Fragment() {

    private lateinit var binding: FragmentProjectContributorsBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var project: Project
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProjectContributorsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        project = arguments?.getParcelable(PROJECT) ?: return
        val prefetchedContributors = arguments?.getParcelableArrayList<User>("contributors") ?: arrayListOf()

        var creator = User()

        userAdapter = UserAdapter(small = true, grid = true)

        binding.contributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        userAdapter.submitList(prefetchedContributors)

        getContributors()

        binding.contributorsRefresher.setOnRefreshListener {
            getContributors()
        }

    }

    private fun getContributors() {
        Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNELS, project.chatChannel)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {
                    val contributors = mutableListOf<User>()
                    val users = it.toObjects(User::class.java)
                    contributors.addAll(users)
                    userAdapter.submitList(contributors)

                    for (user in users) {
                        viewModel.insertUserToCache(user)
                    }

                }
            }.addOnFailureListener {
                Log.e(TAG, "onViewCreated: ${it.localizedMessage}")
            }
    }

    companion object {
        const val TAG = "ContributorsFragment"

        fun newInstance(bundle: Bundle) =
            ProjectContributorsFragment().apply {
                arguments = bundle
            }

    }

}