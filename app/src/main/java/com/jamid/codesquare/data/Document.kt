package com.jamid.codesquare.data

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class Document(
    val id: String,
    val name: String,
    val sizeText: String,
    val documentUri: Uri
)