package com.jamid.codesquare.data

import androidx.annotation.Keep

@Keep
sealed class Post {
    data class Project1(val project: Project) : Post()
    data class Ad(val attachedProjectId: String) : Post()
}