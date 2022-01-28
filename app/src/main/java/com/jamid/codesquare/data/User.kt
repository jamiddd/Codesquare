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
@Entity(tableName = "users")
@Parcelize
@Serializable
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    var name: String,
    var username: String,
    var tag: String,
    var email: String,
    var about: String,
    var photo: String,
    var interests: List<String> = emptyList(),
    var likedUsers: List<String> = emptyList(),
    var likedProjects: List<String> = emptyList(),
    var likedComments: List<String> = emptyList(),
    var savedProjects: List<String> = emptyList(),
    var collaborations: List<String> = emptyList(),
    var projects: List<String> = emptyList(),
    var projectRequests: List<String> = emptyList(),
    var projectInvites: List<String> = emptyList(),
    var chatChannels: List<String> = emptyList(),
    var registrationTokens: List<String> = emptyList(),
    var projectsCount: Long = 0,
    var collaborationsCount: Long = 0,
    var likesCount: Long = 0,
    var createdAt: Long = -1,
    var updatedAt: Long = -1,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    var isCurrentUser: Boolean = false,
    @Embedded(prefix = "user_")
    var location: Location? = null,
    var premiumState: Long = -1
): Parcelable {
    constructor(): this("", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, 0, 0, 0, false, false)

    @Exclude
    fun minify(): UserMinimal {
        return UserMinimal(id, name, photo, username)
    }

    @Exclude
    fun isEmpty() = id.isBlank()

    companion object {
        fun newUser(id: String, name: String, email: String) =
            User(id, name, randomId().take(16), "", email, "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), isLiked = false, isCurrentUser = true)

    }

}
