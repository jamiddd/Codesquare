package com.jamid.codesquare.data

data class ProjectInvite(val id: String, val projectId: String, val receiver: String, val sender: String, val createdAt: Long) {
    constructor(): this("", "", "", "", System.currentTimeMillis())
}