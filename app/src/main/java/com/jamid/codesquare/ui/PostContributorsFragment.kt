package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentPostContributorsBinding
// something simple
class PostContributorsFragment: BaseFragment<FragmentPostContributorsBinding>() {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var post: Post
    private lateinit var userAdapter: UserAdapter

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPostContributorsBinding {
        return FragmentPostContributorsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post = arguments?.getParcelable(POST) ?: return
        val title = arguments?.getString(TITLE)
        val subtitle = arguments?.getString(SUB_TITLE)

        activity.binding.mainToolbar.title = title
        activity.binding.mainToolbar.subtitle = subtitle

        userAdapter = UserAdapter()

        binding.contributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        }

        getContributors()

        binding.contributorsRefresher.setDefaultSwipeRefreshLayoutUi()

        binding.contributorsRefresher.setOnRefreshListener {
            getContributors()
        }

    }

    private fun getContributors() {
        Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNELS, post.chatChannel)
            .get()
            .addOnSuccessListener {

                binding.contributorsRefresher.isRefreshing = false

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