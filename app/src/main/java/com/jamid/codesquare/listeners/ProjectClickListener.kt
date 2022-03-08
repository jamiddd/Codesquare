package com.jamid.codesquare.listeners

import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectRequest

interface ProjectClickListener {
    fun onProjectClick(project: Project)
    fun onProjectLikeClick(project: Project)
    fun onProjectSaveClick(project: Project)
    fun onProjectJoinClick(project: Project)
    fun onProjectCreatorClick(project: Project)
    fun onProjectCommentClick(project: Project)
    fun onProjectOptionClick(project: Project, position: Int)
    fun onProjectUndoClick(project: Project, projectRequest: ProjectRequest)
    fun onProjectContributorsClick(project: Project)
    fun onProjectSupportersClick(project: Project)
    fun onProjectNotFound(project: Project)
    fun onProjectLoad(project: Project)
    fun onAdInfoClick()
    fun onAdError(project: Project)
}