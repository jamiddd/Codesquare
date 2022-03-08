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
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.Result
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

        binding.contributorsRefresher.setOnRefreshListener {
            initProcess()
        }

        initProcess()

    }

    private fun initProcess() {
        getChatChannel()
    }

    // TODO("Do we really need to download the chat channel, take notice")
    private fun getChatChannel() {
        FireUtility.getChatChannel(project.chatChannel) {
            when (it) {
                is Result.Error -> viewModel.setCurrentError(it.exception)
                is Result.Success -> {
                    onReceiveChatChannel(it.data)
                }
                null -> Log.w(TAG, "Something went wrong while fetching chat channel with id: ${project.chatChannel}")
            }
        }
    }

    private fun onReceiveChatChannel(chatChannel: ChatChannel) {
        userAdapter = UserAdapter(small = true, grid = true, associatedChatChannel = chatChannel)

        binding.contributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        getProjectContributors()
    }

    private fun updateUi(isEmpty: Boolean) {
        if (isEmpty) {
            binding.contributorsRecycler.hide()
            binding.noContributorsText.show()
        } else {
            binding.contributorsRecycler.show()
            binding.noContributorsText.hide()
        }
    }

    private fun getProjectContributors() {
        FireUtility.getProjectContributors(project) {
            if (it.isSuccessful) {
                updateUi(false)
                if (!it.result.isEmpty) {

                    val contributors = it.result.toObjects(User::class.java)
                    onFetchUsers(contributors)

                } else {
                    onFetchUsers()
                }
            } else {
                updateUi(true)
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    private fun onFetchUsers(contributors: List<User> = emptyList()) {
        viewModel.getLocalUser(project.creator.userId) { creator ->
            requireActivity().runOnUiThread {
                val x = mutableListOf<User>()
                if (creator != null) {
                    x.add(creator)
                    x.addAll(contributors)
                }
                viewModel.insertUsers(x)
                binding.contributorsRefresher.isRefreshing = false
                userAdapter.submitList(x)
            }
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