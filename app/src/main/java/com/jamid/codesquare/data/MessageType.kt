package com.jamid.codesquare.data

import androidx.annotation.Keep
import com.jamid.codesquare.document
import com.jamid.codesquare.image
import com.jamid.codesquare.video

@Keep
enum class MessageType(s: String) {
    Image(image), Video(video), Document(document)
}
