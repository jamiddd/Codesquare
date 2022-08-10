package com.jamid.codesquare

import androidx.core.content.FileProvider

class CollabMeFileProvider(path: Int): FileProvider() {

    constructor(): this(R.xml.file_paths)



}