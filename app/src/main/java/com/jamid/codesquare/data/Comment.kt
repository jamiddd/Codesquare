package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName = "comments")
@Parcelize
@Keep
data class Comment(
    @PrimaryKey(autoGenerate = false)
    val commentId: String,
    var content: String,
    var senderId: String,
    @Embedded(prefix = "comment_")
    var sender: UserMinimal,
    var parentId: String,
    var postId: String,
    var commentChannelId: String,
    var threadChannelId: String,
    var parentCommentChannelId: String? = null,
    var likesCount: Long = 0,
    var repliesCount: Long = 0,
    var commentLevel: Long = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    @Exclude @set: Exclude @get: Exclude
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var postTitle: String = "",
): Parcelable {
    constructor(): this("", "", "", UserMinimal(), "", "", "", "")
}