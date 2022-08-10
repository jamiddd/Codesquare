package com.jamid.codesquare.data.form

import androidx.annotation.Keep

@Keep
data class LoginFormState(
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)