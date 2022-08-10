package com.jamid.codesquare.data

import android.net.Uri
import androidx.annotation.Keep
// something simple
@Keep
data class UploadItem(
    val fileUri: Uri,
    val path: String,
    val name: String
)
