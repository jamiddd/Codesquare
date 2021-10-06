package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "chat_channels")
data class ChatChannel(
    @PrimaryKey(autoGenerate = false)
    var chatChannelId: String,
    var projectId: String,
    var projectTitle: String,
    var projectImage: String?,
    // need to set a listener to know if a new user has been added to this group TODO()
    var contributorsCount: Long,
    var administrators: List<String>,
    var contributors: List<String>,
    var createdAt: Long,
    var updatedAt: Long,
    @Embedded(prefix = "message_")
    var lastMessage: Message?
): Parcelable {
    constructor(): this("", "", "", "", 0, emptyList(), emptyList(), 0, 0, Message())
}