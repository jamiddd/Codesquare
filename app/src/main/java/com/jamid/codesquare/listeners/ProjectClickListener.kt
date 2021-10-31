package com.jamid.codesquare.listeners

import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project

interface ProjectClickListener {

    fun onProjectClick(project: Project)
    fun onProjectLikeClick(project: Project)
    fun onProjectSaveClick(project: Project)
    fun onProjectJoinClick(project: Project)
    fun onProjectCreatorClick(project: Project)
    fun onProjectCommentClick(project: Project)
    fun onProjectOptionClick(viewHolder: ProjectViewHolder, project: Project)

}