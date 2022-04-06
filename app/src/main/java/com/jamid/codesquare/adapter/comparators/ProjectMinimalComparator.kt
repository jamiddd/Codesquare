package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.ProjectMinimal2

class ProjectMinimalComparator: DiffUtil.ItemCallback<ProjectMinimal2>() {
    override fun areItemsTheSame(oldItem: ProjectMinimal2, newItem: ProjectMinimal2)
            = oldItem.objectID == newItem.objectID

    override fun areContentsTheSame(oldItem: ProjectMinimal2, newItem: ProjectMinimal2)
            = oldItem == newItem
}