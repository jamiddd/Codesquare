package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaMetadata(
    var size_b: Long,
    var originalFileName: String,
    var extension: String
): Parcelable {
    constructor(): this(0, "", "")
}