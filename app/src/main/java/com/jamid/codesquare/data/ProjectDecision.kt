package com.jamid.codesquare.data

data class ProjectDecision(
    val id: String,
    val obj: Any,
    val clazz: String,
    val votersList: List<String>,
    val projectId: String
) {
    constructor(): this("", "", "", emptyList(), "")
}