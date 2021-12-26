package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Entity(tableName = "project_requests")
@Parcelize
data class ProjectRequest(
    @PrimaryKey(autoGenerate = false)
    var requestId: String,
    var projectId: String,
    var senderId: String,
    var receiverId: String,
    @Embedded(prefix = "request_project_")
    @Exclude @set: Exclude @get: Exclude
    var project: Project?,
    @Embedded(prefix = "request_user_")
    @Exclude @set: Exclude @get: Exclude
    var sender: User?,
    var createdAt: Long,
    var notificationId: String? = null
): Parcelable {
    constructor(): this("", "", "", "", Project(), User(), System.currentTimeMillis())
}