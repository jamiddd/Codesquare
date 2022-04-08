package com.jamid.codesquare.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class ProjectMinimal2(
    @SerializedName("objectID")
    var objectID: String,
    @SerializedName("type")
    var type: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("content")
    var content: String,
    @SerializedName("createdAt")
    var createdAt: Long,
    @SerializedName("creator")
    var creator: UserMinimal,
    @SerializedName("images")
    var images: List<String>,
    @SerializedName("location")
    var location: Location,
    @SerializedName("tags")
    var tags: List<String>,
    @SerializedName("updatedAt")
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