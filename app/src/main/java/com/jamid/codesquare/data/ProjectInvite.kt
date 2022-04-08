package com.jamid.codesquare.data

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "project_invites")
@Keep
data class ProjectInvite(
    @PrimaryKey
    var id: String,
    var projectId: String,
    var receiverId: String,
    var senderId: String,
    @Embedded(prefix = "project_invite_sender_")
    var sender: UserMinimal,
    @Embedded(prefix = "project_invite_project_")
    var project: ProjectMinimal,
    var createdAt: Long,
    var updatedAt: Long,
    var notificationId: String
) {
    constructor(): this("", "", "", "", UserMinimal(), ProjectMinimal(), System.currentTimeMillis(), System.currentTimeMillis(),"")
}