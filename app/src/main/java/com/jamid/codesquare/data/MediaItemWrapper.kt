package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep// something simple
data class MediaItemWrapper(
    val mediaItem: MediaItem,
    var isSelected: Boolean,
    var selectedCount: Int
)