package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ProjectRequest

interface ProjectRequestListener {

    fun onProjectRequestAccept(projectRequest: ProjectRequest)
    fun onProjectRequestCancel(projectRequest: ProjectRequest)

}