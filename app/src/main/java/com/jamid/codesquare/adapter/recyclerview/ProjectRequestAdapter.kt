package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.ProjectRequest

class ProjectRequestAdapter : PagingDataAdapter<ProjectRequest, ProjectRequestViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<ProjectRequest>() {
            override fun areItemsTheSame(
                oldItem: ProjectRequest,
                newItem: ProjectRequest
            ): Boolean {
                return oldItem.requestId == newItem.requestId
            }

            override fun areContentsTheSame(
                oldItem: ProjectRequest,
                newItem: ProjectRequest
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onBindViewHolder(holder: ProjectRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectRequestViewHolder {
        return ProjectRequestViewHolder.newInstance(parent)
    }

}