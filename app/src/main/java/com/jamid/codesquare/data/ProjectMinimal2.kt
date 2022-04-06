package com.jamid.codesquare.data

import kotlinx.serialization.Serializable

@Serializable
data class ProjectMinimal2(
    var objectID: String,
    var type: String,
    var name: String,
    var content: String,
    var createdAt: Long,
    var creator: UserMinimal,
    var images: List<String>,
    var location: Location,
    var tags: List<String>,
    var updatedAt: Long
) {
    constructor(): this("", "", "", "", 0, UserMinimal(), emptyList(), Location(), emptyList(), 0)
}

/*
const projectMinimal = {
            objectID: context.params.projectId,
            type: "project",
            name: projectDocument.get("name"),
            content: projectDocument.get("content"),
            createdAt: projectDocument.get("createdAt"),
            creator: projectDocument.get("creator"),
            images: projectDocument.get("images"),
            location: projectDocument.get("location"),
            tags: projectDocument.get("tags"),
            updatedAt: projectDocument.get("updatedAt")
        };
*
* */