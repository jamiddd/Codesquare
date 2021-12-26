package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val geoHash: String
): Parcelable {
    constructor(): this(0.0, 0.0, "", "")
}