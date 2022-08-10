package com.jamid.codesquare.data.form

import android.util.Log
import androidx.annotation.Keep

@Keep
data class RegisterFormState(
    val nameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val confirmPasswordError: Int? = null,
    val isDataValid: Boolean = false
) {
    init {
        Log.d("Something", "Simple: ")
    }
}
