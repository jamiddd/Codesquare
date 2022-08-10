package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
// something simple
@Parcelize
@IgnoreExtraProperties
@Keep
data class Metadata(
    var size: Long, // in bytes
    var name: String,
    var url: String,
    var ext: String,
    @Exclude @set: Exclude @get: Exclude
    var height: Long, // in px TODO("Make it int for next uninstallation")
    @Exclude @set: Exclude @get: Exclude
    var width: Long, // in px TODO("Make it int for next uninstallation")
    @Exclude @set: Exclude @get: Exclude
    var thumbnail: String? = null
): Parcelable {
    constructor(): this(0, "", "", "", 0, 0)
}