package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Project

interface ProjectClickListener {

    fun onProjectClick(project: Project)
    fun onProjectLikeClick(project: Project)
    fun onProjectSaveClick(project: Project)
    fun onProjectJoinClick(project: Project)

}