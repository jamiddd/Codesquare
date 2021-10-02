package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName = "projects")
@Parcelize
data class Project(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var title: String,
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
    val sources: List<String>,
    var contributors: List<String>,
    var requests: List<String>,
    @Embedded(prefix = "project_")
    var location: Location,
    var createdAt: Long,
    var updatedAt: Long,
    @Exclude @set: Exclude @get: Exclude
    var isLiked: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isSaved: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isCollaboration: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isMadeByMe: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isRequested: Boolean,
): Parcelable {
    constructor(): this("", "", "", "", "", UserMinimal(), 0, 0, emptyList(),  emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), Location(), 0, 0, false, false, false, false, false)

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
