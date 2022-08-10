package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
@Keep
data class RankedRule(
    var id: String,
    var name: String,
    var content: String,
    @Exclude @set: Exclude @get: Exclude
    var isOpened: Boolean, // for ui purposes only
    var createdAt: Long
): Parcelable {
    constructor(): this(randomId(), randomId(), randomId(), false, System.currentTimeMillis())
}