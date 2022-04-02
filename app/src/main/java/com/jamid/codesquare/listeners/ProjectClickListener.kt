package com.jamid.codesquare.listeners

import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectRequest

interface ProjectClickListener {
    fun onProjectClick(project: Project)
    fun onProjectLikeClick(project: Project, onChange: (newProject: Project) -> Unit)
    fun onProjectSaveClick(project: Project, onChange: (newProject: Project) -> Unit)
    fun onProjectJoinClick(project: Project, onChange: (newProject: Project) -> Unit)
    fun onProjectCreatorClick(project: Project)
    fun onProjectCommentClick(project: Project)
    fun onProjectOptionClick(project: Project)
    fun onProjectUndoClick(project: Project, onChange: (newProject: Project) -> Unit)
    fun onProjectContributorsClick(project: Project)
    fun onProjectSupportersClick(project: Project)
    fun onProjectNotFound(project: Project)
    fun onProjectLoad(project: Project)
    fun onAdInfoClick()
    fun onAdError(project: Project)
    fun onProjectLocationClick(project: Project)
    fun onCheckForStaleData(project: Project)
}