package com.jamid.codesquare.data

data class FeedOption(
    var filter: String?,
    var sort: FeedSort,
    var order: FeedOrder
)

enum class FeedSort {
    CONTRIBUTORS, LIKES, MOST_VIEWED, MOST_RECENT, LOCATION
}

enum class FeedOrder {
    ASC, DESC
}