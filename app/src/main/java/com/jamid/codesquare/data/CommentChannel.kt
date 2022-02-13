package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "comment_channels")
data class CommentChannel(
    @PrimaryKey
    var commentChannelId: String,
    var parentId: String,
    var projectId: String,
    var postTitle: String,
    var createdAt: Long,
    @Embedded(prefix = "comment_channel_")
    val lastComment: Comment?,
    var archived: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "", System.currentTimeMillis(), Comment())
}