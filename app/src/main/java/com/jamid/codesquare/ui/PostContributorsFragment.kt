package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentPostContributorsBinding

@ExperimentalPagingApi
class PostContributorsFragment: BaseFragment<FragmentPostContributorsBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var post: Post
    private lateinit var userAdapter: UserAdapter

    override fun getViewBinding(): FragmentPostContributorsBinding {
        return FragmentPostContributorsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if ((parentFragment as PostFragmentContainer).getCurrentFragmentTag() == TAG) {
            activity.binding.mainToolbar.menu.clear()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post = arguments?.getParcelable(POST) ?: return
        val title = arguments?.getString(TITLE)
        val subtitle = arguments?.getString(SUB_TITLE)

        (activity as MainActivity?)?.binding?.mainToolbar?.title = title
        (activity as MainActivity?)?.binding?.mainToolbar?.subtitle = subtitle

        userAdapter = UserAdapter(small = true, grid = true)

        binding.contributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        getContributors()

        binding.contributorsRefresher.setOnRefreshListener {
            getContributors()
        }

    }

    private fun getContributors() {
        Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNELS, post.chatChannel)
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
            PostContributorsFragment().apply {
                arguments = bundle
            }

    }

}