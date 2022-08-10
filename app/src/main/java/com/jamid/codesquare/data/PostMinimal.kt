package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep// something simple
data class PostMinimal(
    val id: String,
    val name: String,
    val image: String,
    val creatorId: String,
    val chatChannel: String,
    val commentChannel: String
): Parcelable {
    constructor(): this("", "", "", "", "", "")
}