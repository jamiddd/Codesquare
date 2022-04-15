package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ProjectListAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ProjectListLayoutBinding
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener

@ExperimentalPagingApi
class ProjectListFragment: RoundedBottomSheetDialogFragment(), ProjectMiniItemClickListener {

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

        val receiver = arguments?.getParcelable<User>(USER) ?: return

        val projectListAdapter = ProjectListAdapter(receiver.id, this)

        binding.projectsList.apply {
            adapter = projectListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.getCurrentUserProjects().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                val currentUser = UserManager.currentUser

                // if all the projects are not downloaded for some reason, download it now
                if (currentUser.projectsCount.toInt() != it.size) {
                    FireUtility.downloadAllUserProjects { it1 ->
                        val allProjectsResult = it1 ?: return@downloadAllUserProjects
                        when (allProjectsResult) {
                            is Result.Error -> viewModel.setCurrentError(allProjectsResult.exception)
                            is Result.Success -> {
                                viewModel.insertProjects(allProjectsResult.data)
                            }
                        }
                    }
                }
                projectListAdapter.submitList(it)
            }
        }

    }

    companion object {

        const val TAG = "ProjectListFragment"

        fun newInstance(user: User) = ProjectListFragment().apply {
            arguments = bundleOf(USER to user)
        }
    }

    override fun onInviteClick(project: Project, receiverId: String, onFailure: () -> Unit) {
        val currentUser = UserManager.currentUser
        if (project.contributors.size < 5 || currentUser.premiumState.toInt() == 1) {
            val title = project.name
            val content = currentUser.name + " has invited you to join their project: ${project.name}"
            val notification = Notification.createNotification(
                content,
                receiverId,
                type = -1,
                projectId = project.id,
                title = title
            )

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
        } else {

            onFailure()

            val upgradeMsg = getString(R.string.upgrade_plan_imsg)
            val frag = MessageDialogFragment.builder(upgradeMsg)
                .setPositiveButton(getString(R.string.upgrade)) { _, _ ->
                    (activity as MainActivity?)?.showSubscriptionFragment()
                }
                .setNegativeButton(getString(R.string.cancel)){ a, _ ->
                    a.dismiss()
                }.build()

            frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
        }
    }

    override fun onRevokeInviteClick(invite: ProjectInvite, onFailure: () -> Unit) {
        FireUtility.revokeInvite(invite) {
            if (!it.isSuccessful) {
                Snackbar.make(binding.root, "Something went wrong while trying to revoke project invite", Snackbar.LENGTH_LONG).show()
                viewModel.setCurrentError(it.exception)
                onFailure()
            }
        }
    }


}