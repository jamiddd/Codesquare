package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Keep
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val geoHash: String
): Parcelable {
    constructor(): this(0.0, 0.0, "", "")
}