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

/**
 * @param id The unique user id to represent this user
 * @param name A name of the user which is shown publicly
 * @param username A unique name tag that can also be used to represent this user but for public use
 * @param tag A title to represent the work of the user
 * @param email Email id of the user
 * @param about A small introduction of the user
 * @param photo An image of the user which is public
 * @param interests A list of all the things that the user is interested in
 * @param likedUsers A list of all the users that the current user likes
 * @param likedProjects A list of all the projects liked by the current user
 * @param likedComments A list of all the comments liked by the current user
 * @param savedProjects A list of all the projects saved by the current user
 * @param archivedProjects A list of all the projects archived by the current user
 * @param collaborations A list of all the projects the current user has collaborated
 * @param projects A list of all the projects created by the current user
 * @param projectRequests A list of all the projects requested by the current user
 * @param projectInvites A list of all the invites created by the current user
 * @param chatChannels A list of all the chat channels the current user is associated with
 * @param token A token that is represented by unique id by firebase
 * @param projectsCount A counter for the projects created by the current user
 * @param collaborationsCount A counter for the collaborations by the current user
 * @param likesCount A counter for all the likes received by the current user by other users
 * @param createdAt A date of creation of the current user in the database
 * @param updatedAt A date when the user data was last updated
 * @param isLiked A local variable to check if the other user is liked by the current user
 * @param isCurrentUser A local variable to check if the user is current user
 * @param location Location of the current user
 * @param premiumState A status flag for premium subscription by the current user
*
* */
@IgnoreExtraProperties
@Entity(tableName = "users")
@Parcelize
@Serializable
@Keep
data class User(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("id")
    var id: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("username")
    var username: String,
    @SerializedName("tag")
    var tag: String,
    @SerializedName("email")
    var email: String,
    @SerializedName("about")
    var about: String,
    @SerializedName("photo")
    var photo: String,
    @SerializedName("interests")
    var interests: List<String> = emptyList(),
    @SerializedName("likedUsers")
    var likedUsers: List<String> = emptyList(),
    @SerializedName("likedProjects")
    var likedProjects: List<String> = emptyList(),
    @SerializedName("likedComments")
    var likedComments: List<String> = emptyList(),
    @SerializedName("savedProjects")
    var savedProjects: List<String> = emptyList(),
    @SerializedName("archivedProjects")
    var archivedProjects: List<String> = emptyList(),
    @SerializedName("collaborations")
    var collaborations: List<String> = emptyList(),
    @SerializedName("projects")
    var projects: List<String> = emptyList(),
    @SerializedName("projectRequests")
    var projectRequests: List<String> = emptyList(),
    @SerializedName("projectInvites")
    var projectInvites: List<String> = emptyList(),
    @SerializedName("chatChannels")
    var chatChannels: List<String> = emptyList(),
    @SerializedName("token")
    var token: String = "",
    @SerializedName("projectsCount")
    var projectsCount: Long = 0,
    @SerializedName("collaborationsCount")
    var collaborationsCount: Long = 0,
    @SerializedName("likesCount")
    var likesCount: Long = 0,
    @SerializedName("createdAt")
    var createdAt: Long = -1,
    @SerializedName("updatedAt")
    var updatedAt: Long = -1,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isLiked")
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isCurrentUser")
    var isCurrentUser: Boolean = false,
    @Embedded(prefix = "user_")
    @SerializedName("location")
    var location: Location = Location(),
    @SerializedName("premiumState")
    var premiumState: Long = -1,
    @SerializedName("online")
    var online: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "", 0, 0, 0, 0, 0, false, false)

    @Exclude
    fun minify(): UserMinimal {
        return UserMinimal(id, name, photo, username, premiumState)
    }

    @Exclude
    fun isEmpty() = name.isBlank()

    companion object {
        fun newUser(id: String, name: String, email: String) =
            User(id, name, randomId().take(16), "", email, "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "", 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), isLiked = false, isCurrentUser = true)

    }

}
