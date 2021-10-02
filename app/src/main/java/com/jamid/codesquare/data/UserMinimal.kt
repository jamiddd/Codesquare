package com.jamid.codesquare.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserMinimal(
    var userId: String,
    var name: String,
    var photo: String?,
    var username: String
): Parcelable {
    constructor() : this("", "", "", "")
}