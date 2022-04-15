package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ProjectRequest

interface ProjectRequestListener {
    fun onProjectRequestAccept(projectRequest: ProjectRequest, onFailure: () -> Unit)
    fun onProjectRequestCancel(projectRequest: ProjectRequest)
    fun onProjectRequestProjectDeleted(projectRequest: ProjectRequest)
    fun onProjectRequestSenderDeleted(projectRequest: ProjectRequest)
    fun updateProjectRequest(newProjectRequest: ProjectRequest)
    fun onProjectRequestUndo(projectRequest: ProjectRequest)
    fun onProjectRequestClick(projectRequest: ProjectRequest)
    fun onCheckForStaleData(projectRequest: ProjectRequest)
}