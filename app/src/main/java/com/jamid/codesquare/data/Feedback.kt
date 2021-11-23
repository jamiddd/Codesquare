package com.jamid.codesquare.data

import android.os.Parcelable
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Feedback(
    var id: String,
    var content: String,
    var createdAt: Long,
    var senderId: String
): Parcelable {
    constructor(): this(randomId(), "", System.currentTimeMillis(), "")
}
