package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@IgnoreExtraProperties
@Entity(tableName = "projects")
@Parcelize
@Serializable
data class Project(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var name: String,
    var content: String,
    var commentChannel: String,
    var chatChannel: String,
    @Embedded(prefix = "project_")
    var creator: UserMinimal,
    var likes: Long,
    var comments: Long,
    var rules: List<String>,
    var images: List<String>,
    var tags: List<String>,
    var sources: List<String>,
    var contributors: List<String>,
    var requests: List<String>,
    @Embedded(prefix = "project_")
    var location: Location,
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var expiredAt: Long = -1,
    var blockedList: List<String> = emptyList(),
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isSaved: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isCollaboration: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isMadeByMe: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isRequested: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isBlocked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isNearMe: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isArchived: Boolean = false,
): Parcelable {
    constructor(): this("", "", "", "", "", UserMinimal(), 0, 0, emptyList(),  emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), Location(), 0, 0, -1, emptyList(), false, false, false, false, false, false, false)

    companion object {

        fun newInstance(currentUser: User): Project {
            val newProject = Project()
            newProject.id = randomId()
            newProject.creator = UserMinimal(currentUser.id, currentUser.name, currentUser.photo, currentUser.username)
            val now = System.currentTimeMillis()
            newProject.createdAt = now
            newProject.updatedAt = now
            newProject.isMadeByMe = true
            newProject.contributors = listOf(currentUser.id)
            return newProject
        }

    }

}
