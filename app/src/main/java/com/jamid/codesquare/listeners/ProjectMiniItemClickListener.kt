package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectInvite

interface ProjectMiniItemClickListener {
    fun onInviteClick(project: Project, receiverId: String)
    fun onRevokeInviteClick(invite: ProjectInvite, receiverId: String)
}