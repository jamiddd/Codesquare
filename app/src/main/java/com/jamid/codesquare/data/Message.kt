package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import java.io.File

@IgnoreExtraProperties
@Entity(tableName="messages")
@Parcelize
data class Message(
    @PrimaryKey
    var messageId: String,
    var chatChannelId: String,
    var type: String,
    var content: String,
    var senderId: String,
    @Embedded(prefix = "meta_")
    var metadata: Metadata?,
    var deliveryList: List<String>,
    var readList: List<String>,
    val createdAt: Long,
    @Embedded(prefix = "sender_")
    @Exclude @set: Exclude @get: Exclude
    var sender: User,
    @Exclude @set: Exclude @get: Exclude
    var isDownloaded: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isCurrentUserMessage: Boolean
): Parcelable {
    constructor(): this("", "", "", "", "", Metadata(), emptyList(), emptyList(), System.currentTimeMillis(), User(), false, false)
}
