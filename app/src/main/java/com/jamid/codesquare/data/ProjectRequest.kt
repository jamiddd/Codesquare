package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Entity(tableName = "project_requests")
@Parcelize
@Keep
data class ProjectRequest(
    @PrimaryKey(autoGenerate = false)
    var requestId: String,
    var projectId: String,
    var senderId: String,
    var receiverId: String,
    @Embedded(prefix = "request_project_")
    var project: ProjectMinimal,
    @Embedded(prefix = "request_user_")
    var sender: UserMinimal,
    var createdAt: Long,
    var updatedAt: Long,
    var notificationId: String
): Parcelable {
    constructor(): this("", "", "", "", ProjectMinimal(), UserMinimal(), System.currentTimeMillis(), System.currentTimeMillis(), "")
}