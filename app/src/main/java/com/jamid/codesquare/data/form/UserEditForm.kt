package com.jamid.codesquare.data.form

data class UserEditForm(
    var name: String,
    var username: String,
    var tag: String,
    var about: String,
    var interests: List<String>,
    var photo: String
)
