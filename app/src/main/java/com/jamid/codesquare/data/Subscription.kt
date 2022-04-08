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
data class Subscription(
    var id: String,
    var price: Long,
    var priceText: String,
    var currency: String,
    var period: String,
    var isActive: Boolean,
    var description: String,
    var createdAt: Long,
    @Exclude @set: Exclude @get: Exclude
    var isSelected: Boolean
): Parcelable {
    constructor(): this(randomId(), 0, "", "", "", false, "", System.currentTimeMillis(), false)
}