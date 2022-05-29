package com.jamid.codesquare.ui.create

import com.jamid.codesquare.data.Location

data class CreatePostForm(
    var isValid: Boolean = false,
    var title: String = "",
    var content: String = "",
    var images: List<String> = emptyList(),
    var tags: List<String> = emptyList(),
    var links: List<String> = emptyList(),
    var location: Location? = null
)