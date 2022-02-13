package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.ProjectListAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ProjectListLayoutBinding
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener

@ExperimentalPagingApi
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
            currentUserId = UserManager.currentUserId
        }

        binding.projectsList.apply {
            adapter = projectListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.getCurrentUserProjects().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                if (receiver.projectsCount.toInt() != it.size) {
                    // download all projects
                    FireUtility.downloadAllUserProjects { it1 ->
                        when (it1) {
                            is Result.Error -> viewModel.setCurrentError(it1.exception)
                            is Result.Success -> {
                                viewModel.insertProjects(*it1.data.toTypedArray())
                            }
                            null -> {
                                // no projects
                            }
                        }
                    }
                }
                projectListAdapter.submitList(it)
            }
        }



    }

    companion object {
        fun newInstance(user: User) = ProjectListFragment().apply {
            arguments = bundleOf("user" to user)
        }
    }

    override fun onInviteClick(project: Project, receiverId: String) {
        val currentUser = UserManager.currentUser
        val title = project.name
        val content = currentUser.name + " has invited you to join their project: ${project.name}"
        val notification = Notification.createNotification(content, currentUser.id, receiverId, type = -1,  projectId = project.id, title = title)

        FireUtility.inviteUserToProject(project, receiverId, notification.id) {
            if (it.isSuccessful) {
                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            viewModel.setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (it1.isSuccessful) {
                                        viewModel.insertNotifications(notification)
                                    } else {
                                        viewModel.setCurrentError(it1.exception)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                viewModel.setCurrentError(it.exception)
            }
        }
    }

    override fun onRevokeInviteClick(invite: ProjectInvite, receiverId: String) {
        FireUtility.revokeInvite(invite) {
            if (!it.isSuccessful) {
                viewModel.setCurrentError(it.exception)
            }
        }
    }


}