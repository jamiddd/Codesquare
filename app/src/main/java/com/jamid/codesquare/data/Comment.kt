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
    var projectId: String,
    var commentChannelId: String,
    var threadChannelId: String,
    var likesCount: Long,
    var repliesCount: Long,
    var commentLevel: Long,
    var createdAt: Long,
    var updatedAt: Long,
    var likes: List<String>,
    @Exclude @set: Exclude @get: Exclude
    var isLiked: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var postTitle: String,
): Parcelable {
    constructor(): this("", "", "", UserMinimal(), "", "", "", "", 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis(), emptyList(), false, "")
}