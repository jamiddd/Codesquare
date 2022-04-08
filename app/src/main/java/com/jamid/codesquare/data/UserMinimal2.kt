package com.jamid.codesquare.data

import kotlinx.serialization.Serializable

@Serializable
data class UserMinimal2(
    var objectID: String,
    var email: String,
    var about: String,
    var createdAt: Long,
    var interests: List<String>,
    var location: Location,
    var name: String,
    var photo: String,
    var premiumState: Long,
    var tag: String,
    var username: String,
    var type: String
) {
    constructor(): this("", "", "", 0, emptyList(), Location(), "", "", -1, "", "", "")
}