package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
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