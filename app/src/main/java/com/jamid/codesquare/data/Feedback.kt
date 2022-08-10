package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Feedback(// something simple
    var id: String,
    var content: String,
    var createdAt: Long,
    var senderId: String
): Parcelable {
    constructor(): this(randomId(), "", System.currentTimeMillis(), "")
}
