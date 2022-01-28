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

class ProjectListAdapter(private val projectMiniItemClickListener: ProjectMiniItemClickListener): ListAdapter<Project, ProjectListAdapter.ProjectListItemViewHolder>(ProjectComparator()){

    var receiverIdForInvite: String = ""
    var currentUserId: String = ""

    inner class ProjectListItemViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        fun bind(project: Project) {
            val binding = ProjectListItemBinding.bind(view)
            binding.projectMiniImage.setImageURI(project.images.firstOrNull())
            binding.projectMiniName.text = project.name

            binding.inviteBtnProgress.show()
            binding.projectMiniInviteBtn.disappear()

            if (project.contributors.contains(receiverIdForInvite)) {
                // hide invite button
                binding.inviteBtnProgress.hide()
                binding.projectMiniInviteBtn.hide()
            } else {
                FireUtility.getOldProjectRequest(project.id, receiverIdForInvite) {
                    when (it) {
                        is Result.Error -> Log.e(TAG, it.exception.localizedMessage.orEmpty())
                        is Result.Success -> {
                            // since there is already a request by this user, somehow need to show the user that there is a request,
                            // or make accept reject button right here, maybe later
                            binding.inviteBtnProgress.hide()
                            binding.projectMiniInviteBtn.hide()
                        }
                        null -> {
                            // there is no project request
                            FireUtility.getOldInvite(project.id, receiverIdForInvite, currentUserId) { it1 ->

                                binding.inviteBtnProgress.hide()
                                binding.projectMiniInviteBtn.show()

                                when (it1) {
                                    is Result.Error -> Log.e(TAG, it1.exception.localizedMessage.orEmpty())
                                    is Result.Success -> {
                                        val invite = it1.data
                                        binding.projectMiniInviteBtn.text = view.context.getString(R.string.revoke)
                                        binding.projectMiniInviteBtn.icon = null
                                        binding.projectMiniInviteBtn.setOnClickListener {
                                            projectMiniItemClickListener.onRevokeInviteClick(invite, receiverIdForInvite)
                                            bind(project)
                                        }
                                    }
                                    null -> {
                                        binding.projectMiniInviteBtn.text = view.context.getString(R.string.invite)
                                        binding.projectMiniInviteBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_how_to_reg_24)
                                        binding.projectMiniInviteBtn.setOnClickListener {
                                            projectMiniItemClickListener.onInviteClick(project, receiverIdForInvite)
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
        return ProjectListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProjectListItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private const val TAG = "ProjectListAdapter"
    }

}