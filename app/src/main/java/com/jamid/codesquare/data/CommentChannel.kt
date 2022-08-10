package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "comment_channels")
@Keep
data class CommentChannel(
    @PrimaryKey
    var commentChannelId: String,
    var parentId: String,
    var postId: String,
    var commentsCount: Long,
    var createdAt: Long,
    @Embedded(prefix = "comment_channel_")
    var lastComment: Comment?,
    var archived: Boolean = false,
    var updatedAt: Long = System.currentTimeMillis()
): Parcelable {
    constructor(): this("", "", "", 0, System.currentTimeMillis(), Comment())
}