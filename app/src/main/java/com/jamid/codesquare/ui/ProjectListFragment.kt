package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.ProjectListAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectInvite
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.ProjectListLayoutBinding
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener

class ProjectListFragment: BottomSheetDialogFragment(), ProjectMiniItemClickListener {

    private lateinit var binding: ProjectListLayoutBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ProjectListLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val receiver = arguments?.getParcelable<User>("user") ?: return

        val projectListAdapter = ProjectListAdapter(this).apply {
            receiverIdForInvite = receiver.id
            currentUserId = viewModel.currentUser.value?.id.orEmpty()
        }

        binding.projectsList.apply {
            adapter = projectListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.getCurrentUserProjects().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                projectListAdapter.submitList(it)
            }
        }

    }

    companion object {
        private const val TAG = "ProjectListFragment"

        fun newInstance(user: User) = ProjectListFragment().apply {
            arguments = bundleOf("user" to user)
        }
    }

    override fun onInviteClick(project: Project, receiverId: String) {
        viewModel.inviteUserToProject(project, receiverId)
    }

    override fun onRevokeInviteClick(invite: ProjectInvite, receiverId: String) {
        viewModel.revokeInvite(invite, receiverId)
    }


}