package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ProjectRequest

interface ProjectRequestListener {
    fun onProjectRequestAccept(projectRequest: ProjectRequest)
    fun onProjectRequestCancel(projectRequest: ProjectRequest)
    fun onProjectRequestProjectDeleted(projectRequest: ProjectRequest)
    fun onProjectRequestSenderDeleted(projectRequest: ProjectRequest)
    fun updateProjectRequest(newProjectRequest: ProjectRequest)
}