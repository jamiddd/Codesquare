package com.jamid.codesquare.data

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class UserUpdate(
    val username: String? = null,
    val name: String? = null,
    val photo: Uri? = null,
    val isPreUploadedImage: Boolean = false,
    val tag: String? = null,
    val about: String? = null,
    val interests: List<String> = emptyList()
)
