package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.ProjectMinimalComparator
import com.jamid.codesquare.data.ProjectMinimal2

class ProjectMinimalAdapter: ListAdapter<ProjectMinimal2, ProjectMinimalViewHolder>(ProjectMinimalComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectMinimalViewHolder {
        return ProjectMinimalViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_mini_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProjectMinimalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

