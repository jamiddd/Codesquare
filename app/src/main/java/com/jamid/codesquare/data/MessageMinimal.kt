package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageMinimal(
    val senderId: String,
    val name: String,
    val content: String,
    val type: String,
    val chatChannelId: String,
    val isDownloaded: Boolean,
    @Embedded(prefix = "metadata_")
    val metadata: Metadata?
): Parcelable {
    constructor(): this("", "", "", "", "", false, null)
}