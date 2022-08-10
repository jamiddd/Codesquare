package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
sealed class Message2: Parcelable {
    // something simple
    @Parcelize
    @Keep
    data class MessageItem(
        val message: Message
    ): Message2()

    @Parcelize
    data class DateSeparator(val date: Date, val text: String): Message2()
}