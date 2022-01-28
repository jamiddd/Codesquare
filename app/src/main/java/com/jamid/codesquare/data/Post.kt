package com.jamid.codesquare.data

sealed class Post {
    data class Project1(val project: Project) : Post()
    data class Ad(val attachedProjectId: String) : Post()
}