package com.jamid.codesquare.data

data class Debug(
    val id: String,
    val createdAt: Long,
    val message: String,
    val line: Long,
    val file: String
) {
    constructor(): this("", 0, "", 0, "")
}