package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageMinimal(
    val senderId: String,
    var name: String,
    var content: String,
    var type: String,
    var messageId: String,
    var chatChannelId: String,
    var isDownloaded: Boolean,
    @Embedded(prefix = "metadata_")
    var metadata: Metadata?
): Parcelable {
    constructor(): this("", "", "", "", "", "", false, null)
}