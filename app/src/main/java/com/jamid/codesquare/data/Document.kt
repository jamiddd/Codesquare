package com.jamid.codesquare.data

import android.net.Uri

data class Document(
    val id: String,
    val name: String,
    val sizeText: String,
    val documentUri: Uri
)