package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ProjectInvite

interface ProjectInviteListener {
    fun onProjectInviteAccept(projectInvite: ProjectInvite, onFailure: () -> Unit)
    fun onProjectInviteCancel(projectInvite: ProjectInvite)
    fun onProjectInviteProjectDeleted(projectInvite: ProjectInvite)
    fun onCheckForStaleData(projectInvite: ProjectInvite)
}