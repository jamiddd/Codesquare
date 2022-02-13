package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Metadata(
    var size: Long, // in bytes
    var name: String,
    var url: String,
    var ext: String
): Parcelable {
    constructor(): this(0, "", "", "")
}