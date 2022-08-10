package com.jamid.codesquare.data

import androidx.annotation.Keep
// something simple
@Keep
class UniqueUsernameException(msg: String): Exception(msg) {
    //
}

@Keep
class UserDocumentNotFoundException(msg: String): Exception(msg) {

}

@Keep
class ImageUploadException(msg: String): Exception(msg) {
    //
}