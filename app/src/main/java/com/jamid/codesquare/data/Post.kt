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
@Entity(tableName = "posts")
@Parcelize
@Serializable
@Keep
data class Post(
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
    @Embedded(prefix = "post_")
    @SerializedName("creator")
    var creator: UserMinimal = UserMinimal(),
    @SerializedName("likesCount")
    var likesCount: Long = 0,
    @SerializedName("commentsCount")
    var commentsCount: Long = 0,
    @SerializedName("contributorsCount")
    var contributorsCount: Long = 0,
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
    @SerializedName("images")
    var images: List<String> = emptyList(),
    @SerializedName("tags")
    var tags: List<String> = emptyList(),
    @SerializedName("sources")
    var sources: List<String> = emptyList(),
    @SerializedName("contributors")
    var contributors: List<String> = emptyList(),
    @SerializedName("requests")
    var requests: List<String> = emptyList(),
    @Embedded(prefix = "post_")
    @SerializedName("location")
    var location: Location = Location(),
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
    @Transient
    @SerializedName("archived")
    var archived: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    @Transient
    @SerializedName("isAd")
    var isAd: Boolean = false,
): Parcelable {
    constructor(): this("", "", "", "", "")

    @Exclude
    fun minify(): PostMinimal {
        return PostMinimal(id, name, images.first(), creator.userId, chatChannel, commentChannel)
    }

    companion object {

        fun newInstance(currentUser: User): Post {
            val newPost = Post()
            newPost.id = randomId()
            newPost.creator = UserMinimal(currentUser.id, currentUser.name, currentUser.photo, currentUser.username, currentUser.premiumState)
            val now = System.currentTimeMillis()
            newPost.createdAt = now
            newPost.updatedAt = now
            newPost.isMadeByMe = true
            newPost.contributors = listOf(currentUser.id)
            return newPost
        }

    }

}
