package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Project

class ProjectComparator: DiffUtil.ItemCallback<Project>() {
    override fun areItemsTheSame(oldItem: Project, newItem: Project)
        = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Project, newItem: Project)
        = oldItem == newItem
}