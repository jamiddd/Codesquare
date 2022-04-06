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

/*const userMinimal = {
            objectID: context.params.userId,
            email: userDocument.get("email"),
            about: userDocument.get("about"),
            createdAt: userDocument.get("createdAt"),
            interests: userDocument.get("interests"),
            location: userDocument.get("location"),
            name: userDocument.get("name"),
            photo: userDocument.get("photo"),
            premiumState: userDocument.get("premiumState"),
            tag: userDocument.get("tag"),
            username: userDocument.get("username"),
            type: "user"
        };*/