package com.jamid.codesquare.data.form

import android.util.Log
import androidx.annotation.Keep

@Keep
data class UpdateUserFormState(
    val nameError: Int? = null,
    val usernameError: Int? = null,
    val tagError: Int? = null,
    val aboutError: Int? = null,
    val isValid: Boolean = false
) {
    init {
        Log.d("Something", "Simple: ")
    }
}