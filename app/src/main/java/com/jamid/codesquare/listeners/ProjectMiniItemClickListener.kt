package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectInvite

interface ProjectMiniItemClickListener {
    fun onInviteClick(project: Project, receiverId: String, onFailure: () -> Unit)
    fun onRevokeInviteClick(invite: ProjectInvite, onFailure: () -> Unit)
}