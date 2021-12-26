package com.jamid.codesquare.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "project_invites")
data class ProjectInvite(
    @PrimaryKey
    var id: String,
    var projectId: String,
    var receiverId: String,
    var senderId: String,
    var createdAt: Long,
    @Embedded(prefix = "project_invite_sender_")
    @Exclude @set: Exclude @get: Exclude
    var sender: User? = null,
    @Embedded(prefix = "project_invite_project_")
    @Exclude @set: Exclude @get: Exclude
    var project: Project? = null,
    var notificationId: String? = null
) {
    constructor(): this("", "", "", "", System.currentTimeMillis())
}