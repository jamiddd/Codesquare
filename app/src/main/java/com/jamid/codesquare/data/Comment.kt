package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName = "comments")
@Parcelize
data class Comment(
    @PrimaryKey(autoGenerate = false)
    val commentId: String,
    var content: String,
    var senderId: String,
    var parentId: String,
    var commentChannelId: String,
    var threadChannelId: String,
    var likes: Long,
    var repliesCount: Long,
    var commentLevel: Long,
    var postedAt: Long,
    @Embedded(prefix = "comment_")
    @Exclude @set: Exclude @get: Exclude
    var sender: UserMinimal,
    @Exclude @set: Exclude @get: Exclude
    var isLiked: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var postTitle: String,
): Parcelable {
    constructor(): this("", "", "", "", "", "", 0, 0, 0, 0, UserMinimal(), false, "")
}