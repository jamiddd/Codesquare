package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.ProjectComparator
import com.jamid.codesquare.data.Project

class ProjectAdapter: PagingDataAdapter<Project, PostViewHolder>(ProjectComparator()) {

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return if (viewType == 0) {
            ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
        } else {
            AdViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.custom_post_ad, parent, false))
        }

    }

    override fun getItemViewType(position: Int): Int {
        val project = getItem(position)
        return if (project != null) {
            if (!project.isAd) {
                0
            } else {
                1
            }
        } else {
            super.getItemViewType(position)
        }
    }
}