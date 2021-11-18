package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName = "users")
@Parcelize
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    var name: String,
    var username: String,
    var tag: String,
    var email: String,
    var about: String,
    var photo: String?,
    var interests: List<String>,
    var likedUsers: List<String>,
    var likedProjects: List<String>,
    var likedComments: List<String>,
    var savedProjects: List<String>,
    var collaborations: List<String>,
    var projects: List<String>,
    var projectRequests: List<String>,
    var chatChannels: List<String>,
    var registrationTokens: List<String>,
    var projectsCount: Long,
    var collaborationsCount: Long,
    var likesCount: Long,
    var createdAt: Long,
    var updatedAt: Long,
    @Exclude @set: Exclude @get: Exclude
    var isFollowed: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isFollowing: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isCurrentUser: Boolean
): Parcelable {
    constructor(): this("", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, 0, 0, 0, false, false, false)

    @Exclude
    fun minify(): UserMinimal {
        return UserMinimal(id, name, photo, username)
    }

    @Exclude
    fun isEmpty() = id.isBlank()

    companion object {
        fun newUser(id: String, name: String, email: String) =
            User(id, name, randomId().take(16), "", email, "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), isFollowed = false, isFollowing = false, isCurrentUser = true)

    }

}
