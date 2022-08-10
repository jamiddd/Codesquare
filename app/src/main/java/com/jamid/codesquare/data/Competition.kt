package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Competition(// something simple
    var id: String,
    var name: String,
    var type: String,
    var currentStatus: String,
    var rankCounter: Long,
    var usersCount: Long,
    var poolValue: Double,
    var rootPercent: Double,
): Parcelable {
    constructor(): this("", "", "", "", 0, 0, 0.0, 0.0)
}
