package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.ProjectComparator
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.ProjectListItemBinding
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener

class ProjectListAdapter(
    private val receiverId: String,
    private val projectMiniItemClickListener: ProjectMiniItemClickListener
) : ListAdapter<Project, ProjectListAdapter.ProjectListItemViewHolder>(ProjectComparator()) {

    inner class ProjectListItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(project: Project) {
            val binding = ProjectListItemBinding.bind(view)
            binding.projectMiniImage.setImageURI(project.images.firstOrNull())
            binding.projectMiniName.text = project.name

            binding.inviteBtnProgress.show()
            binding.projectMiniInviteBtn.disappear()

            // checking if the user is already a contributor
            if (project.contributors.contains(receiverId)) {
                // hide invite button
                binding.inviteBtnProgress.hide()
                binding.projectMiniInviteBtn.hide()
            } else {
                // if the user is not already a contributor, check if the user has sent request to the current user
                FireUtility.getProjectRequest(project.id, receiverId) {
                    when (it) {
                        is Result.Error -> Log.e(TAG, it.exception.localizedMessage.orEmpty())
                        is Result.Success -> {
                            // since there is already a request by this user, somehow need to show the user that there is a request,
                            // or make accept reject button right here, maybe later
                            binding.inviteBtnProgress.hide()
                            binding.projectMiniInviteBtn.hide()
                        }
                        null -> {
                            // there is no existing project request by this user, check if we have already sent invite to this user
                            FireUtility.getExistingInvite(
                                project.id,
                                receiverId,
                                UserManager.currentUserId
                            ) { it1 ->

                                binding.inviteBtnProgress.hide()
                                binding.projectMiniInviteBtn.show()

                                when (it1) {
                                    is Result.Error -> Log.e(
                                        TAG,
                                        it1.exception.localizedMessage.orEmpty()
                                    )
                                    is Result.Success -> {
                                        // yes, we have already sent invite to this user
                                        val invite = it1.data
                                        binding.projectMiniInviteBtn.text =
                                            view.context.getString(R.string.revoke)
                                        binding.projectMiniInviteBtn.icon = null

                                        // setting the button to revoke this invite on click
                                        binding.projectMiniInviteBtn.setOnClickListener {
                                            projectMiniItemClickListener.onRevokeInviteClick(
                                                invite
                                            ) {
                                                binding.inviteBtnProgress.hide()
                                                binding.projectMiniInviteBtn.show()
                                            }
                                            bind(project)
                                        }
                                    }
                                    null -> {
                                        // no existing invite found, this is the only condition when we can send invite
                                        binding.projectMiniInviteBtn.text =
                                            view.context.getString(R.string.invite)
                                        binding.projectMiniInviteBtn.icon =
                                            ContextCompat.getDrawable(
                                                view.context,
                                                R.drawable.ic_round_how_to_reg_24
                                            )

                                        // setting the button to invite this user on click
                                        binding.projectMiniInviteBtn.setOnClickListener {
                                            projectMiniItemClickListener.onInviteClick(
                                                project,
                                                receiverId
                                            ) {
                                                binding.inviteBtnProgress.hide()
                                                binding.projectMiniInviteBtn.show()
                                            }
                                            bind(project)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectListItemViewHolder {
        return ProjectListItemViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.project_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProjectListItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private const val TAG = "ProjectListAdapter"
    }

}