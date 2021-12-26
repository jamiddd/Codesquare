package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.ProjectInvite

interface ProjectInviteListener {
    fun updateProjectInvite(newProjectInvite: ProjectInvite)
    fun onProjectInviteAccept(projectInvite: ProjectInvite)
    fun onProjectInviteCancel(projectInvite: ProjectInvite)
    fun onProjectInviteProjectDeleted(projectInvite: ProjectInvite)
}