package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class ProjectMinimal(
    val id: String,
    val name: String,
    val image: String,
    val creatorId: String,
    val chatChannel: String,
    val commentChannel: String
): Parcelable {
    constructor(): this("", "", "", "", "", "")
}