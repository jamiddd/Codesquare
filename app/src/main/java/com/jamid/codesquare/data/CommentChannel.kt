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
    val commentChannelId: String,
    val parentId: String,
    val postTitle: String,
    val createdAt: Long,
    @Embedded(prefix = "comment_channel_")
    val lastComment: Comment?
): Parcelable {
    constructor(): this("", "", "", System.currentTimeMillis(), Comment())
}