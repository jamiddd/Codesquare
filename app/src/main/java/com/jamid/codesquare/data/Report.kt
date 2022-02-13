package com.jamid.codesquare.data

import android.os.Parcelable
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Report(
    var id: String,
    var senderId: String,
    var contextId: String,
    var snapshots: List<String>,
    var reason: String,
    var createdAt: Long
): Parcelable {
    constructor(): this(randomId(), "", "", emptyList(), "", System.currentTimeMillis())
}