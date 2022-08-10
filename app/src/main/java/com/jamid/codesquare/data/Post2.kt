package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep
sealed class Post2 {
    data class Collab(val post: Post): Post2()
    data class Advertise(val id: String): Post2()
}