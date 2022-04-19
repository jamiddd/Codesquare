package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@IgnoreExtraProperties
@Entity(tableName = "projects")
@Parcelize
@Serializable
@Keep
data class Project(
    @SerializedName("id")
    @PrimaryKey(autoGenerate = false)
    var id: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("content")
    var content: String,
    @SerializedName("commentChannel")
    var commentChannel: String,
    @SerializedName("chatChannel")
    var chatChannel: String,
    @Embedded(prefix = "project_")
    @SerializedName("creator")
    var creator: UserMinimal,
    @SerializedName("likes")
    var likes: Long,
    @SerializedName("comments")
    var comments: Long,
    @SerializedName("images")
    var images: List<String>,
    @SerializedName("tags")
    var tags: List<String>,
    @SerializedName("sources")
    var sources: List<String>,
    @SerializedName("contributors")
    var contributors: List<String>,
    @SerializedName("requests")
    var requests: List<String>,
    @Embedded(prefix = "project_")
    @SerializedName("location")
    var location: Location,
    @SerializedName("createdAt")
    var createdAt: Long = 0,
    @SerializedName("updatedAt")
    var updatedAt: Long = 0,
    @SerializedName("expiredAt")
    var expiredAt: Long = -1,
    @SerializedName("viewsCount")
    var viewsCount: Long = 0,
    @SerializedName("blockedList")
    var blockedList: List<String> = emptyList(),
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isLiked")
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isSaved")
    var isSaved: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isCollaboration")
    var isCollaboration: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isMadeByMe")
    var isMadeByMe: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isRequested")
    var isRequested: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isBlocked")
    var isBlocked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isNearMe")
    var isNearMe: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isArchived")
    var isArchived: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isAd")
    var isAd: Boolean = false,
): Parcelable {
    constructor(): this("", "", "", "", "", UserMinimal(), 0, 0, emptyList(),  emptyList(), emptyList(), emptyList(), emptyList(), Location(), 0, 0, -1, 0, emptyList(), false, false, false, false, false, false, false)

    @Exclude
    fun minify(): ProjectMinimal {
        return ProjectMinimal(id, name, images.first(), creator.userId, chatChannel, commentChannel)
    }

    companion object {

        fun newInstance(currentUser: User): Project {
            val newProject = Project()
            newProject.id = randomId()
            newProject.creator = UserMinimal(currentUser.id, currentUser.name, currentUser.photo, currentUser.username, currentUser.premiumState)
            val now = System.currentTimeMillis()
            newProject.createdAt = now
            newProject.updatedAt = now
            newProject.isMadeByMe = true
            newProject.contributors = listOf(currentUser.id)
            return newProject
        }

    }

}
