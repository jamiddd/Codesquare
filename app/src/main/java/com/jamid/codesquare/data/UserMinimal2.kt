package com.jamid.codesquare.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
// something simple
@Serializable
@Keep
data class UserMinimal2(
    @SerializedName("objectID")
    var objectID: String,
    @SerializedName("email")
    var email: String,
    @SerializedName("about")
    var about: String,
    @SerializedName("createdAt")
    var createdAt: Long,
    @SerializedName("interests")
    var interests: List<String>,
    @SerializedName("location")
    var location: Location,
    @SerializedName("name")
    var name: String,
    @SerializedName("photo")
    var photo: String,
    @SerializedName("premiumState")
    var premiumState: Long,
    @SerializedName("tag")
    var tag: String,
    @SerializedName("username")
    var username: String,
    @SerializedName("type")
    var type: String
) {
    constructor(): this("", "", "", 0, emptyList(), Location(), "", "", -1, "", "", "")
}