package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep
data class Image(val url: String, val width: Int, val height: Int, val extra: String)