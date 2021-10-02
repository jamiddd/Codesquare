package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String
): Parcelable {
    constructor(): this(0.0, 0.0, "")
}