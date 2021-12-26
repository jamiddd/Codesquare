package com.jamid.codesquare

import kotlinx.serialization.*
import kotlinx.serialization.json.*

private val json = Json {
    ignoreUnknownKeys = true
}

internal inline fun <reified R : Any> String.convertToDataClass() =
    json.decodeFromString<R>(this)