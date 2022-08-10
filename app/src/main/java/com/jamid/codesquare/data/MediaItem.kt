package com.jamid.codesquare.data

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.Keep
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize// something simple
@Keep
data class MediaItem(
    var url: String,
    var name: String = randomId(),
    var type: String = "",
    var mimeType: String = "",
    var sizeInBytes: Long = 0,
    var ext: String = "",
    var path: String = "",
    var thumbnail: Bitmap? = null,
    var dateCreated: Long = System.currentTimeMillis(),
    var dateModified: Long = System.currentTimeMillis()
): Parcelable {
    constructor(): this(randomId())
}