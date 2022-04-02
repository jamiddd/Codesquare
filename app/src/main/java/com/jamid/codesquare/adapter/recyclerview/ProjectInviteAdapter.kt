package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.ProjectInviteComparator
import com.jamid.codesquare.adapter.recyclerview.ProjectInviteAdapter.ProjectInviteViewHolder
import com.jamid.codesquare.data.ProjectInvite
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.ProjectInviteListener

class ProjectInviteAdapter : PagingDataAdapter<ProjectInvite, ProjectInviteViewHolder>(ProjectInviteComparator()) {

    inner class ProjectInviteViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: RequestItemBinding
        private val projectInviteListener = view.context as ProjectInviteListener

        fun bind(projectInvite: ProjectInvite?) {
            if (projectInvite == null)
                return

            binding = RequestItemBinding.bind(view)
            binding.requestProgress.show()
            binding.requestPrimaryAction.disappear()
            binding.requestSecondaryAction.disappear()

            binding.requestTime.text = getTextForTime(projectInvite.createdAt)

            binding.requestProgress.hide()
            binding.requestPrimaryAction.show()
            binding.requestSecondaryAction.show()

            binding.requestPrimaryAction.setOnClickListener {
                binding.requestProgress.show()
                binding.requestPrimaryAction.disappear()
                projectInviteListener.onProjectInviteAccept(projectInvite)
            }

            binding.requestSecondaryAction.setOnClickListener {
                projectInviteListener.onProjectInviteCancel(projectInvite)
            }

            projectInviteListener.onCheckForStaleData(projectInvite)

        }

    }

    override fun onBindViewHolder(holder: ProjectInviteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectInviteViewHolder {
        return ProjectInviteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false))
    }

    companion object {
        private const val TAG = "ProjectInviteAdapter"
    }

}