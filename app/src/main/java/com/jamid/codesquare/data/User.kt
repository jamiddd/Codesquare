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
    var archivedProjects: List<String> = emptyList(),
    var collaborations: List<String> = emptyList(),
    var projects: List<String> = emptyList(),
    var projectRequests: List<String> = emptyList(),
    var projectInvites: List<String> = emptyList(),
    var chatChannels: List<String> = emptyList(),
    var token: String = "",
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
    constructor(): this("", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "", 0, 0, 0, 0, 0, false, false)

    @Exclude
    fun minify(): UserMinimal {
        return UserMinimal(id, name, photo, username)
    }

    @Exclude
    fun isEmpty() = name.isBlank()

    companion object {
        fun newUser(id: String, name: String, email: String) =
            User(id, name, randomId().take(16), "", email, "", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "", 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), isLiked = false, isCurrentUser = true)

    }

}
