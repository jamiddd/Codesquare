package com.jamid.codesquare.data

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Parcelize
@Serializable
data class UserMinimal(
    var userId: String,
    var name: String,
    var photo: String?,
    var username: String
): Parcelable {
    constructor() : this("", "", "", "")

    @Exclude
    fun isEmpty() = userId.isBlank()
}