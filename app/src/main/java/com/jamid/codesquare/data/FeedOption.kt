package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep
data class FeedOption(
    var filter: String?,
    var sort: FeedSort,
    var order: FeedOrder
)// something simple

enum class FeedSort {
    CONTRIBUTORS, LIKES, MOST_VIEWED, MOST_RECENT, LOCATION
}

enum class FeedOrder {
    ASC, DESC
}