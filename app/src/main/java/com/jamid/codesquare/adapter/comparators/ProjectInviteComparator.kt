package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.ProjectInvite

class ProjectInviteComparator: DiffUtil.ItemCallback<ProjectInvite>() {
    override fun areItemsTheSame(oldItem: ProjectInvite, newItem: ProjectInvite): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ProjectInvite, newItem: ProjectInvite): Boolean {
        return oldItem == newItem
    }
}